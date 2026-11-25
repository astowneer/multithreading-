// Package arraysum sums an array of integers, sequentially or with goroutines. The array is split
// into more parts than there are workers and every part gets its own goroutine, but a semaphore
// lets only workers of them run at a time. Partial sums are added to a mutex-protected total.
package arraysum

import (
	"fmt"
	"math/rand/v2"
	"sync"
)

// ChunksPerWorker is how many parts the array is split into per worker.
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

// Parallel sums the slice in workers*ChunksPerWorker contiguous parts, one goroutine per part, with
// at most workers goroutines running at once.
//
// The loop acquires a permit before it starts each goroutine, and the goroutine releases it when
// done. Once all permits are taken, the loop itself blocks, so at most workers goroutines exist at
// any moment, not just run. Acquiring inside the goroutine instead would also cap the running
// goroutines, but would start all of them at once, each waiting for its turn.
func Parallel(arr []int32, workers int) (int64, error) {
	if workers < 1 {
		return 0, fmt.Errorf("workers must be >= 1, was %d", workers)
	}
	chunks, err := splitRange(len(arr), workers*ChunksPerWorker)
	if err != nil {
		return 0, err
	}

	sem := newSemaphore(workers)
	var (
		total sharedSum
		wg    sync.WaitGroup
	)
	wg.Add(len(chunks))
	for _, r := range chunks {
		sem.acquire()
		go func(part []int32) {
			defer wg.Done()
			defer sem.release()
			total.add(Sequential(part))
		}(arr[r.start:r.end])
	}
	wg.Wait()
	return total.get(), nil
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
