package arraysum

import (
	"sync"
	"testing"
)

func TestCasSumAddsSequentially(t *testing.T) {
	var s casSum
	for _, v := range []int64{5, -2, 10} {
		if retries := s.add(v); retries != 0 {
			t.Errorf("add(%d) retried %d times with no other goroutine running", v, retries)
		}
	}
	if got := s.get(); got != 13 {
		t.Errorf("get() = %d, want 13", got)
	}
}

// TestCasSumLosesNoUpdateUnderContention adds 1 from many goroutines at once. With a plain
// read-then-write instead of the CAS loop, some of the additions would be lost.
func TestCasSumLosesNoUpdateUnderContention(t *testing.T) {
	const (
		goroutines = 32
		addsEach   = 10_000
	)
	var (
		s            casSum
		wg           sync.WaitGroup
		mu           sync.Mutex
		totalRetries int
	)
	wg.Add(goroutines)
	for g := 0; g < goroutines; g++ {
		go func() {
			defer wg.Done()
			retries := 0
			for i := 0; i < addsEach; i++ {
				retries += s.add(1)
			}
			mu.Lock()
			totalRetries += retries
			mu.Unlock()
		}()
	}
	wg.Wait()

	if got := s.get(); got != goroutines*addsEach {
		t.Errorf("get() = %d, want %d", got, goroutines*addsEach)
	}
	t.Logf("%d CAS retries for %d adds", totalRetries, goroutines*addsEach)
}
