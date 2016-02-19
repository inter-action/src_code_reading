package concurrent

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"sync"
	"testing"
	"time"
)

/*
todo: 处理 dead lock 的问题
	这里边没有死锁， 由于 LockTestObject 和 内部 cond 对象公用一个锁 任何调用 cond.Wait.. 开头的方法都会先释放
	锁, 执行完之后再回上锁, 我并不认为这种设计方式合理, 首先第一眼看上去 TestTimeoutCondWaitTimeoutRemain 方法
	就会执行死锁 （我就是这样的 找了半天才发现不是）容易造成对代码错误的理解, 用起来估计也容易写出带有bug的代码
	(你以为你在 LockTestObject 对象上上锁了, cond.wait 却给打开了！)

	cond.Wait~ 开头的方法 首先会打开自身的锁 然后依赖于自身的 channel 来做等待
*/
type LockTestObject struct {
	lock *sync.Mutex
	cond *TimeoutCond
}

func NewLockTestObject() *LockTestObject {
	lock := new(sync.Mutex)
	// ! same lock for two obj
	return &LockTestObject{lock: lock, cond: NewTimeoutCond(lock)}
}

// return only after on timeout or cond.signal
func (o *LockTestObject) lockAndWaitWithTimeout(timeout time.Duration) (time.Duration, bool) {
	o.lock.Lock()
	defer o.lock.Unlock()
	return o.cond.WaitWithTimeout(timeout)
}

// wait cond.singal triggering
func (o *LockTestObject) lockAndWait() bool {
	o.lock.Lock()
	defer o.lock.Unlock()
	fmt.Println("lockAndWait")
	return o.cond.Wait()
}

// lock this LockTestObject, send cond.Singal
func (o *LockTestObject) lockAndNotify() {
	o.lock.Lock()
	defer o.lock.Unlock()
	fmt.Println("lockAndNotify")
	o.cond.Signal()
}

// these test doesnt insert any assert !, so unit test doesnt do any deed

// ! this func will cause dead lock
func TestTimeoutCondWait(t *testing.T) {
	fmt.Println("TestTimeoutCondWait")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	wait.Add(2)

	go func() {
		// this goes first, obj will be locked, then obj.lockAndNotify will never get
		// a chance to excute obj.cond.Signal, therehence, obj.lockAndWait will never
		// release its lock
		//
		// No dead lock ! lockAndWait call cond.Wait, and cond.Wait release the lock both obj holds
		// and use its channel to implement wait.
		obj.lockAndWait()
		wait.Done()
	}()
	time.Sleep(time.Duration(50) * time.Millisecond)
	go func() {
		obj.lockAndNotify()
		wait.Done()
	}()
	wait.Wait()
}

func TestTimeoutCondWaitTimeout(t *testing.T) {
	fmt.Println("TestTimeoutCondWaitTimeout")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	wait.Add(1)
	go func() {
		obj.lockAndWaitWithTimeout(time.Duration(2) * time.Second)
		wait.Done()
	}()
	wait.Wait()
	// insert assert(true) here?
	//  if wait.Done() never get its chance to execute, insertion of assert(true) does no good
}

func TestTimeoutCondWaitTimeoutNotify(t *testing.T) {
	fmt.Println("TestTimeoutCondWaitTimeoutNotify")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	wait.Add(2)
	ch := make(chan int, 1)
	timeout := 2000
	go func() {
		begin := currentTimeMillis()
		// time.Duration default to nano secs
		obj.lockAndWaitWithTimeout(time.Duration(timeout) * time.Millisecond)
		end := currentTimeMillis()
		ch <- int((end - begin))
		wait.Done()
	}()
	sleep(200) // 200 nano-secs
	go func() {
		obj.lockAndNotify()
		wait.Done()
	}()
	wait.Wait()
	time := <-ch
	close(ch)
	assert.True(t, time < timeout)
	assert.True(t, time >= 200)
}

func sleep(millisecond int) {
	time.Sleep(time.Duration(millisecond) * time.Millisecond)
}

func currentTimeMillis() int64 {
	return time.Now().UnixNano() / int64(time.Millisecond)
}

func TestTimeoutCondWaitTimeoutRemain(t *testing.T) {
	fmt.Println("TestTimeoutCondWaitTimeoutRemain")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	wait.Add(2)
	ch := make(chan time.Duration, 1)
	timeout := time.Duration(2000) * time.Millisecond
	go func() {
		remainTimeout, _ := obj.lockAndWaitWithTimeout(timeout) // todo: 还是有死锁, 这个地方一定会是 timeout 之后 (没有死锁, 见上文)
		ch <- remainTimeout
		wait.Done()
	}()
	sleep(200)
	go func() {
		obj.lockAndNotify()
		wait.Done()
	}()
	wait.Wait()
	remainTimeout := <-ch
	close(ch)
	assert.True(t, remainTimeout < timeout, "expect remainTimeout %v < %v", remainTimeout, timeout)
	assert.True(t, remainTimeout >= time.Duration(200)*time.Millisecond, "expect remainTimeout %v >= 200 millisecond", remainTimeout)
}

func TestTimeoutCondHasWaiters(t *testing.T) {
	fmt.Println("TestTimeoutCondHasWaiters")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	wait.Add(2)
	go func() {
		obj.lockAndWait()
		wait.Done()
	}()
	time.Sleep(time.Duration(50) * time.Millisecond)
	// todo: 这个地方应该一直锁死才对
	obj.lock.Lock()
	assert.True(t, obj.cond.HasWaiters())
	obj.lock.Unlock()

	go func() {
		obj.lockAndNotify()
		wait.Done()
	}()
	wait.Wait()
	assert.False(t, obj.cond.HasWaiters())
}

func TestInterrupted(t *testing.T) {
	fmt.Println("TestInterrupted")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	count := 5
	wait.Add(5)
	ch := make(chan bool, 5)
	for i := 0; i < count; i++ {
		go func() {
			/*
				first goroutine block at here, when `obj.cond.Interrupt()` get called
				lockAndWait exits, original channel obj.cond.signal got closed,
				i++, next loop begins, loop i > 2 does not block at all
			*/
			ch <- obj.lockAndWait()
			wait.Done()
		}()
	}
	sleep(100)
	go func() { obj.cond.Interrupt() }()
	wait.Wait()

	for i := 0; i < count; i++ {
		b := <-ch
		assert.True(t, b, "expect %v interrupted bug get false", i)
	}
}

func TestInterruptedWithTimeout(t *testing.T) {
	fmt.Println("TestInterruptedWithTimeout")
	obj := NewLockTestObject()
	wait := sync.WaitGroup{}
	count := 5
	wait.Add(5)
	ch := make(chan bool, 5)
	timeout := time.Duration(1000) * time.Millisecond
	for i := 0; i < count; i++ {
		go func() {
			_, interrupted := obj.lockAndWaitWithTimeout(timeout)
			ch <- interrupted
			wait.Done()
		}()
	}
	sleep(100)
	go func() { obj.cond.Interrupt() }()
	wait.Wait()
	for i := 0; i < count; i++ {
		b := <-ch
		assert.True(t, b, "expect %v interrupted bug get false", i)
	}
}

func TestSignalNoWait(t *testing.T) {
	obj := NewLockTestObject()
	obj.cond.Signal()
}
