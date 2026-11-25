package arraysum

import "sync"

// collector gathers partial sums from goroutines and lets one goroutine wait until a given number
// of them have arrived. It does with sync.Cond what a WaitGroup plus a mutex-protected total do
// together.
//
// mu guards sum and arrived. cond is tied to mu: Wait atomically unlocks mu and puts the caller to
// sleep, and locks mu again before returning, so the condition can be checked and waited on
// without a gap in which a wake-up could be missed.
type collector struct {
	mu      sync.Mutex
	cond    *sync.Cond
	sum     int64
	arrived int
}

func newCollector() *collector {
	c := &collector{}
	c.cond = sync.NewCond(&c.mu)
	return c
}

// add records one partial sum and wakes the waiting goroutine so it can check whether everything
// has arrived.
func (c *collector) add(partial int64) {
	c.mu.Lock()
	c.sum += partial
	c.arrived++
	c.mu.Unlock()
	c.cond.Broadcast()
}

// wait blocks until n partial sums have arrived and returns their total.
//
// The condition is checked in a loop, never with a single if: a goroutine can wake up while the
// condition is still false (another add woke it, but more are due), so after every wake-up it
// checks again and goes back to sleep if needed.
func (c *collector) wait(n int) int64 {
	c.mu.Lock()
	defer c.mu.Unlock()
	for c.arrived < n {
		c.cond.Wait()
	}
	return c.sum
}
