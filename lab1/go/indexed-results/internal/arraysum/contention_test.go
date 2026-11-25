package arraysum

import "testing"

// TestParallelSumIsExactUnderContention runs many workers over a small array many times, so many
// goroutines publish their partial sums at almost the same moment. A lost update in the
// synchronisation would show up as a wrong total, and under -race any unsynchronised access to
// shared memory fails the test.
func TestParallelSumIsExactUnderContention(t *testing.T) {
	const (
		size    = 10_000
		workers = 64
		runs    = 200
	)
	arr := make([]int32, size)
	for i := range arr {
		arr[i] = 1
	}

	for run := 0; run < runs; run++ {
		if got := mustParallel(t, arr, workers); got != size {
			t.Fatalf("run %d: Parallel = %d, want %d", run, got, size)
		}
	}
}
