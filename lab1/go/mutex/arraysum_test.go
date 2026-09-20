package main

import (
	"math"
	"testing"
)

// unevenSize is not divisible by 2, 3, 4, 7 or 16, so every goroutine count leaves a remainder.
const unevenSize = 1_000_003

var threadCounts = []int{1, 2, 3, 4, 7, 16}

func mustParallelSum(t *testing.T, arr []int32, threads int) int64 {
	t.Helper()
	sum, err := parallelSum(arr, threads)
	if err != nil {
		t.Fatalf("parallelSum(_, %d): %v", threads, err)
	}
	return sum
}

func TestSequentialSumKnownValues(t *testing.T) {
	if got := sequentialSum([]int32{1, 2, 3}); got != 6 {
		t.Errorf("sequentialSum([1 2 3]) = %d, want 6", got)
	}
	if got := sequentialSum(nil); got != 0 {
		t.Errorf("sequentialSum(nil) = %d, want 0", got)
	}
}

func TestSumDoesNotOverflowInt32(t *testing.T) {
	arr := []int32{math.MaxInt32, math.MaxInt32, math.MaxInt32, math.MaxInt32}
	want := 4 * int64(math.MaxInt32)

	if got := sequentialSum(arr); got != want {
		t.Errorf("sequentialSum = %d, want %d", got, want)
	}
	if got := mustParallelSum(t, arr, 3); got != want {
		t.Errorf("parallelSum = %d, want %d", got, want)
	}
}

func TestParallelSumMatchesClosedFormForUnevenSize(t *testing.T) {
	arr := make([]int32, unevenSize)
	var want int64
	for i := range arr {
		arr[i] = int32(i % 100)
		want += int64(i % 100)
	}

	for _, threads := range threadCounts {
		if got := mustParallelSum(t, arr, threads); got != want {
			t.Errorf("parallelSum with %d goroutines = %d, want %d", threads, got, want)
		}
	}
	if got := sequentialSum(arr); got != want {
		t.Errorf("sequentialSum = %d, want %d", got, want)
	}
}

func TestParallelSumWorksWithMoreGoroutinesThanElements(t *testing.T) {
	if got := mustParallelSum(t, []int32{1, 2, 3}, 8); got != 6 {
		t.Errorf("parallelSum([1 2 3], 8) = %d, want 6", got)
	}
	if got := mustParallelSum(t, nil, 4); got != 0 {
		t.Errorf("parallelSum(nil, 4) = %d, want 0", got)
	}
}

func TestParallelSumRejectsInvalidGoroutineCount(t *testing.T) {
	if _, err := parallelSum([]int32{1, 2, 3}, 0); err == nil {
		t.Error("parallelSum(_, 0) returned no error")
	}
}

func TestFillTouchesEveryElementIncludingRemainder(t *testing.T) {
	for _, threads := range []int{1, 3, 4, 7} {
		arr := make([]int32, unevenSize)
		for i := range arr {
			arr[i] = -1
		}

		if err := fillRandomParallel(arr, threads); err != nil {
			t.Fatalf("fillRandomParallel(_, %d): %v", threads, err)
		}

		for i, value := range arr {
			if value < 0 || value >= randomBound {
				t.Fatalf("%d goroutines: element %d is %d, left unfilled or out of range", threads, i, value)
			}
		}
	}
}
