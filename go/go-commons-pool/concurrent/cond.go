package concurrent

import (
	"sync"
	"time"
)

/*


Desc:
	这个模块的作用就是提供一个 timeout 的条件, 当在这个 timeout 之内的时候 会 block 当前 goroutine

	优先阅读下 cond_test 就能知道这个模块的作用 还有自己的注释

todo:
	- TimeoutCond L(locker) 的作用? 尤其是像 Wait 这样的方法先释放锁的原因

Notation
	- finished todos

*/

// TimeoutCond is a sync.Cond  improve for support wait timeout.
type TimeoutCond struct {
	L          sync.Locker
	signal     chan int //unbuffered channel
	hasWaiters bool
}

// NewTimeoutCond return a new TimeoutCond
func NewTimeoutCond(l sync.Locker) *TimeoutCond {
	cond := TimeoutCond{L: l, signal: make(chan int, 0)}
	return &cond
}

// WaitWithTimeout wait for signal
// return remain wait time, and is interrupted
func (cond *TimeoutCond) WaitWithTimeout(timeout time.Duration) (time.Duration, bool) {
	cond.setHasWaiters(true)
	ch := cond.signal
	//wait should unlock mutex,  if not will cause deadlock
	cond.L.Unlock() //todo:?
	defer cond.setHasWaiters(false)
	defer cond.L.Lock()

	begin := time.Now().UnixNano()
	select {
	case _, ok := <-ch: //get result, return remainTimeout, isok status
		end := time.Now().UnixNano()
		remainTimeout := timeout - time.Duration(end-begin)
		return remainTimeout, !ok
	case <-time.After(timeout): //timeout
		return 0, false
	}
}

func (cond *TimeoutCond) setHasWaiters(value bool) {
	cond.hasWaiters = value
}

// HasWaiters queries whether any goroutine are waiting on this condition
func (cond *TimeoutCond) HasWaiters() bool {
	return cond.hasWaiters
}

// Wait for signal return waiting is interrupted
func (cond *TimeoutCond) Wait() bool {
	cond.setHasWaiters(true)
	//copy signal in lock, avoid data race with Interrupt
	ch := cond.signal
	cond.L.Unlock()
	defer cond.setHasWaiters(false)
	defer cond.L.Lock()
	_, ok := <-ch //close(cond.signal) will cause ok to be false
	return !ok
}

// Signal wakes one goroutine waiting on c, if there is any.
func (cond *TimeoutCond) Signal() {
	//todo: why write like this? instead of cond.signal <- 1
	select {
	case cond.signal <- 1:
	default:
	}
}

// Interrupt goroutine wait on this TimeoutCond
func (cond *TimeoutCond) Interrupt() {
	cond.L.Lock() //get lock
	defer cond.L.Unlock()

	close(cond.signal)              //close signal
	cond.signal = make(chan int, 0) //re-assign signal variable
}
