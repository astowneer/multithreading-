// Package arraysum sums an array of integers, sequentially or with goroutines. The goroutines are
// managed by an errgroup.Group, which waits for them, limits how many run at once and returns the
// first error any of them reports, cancelling the rest.
package arraysum

import (
	"context"
	"math/rand/v2"
	"sync"

	"golang.org/x/sync/errgroup"
)

// RandomBound is the exclusive upper bound of the values written by FillRandomParallel.
const RandomBound = 100

// Sequential sums the slice on the calling goroutine.
func Sequential(arr []int32) int64 {
	var sum int64
	for _, value := range arr {
		sum += int64(value)
	}
	return sum
}

// cancelCheckBlock is how many elements a goroutine sums between two checks of its context.
// Checking per element would slow the loop down; per block, cancellation still takes effect
// within microseconds.
const cancelCheckBlock = 1 << 16

// Parallel sums the slice using workers goroutines, each handling one contiguous part. It is
// ParallelContext with a context that is never cancelled.
func Parallel(arr []int32, workers int) (int64, error) {
	return ParallelContext(context.Background(), arr, workers)
}

// ParallelContext sums the slice using workers goroutines, each handling one contiguous part, and
// stops early when ctx is cancelled.
//
// errgroup.WithContext derives a context that is cancelled as soon as any goroutine returns an
// error (or the parent ctx is cancelled). Every goroutine checks that context between blocks, so
// one failure or one Ctrl+C stops all of them, and g.Wait returns the first error.
//
// g.Go starts a goroutine and g.Wait waits for all of them, like a WaitGroup with Add and Done
// done for you. SetLimit caps how many run at once. Goroutine i writes only partials[i], so the
// partial sums need no lock.
func ParallelContext(ctx context.Context, arr []int32, workers int) (int64, error) {
	ranges, err := splitRange(len(arr), workers)
	if err != nil {
		return 0, err
	}

	g, ctx := errgroup.WithContext(ctx)
	g.SetLimit(workers)
	partials := make([]int64, len(ranges))
	for i, r := range ranges {
		g.Go(func() error {
			sum, err := sumContext(ctx, arr[r.start:r.end])
			partials[i] = sum
			return err
		})
	}
	if err := g.Wait(); err != nil {
		return 0, err
	}

	var total int64
	for _, partial := range partials {
		total += partial
	}
	return total, nil
}

// sumContext sums part in blocks of cancelCheckBlock elements and returns ctx.Err() as soon as
// the context is cancelled.
func sumContext(ctx context.Context, part []int32) (int64, error) {
	var sum int64
	for start := 0; start < len(part); start += cancelCheckBlock {
		if err := ctx.Err(); err != nil {
			return 0, err
		}
		sum += Sequential(part[start:min(start+cancelCheckBlock, len(part))])
	}
	return sum, nil
}

// FillRandomParallel fills the slice with random values in [0, RandomBound) using workers
// goroutines.
func FillRandomParallel(arr []int32, workers int) error {
	ranges, err := splitRange(len(arr), workers)
	if err != nil {
		return err
	}

	runAll(ranges, func(r chunkRange) {
		part := arr[r.start:r.end]
		for i := range part {
			part[i] = rand.Int32N(RandomBound)
		}
	})
	return nil
}

// runAll starts one goroutine per range, then waits for all of them to finish.
func runAll(ranges []chunkRange, task func(chunkRange)) {
	var wg sync.WaitGroup
	wg.Add(len(ranges))
	for _, r := range ranges {
		go func(r chunkRange) {
			defer wg.Done()
			task(r)
		}(r)
	}
	wg.Wait()
}
