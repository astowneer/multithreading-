// Package arraysum sums an array of integers, sequentially or with goroutines. Each goroutine sums
// one contiguous part of the array and adds its partial sum to a shared total with an atomic
// add, so no lock is needed.
package arraysum

import (
	"math/rand/v2"
	"sync"
	"sync/atomic"
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

// Parallel sums the slice using workers goroutines, each handling one contiguous part. Each
// goroutine sums its part into a local variable and adds it to the shared total once, with
// atomic.Int64.Add. The add is a single indivisible CPU instruction (LOCK XADD on amd64, LDADD on
// arm64), so two goroutines adding at the same moment cannot lose an update and no goroutine ever
// waits for a lock.
func Parallel(arr []int32, workers int) (int64, error) {
	ranges, err := splitRange(len(arr), workers)
	if err != nil {
		return 0, err
	}

	var total atomic.Int64
	runAll(ranges, func(r chunkRange) {
		total.Add(Sequential(arr[r.start:r.end]))
	})
	// wg.Wait inside runAll happens after every Add, so a plain Load sees the final total.
	return total.Load(), nil
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
