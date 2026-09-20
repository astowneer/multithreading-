package main

import (
	"math/rand/v2"
	"sync"
)

// randomBound is the exclusive upper bound of the values written by fillRandomParallel.
const randomBound = 100

// sequentialSum sums the slice on the calling goroutine.
func sequentialSum(arr []int32) int64 {
	var sum int64
	for _, value := range arr {
		sum += int64(value)
	}
	return sum
}

// parallelSum sums the slice using threads goroutines, each handling one contiguous part. Each
// goroutine sums its part into a local variable and adds it to the shared total once, so the
// mutex is taken threads times rather than once per element.
func parallelSum(arr []int32, threads int) (int64, error) {
	ranges, err := splitRange(len(arr), threads)
	if err != nil {
		return 0, err
	}

	var total sharedSum
	runAll(ranges, func(r chunkRange) {
		total.add(sequentialSum(arr[r.start:r.end]))
	})
	return total.get(), nil
}

// fillRandomParallel fills the slice with random values in [0, randomBound) using threads
// goroutines.
func fillRandomParallel(arr []int32, threads int) error {
	ranges, err := splitRange(len(arr), threads)
	if err != nil {
		return err
	}

	runAll(ranges, func(r chunkRange) {
		part := arr[r.start:r.end]
		for i := range part {
			part[i] = rand.Int32N(randomBound)
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
