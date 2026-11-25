// Package arraysum sums an array of integers, sequentially or with goroutines. Each goroutine sums
// one contiguous part of the array and writes its partial sum into its own slot of a results
// slice; after all goroutines finish, the calling goroutine adds the slots up. No two goroutines
// write the same memory, so the only synchronisation needed is waiting for them.
package arraysum

import (
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

// Parallel sums the slice using workers goroutines, each handling one contiguous part.
//
// Goroutine i writes only partials[i]. Different elements of a slice are different memory
// locations, so these writes don't race with each other and need no lock. wg.Wait then makes
// every write visible to the calling goroutine (Done happens before the Wait it unblocks returns),
// which reads the slots and adds them up.
func Parallel(arr []int32, workers int) (int64, error) {
	ranges, err := splitRange(len(arr), workers)
	if err != nil {
		return 0, err
	}

	partials := make([]int64, len(ranges))
	var wg sync.WaitGroup
	wg.Add(len(ranges))
	for i, r := range ranges {
		go func(i int, part []int32) {
			defer wg.Done()
			partials[i] = Sequential(part)
		}(i, arr[r.start:r.end])
	}
	wg.Wait()

	var total int64
	for _, partial := range partials {
		total += partial
	}
	return total, nil
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
