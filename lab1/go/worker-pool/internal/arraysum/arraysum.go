// Package arraysum sums an array of integers, sequentially or with goroutines. The array is split
// into more parts than there are workers, and a fixed pool of worker goroutines takes the parts
// from a channel one at a time, so a worker that finishes early takes more parts.
package arraysum

import (
	"fmt"
	"math/rand/v2"
	"sync"
)

// ChunksPerWorker is how many parts the array is split into per worker. More parts balance the
// load better, because a fast worker takes more of them; fewer parts mean less channel traffic.
const ChunksPerWorker = 4

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

// Parallel sums the slice using a pool of workers goroutines. The slice is split into
// workers*ChunksPerWorker contiguous parts, which the workers take from a channel; each worker
// sends the sum of each part it takes back on another channel.
func Parallel(arr []int32, workers int) (int64, error) {
	if workers < 1 {
		return 0, fmt.Errorf("workers must be >= 1, was %d", workers)
	}
	chunks, err := splitRange(len(arr), workers*ChunksPerWorker)
	if err != nil {
		return 0, err
	}

	return runPool(workers, chunks, func(r chunkRange) int64 {
		return Sequential(arr[r.start:r.end])
	}), nil
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
