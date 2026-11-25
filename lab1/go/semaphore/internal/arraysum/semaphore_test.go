package arraysum

import (
	"fmt"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

func TestSemaphoreBlocksWhenAllPermitsAreTaken(t *testing.T) {
	sem := newSemaphore(2)
	sem.acquire()
	sem.acquire()

	acquired := make(chan struct{})
	go func() {
		sem.acquire()
		close(acquired)
	}()

	select {
	case <-acquired:
		t.Fatal("third acquire succeeded while both permits were taken")
	case <-time.After(20 * time.Millisecond):
	}

	sem.release()
	select {
	case <-acquired:
	case <-time.After(time.Second):
		t.Fatal("acquire still blocked after a release")
	}
}

// TestSemaphoreLimitsConcurrentGoroutines starts 40 goroutines and measures how many of them hold
// a permit at the same moment.
func TestSemaphoreLimitsConcurrentGoroutines(t *testing.T) {
	for _, permits := range []int{1, 3} {
		t.Run(fmt.Sprintf("permits=%d", permits), func(t *testing.T) {
			sem := newSemaphore(permits)
			var (
				wg            sync.WaitGroup
				running, peak atomic.Int64
			)
			wg.Add(40)
			for i := 0; i < 40; i++ {
				go func() {
					defer wg.Done()
					sem.acquire()
					defer sem.release()

					now := running.Add(1)
					for {
						old := peak.Load()
						if now <= old || peak.CompareAndSwap(old, now) {
							break
						}
					}
					time.Sleep(time.Millisecond)
					running.Add(-1)
				}()
			}
			wg.Wait()

			if got := peak.Load(); got > int64(permits) {
				t.Errorf("%d goroutines held a permit at once, want at most %d", got, permits)
			}
		})
	}
}
