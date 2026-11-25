// Package arraysum sums an array of integers, sequentially or with goroutines. Each goroutine sums
// one contiguous part of the array and sends its partial sum over a channel; the calling goroutine
// receives the partial sums and adds them up. No memory is shared, so no lock is needed.
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
// Each goroutine sends its partial sum into the results channel. The calling goroutine receives
// exactly one value per goroutine, so the receive loop also replaces the WaitGroup: when it ends,
// every goroutine has delivered its sum. Only the receiving goroutine touches total, so total
// needs no synchronisation; the channel hands each value over safely.
//
// The channel is buffered with room for every partial sum, so a goroutine never waits for the
// receiver: it sends and exits.
func Parallel(arr []int32, workers int) (int64, error) {
	ranges, err := splitRange(len(arr), workers)
	if err != nil {
		return 0, err
	}

	results := make(chan int64, len(ranges))
	for _, r := range ranges {
		go func(part []int32) {
			results <- Sequential(part)
		}(arr[r.start:r.end])
	}

	var total int64
	for range ranges {
		total += <-results
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
