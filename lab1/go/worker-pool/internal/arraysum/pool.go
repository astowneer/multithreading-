package arraysum

import "sync"

// runPool runs task for every job on a fixed pool of workers goroutines and returns the sum of
// the results. It is the classic Go worker pool made of three channels' worth of plumbing:
//
//   - a producer goroutine sends every job into jobs and then closes it;
//   - each worker receives jobs until jobs is closed and empty, and sends one result per job;
//   - a closer goroutine waits for every worker to return and then closes results;
//   - the calling goroutine receives from results until it is closed and adds the values up.
//
// Each channel is closed by the only goroutine that sends on it, and only after its last send,
// which is the rule that keeps a pool free of "send on closed channel" panics.
func runPool(workers int, jobs []chunkRange, task func(chunkRange) int64) int64 {
	jobCh := make(chan chunkRange)
	resultCh := make(chan int64)

	var wg sync.WaitGroup
	wg.Add(workers)
	for w := 0; w < workers; w++ {
		go func() {
			defer wg.Done()
			for job := range jobCh {
				resultCh <- task(job)
			}
		}()
	}

	go func() {
		for _, job := range jobs {
			jobCh <- job
		}
		close(jobCh)
	}()

	go func() {
		wg.Wait()
		close(resultCh)
	}()

	var total int64
	for result := range resultCh {
		total += result
	}
	return total
}
