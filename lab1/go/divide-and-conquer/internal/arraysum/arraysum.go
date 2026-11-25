// Package arraysum sums an array of integers, sequentially or with goroutines. The parallel sum is
// divide and conquer in fork-join style: the array is halved recursively, each split forks a
// goroutine for one half, and the halves' sums are joined on the way back up.
package arraysum

import (
	"fmt"
	"math/rand/v2"
	"sync"
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

// Parallel sums the slice using workers goroutines in total: sumRecursive splits it into workers
// balanced parts by halving, and every split but the first runs on a goroutine forked for it.
func Parallel(arr []int32, workers int) (int64, error) {
	if workers < 1 {
		return 0, fmt.Errorf("workers must be >= 1, was %d", workers)
	}
	return sumRecursive(arr, workers), nil
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
