package arraysum

import (
	"fmt"
	"sync/atomic"
	"testing"
	"time"
)

func TestRunPoolRunsEveryJobOnce(t *testing.T) {
	jobs := make([]chunkRange, 50)
	for i := range jobs {
		jobs[i] = chunkRange{i, i + 1}
	}
	var calls atomic.Int64

	// Each job returns its start index, so the total is 0+1+...+49 only if every job ran once.
	got := runPool(3, jobs, func(r chunkRange) int64 {
		calls.Add(1)
		return int64(r.start)
	})

	if got != 49*50/2 {
		t.Errorf("total = %d, want %d", got, 49*50/2)
	}
	if calls.Load() != 50 {
		t.Errorf("task ran %d times, want 50", calls.Load())
	}
}

func TestRunPoolWithNoJobsReturnsZero(t *testing.T) {
	if got := runPool(4, nil, func(chunkRange) int64 { return 1 }); got != 0 {
		t.Errorf("total = %d, want 0", got)
	}
}

// TestRunPoolNeverRunsMoreTasksThanWorkers measures how many tasks run at the same moment. This is
// what makes a pool a pool: 40 jobs, but never more than workers of them in flight.
func TestRunPoolNeverRunsMoreTasksThanWorkers(t *testing.T) {
	for _, workers := range []int{1, 2, 5} {
		t.Run(fmt.Sprintf("workers=%d", workers), func(t *testing.T) {
			var running, peak atomic.Int64
			jobs := make([]chunkRange, 40)

			runPool(workers, jobs, func(chunkRange) int64 {
				now := running.Add(1)
				for {
					old := peak.Load()
					if now <= old || peak.CompareAndSwap(old, now) {
						break
					}
				}
				time.Sleep(time.Millisecond)
				running.Add(-1)
				return 0
			})

			if got := peak.Load(); got > int64(workers) {
				t.Errorf("%d tasks ran at once, want at most %d", got, workers)
			}
		})
	}
}
