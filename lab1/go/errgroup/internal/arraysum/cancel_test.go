package arraysum

import (
	"context"
	"errors"
	"testing"
	"time"
)

func TestParallelContextStopsWhenCancelledBeforeStart(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()

	_, err := ParallelContext(ctx, make([]int32, 1_000_000), 4)

	if !errors.Is(err, context.Canceled) {
		t.Errorf("err = %v, want context.Canceled", err)
	}
}

func TestParallelContextStopsWhenDeadlinePasses(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), time.Millisecond)
	defer cancel()
	arr := make([]int32, 50_000_000) // takes far longer than 1 ms to sum

	start := time.Now()
	_, err := ParallelContext(ctx, arr, 4)

	if !errors.Is(err, context.DeadlineExceeded) {
		t.Errorf("err = %v, want context.DeadlineExceeded", err)
	}
	if elapsed := time.Since(start); elapsed > 500*time.Millisecond {
		t.Errorf("took %v to stop after the deadline", elapsed)
	}
}

func TestSumContextMatchesSequentialAcrossBlockBoundaries(t *testing.T) {
	for _, size := range []int{0, 1, cancelCheckBlock - 1, cancelCheckBlock, 3*cancelCheckBlock + 7} {
		arr := make([]int32, size)
		for i := range arr {
			arr[i] = int32(i % 100)
		}
		got, err := sumContext(context.Background(), arr)
		if err != nil {
			t.Fatal(err)
		}
		if want := Sequential(arr); got != want {
			t.Errorf("size %d: sumContext = %d, want %d", size, got, want)
		}
	}
}
