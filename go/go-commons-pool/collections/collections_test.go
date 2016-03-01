package collections

import (
	"github.com/stretchr/testify/assert"
	"sync"
	"testing"
)
/*

bm:

HashableObject 和 UnhashableObject 的区别， 见下边描述

golang map key constraint:
>The comparison operators == and != must be fully defined for operands of the key type;
thus the key type must not be a function, map, or slice. If the key type is an interface type,
these comparison operators must be defined for the dynamic key values; failure will cause a run-time panic.

ref:
	http://jolestar.com/go-commons-pool-and-go-concurrent/
	https://blog.golang.org/go-maps-in-action

 */
type HashableObject struct {
	str string
	i   int
	b   bool
}

type UnhashableObject struct {
	str  string
	i    int
	b    bool
	strs []string
	m    map[string]int
}

func TestSyncMapString(t *testing.T) {
	m := NewSyncMap()
	key := "key"
	//var key *interface{}
	//key = &"key1"

	// 这个地方需要注意下 func (m *SyncIdentityMap) Put(key interface{}, value interface{}) {
	// &key 返回的是 [uintptr](uintptr is an integer type that is large enough to hold
	// the bit pattern of any pointer.)
	// 所以这个地方放到map中的值是能确保一致的
	m.Put(&key, "value1")
	assert.Equal(t, "value1", m.Get(&key))
	m.Remove(&key)
	assert.Nil(t, m.Get(&key))
}

func TestSyncMapValues(t *testing.T) {
	m := NewSyncMap()
	key := "key"
	key2 := "key2"
	m.Put(&key, "value1")
	m.Put(&key2, "value2")
	values := m.Values()
	assert.Equal(t, 2, len(values))
}

func TestSyncMapHashableObject(t *testing.T) {
	m := NewSyncMap()
	o1 := HashableObject{}
	m.Put(&o1, "value1")
	assert.Equal(t, "value1", m.Get(&o1))
	//change object
	o1.str = "str"
	o1.i = 6
	assert.Equal(t, "value1", m.Get(&o1))
}

func TestSyncMapHashableObject2(t *testing.T) {
	m := NewSyncMap()
	o1 := HashableObject{}
	m.Put(&o1, "value1")
	assert.Equal(t, "value1", m.Get(&o1))

	o2 := HashableObject{}
	assert.Nil(t, m.Get(&o2))
}

func TestSyncMapHashableObject3(t *testing.T) {
	m := NewSyncMap()
	o1 := HashableObject{}
	m.Put(&o1, &o1)
	o1.str = "h"
	assert.Equal(t, &o1, m.Get(&o1))// bm: m.Get(&o1) returns *HashableObject
}

func TestSyncMapUnhashableObject(t *testing.T) {
	m := NewSyncMap()
	o1 := UnhashableObject{}
	m.Put(&o1, "value1")
	assert.Equal(t, "value1", m.Get(&o1))
	//change object
	o1.str = "str"
	o1.i = 6
	assert.Equal(t, "value1", m.Get(&o1))
}

func TestMultiThread(t *testing.T) {
	m := NewSyncMap()
	wait := sync.WaitGroup{}
	wait.Add(1000)
	for i := 0; i < 1000; i++ {
		go func() {
			o1 := HashableObject{}
			m.Put(&o1, &o1)
			wait.Done()
		}()
	}
	wait.Wait()
	assert.Equal(t, 1000, m.Size())
}
