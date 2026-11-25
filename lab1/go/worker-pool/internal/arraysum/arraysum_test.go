package arraysum

import (
	"fmt"
	"math"
	"testing"
)

// unevenSize is not divisible by 2, 3, 4, 7 or 16, so every goroutine count leaves a remainder.
const unevenSize = 1_000_003

var workerCounts = []int{1, 2, 3, 4, 7, 16}

func mustParallel(t *testing.T, arr []int32, workers int) int64 {
	t.Helper()
	sum, err := Parallel(arr, workers)
	if err != nil {
		t.Fatalf("Parallel(_, %d): %v", workers, err)
	}
	return sum
}

func TestSequentialSumKnownValues(t *testing.T) {
	if got := Sequential([]int32{1, 2, 3}); got != 6 {
		t.Errorf("Sequential([1 2 3]) = %d, want 6", got)
	}
	if got := Sequential(nil); got != 0 {
		t.Errorf("Sequential(nil) = %d, want 0", got)
	}
}

func TestSumDoesNotOverflowInt32(t *testing.T) {
	arr := []int32{math.MaxInt32, math.MaxInt32, math.MaxInt32, math.MaxInt32}
	want := 4 * int64(math.MaxInt32)

	if got := Sequential(arr); got != want {
		t.Errorf("Sequential = %d, want %d", got, want)
	}
	if got := mustParallel(t, arr, 3); got != want {
		t.Errorf("Parallel = %d, want %d", got, want)
	}
}

func TestParallelSumMatchesClosedFormForUnevenSize(t *testing.T) {
	arr := make([]int32, unevenSize)
	var want int64
	for i := range arr {
		arr[i] = int32(i % 100)
		want += int64(i % 100)
	}

	for _, workers := range workerCounts {
		t.Run(fmt.Sprintf("workers=%d", workers), func(t *testing.T) {
			if got := mustParallel(t, arr, workers); got != want {
				t.Errorf("Parallel = %d, want %d", got, want)
			}
		})
	}
	if got := Sequential(arr); got != want {
		t.Errorf("Sequential = %d, want %d", got, want)
	}
}

func TestParallelSumWorksWithMoreGoroutinesThanElements(t *testing.T) {
	if got := mustParallel(t, []int32{1, 2, 3}, 8); got != 6 {
		t.Errorf("Parallel([1 2 3], 8) = %d, want 6", got)
	}
	if got := mustParallel(t, nil, 4); got != 0 {
		t.Errorf("Parallel(nil, 4) = %d, want 0", got)
	}
}

func TestParallelSumRejectsInvalidGoroutineCount(t *testing.T) {
	if _, err := Parallel([]int32{1, 2, 3}, 0); err == nil {
		t.Error("Parallel(_, 0) returned no error")
	}
}

func TestFillTouchesEveryElementIncludingRemainder(t *testing.T) {
	for _, workers := range []int{1, 3, 4, 7} {
		t.Run(fmt.Sprintf("workers=%d", workers), func(t *testing.T) {
			arr := make([]int32, unevenSize)
			for i := range arr {
				arr[i] = -1
			}

			if err := FillRandomParallel(arr, workers); err != nil {
				t.Fatal(err)
			}

			for i, value := range arr {
				if value < 0 || value >= RandomBound {
					t.Fatalf("element %d is %d, left unfilled or out of range", i, value)
				}
			}
		})
	}
}
