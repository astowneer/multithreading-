package arraysum

// minForkSize is the smallest part worth splitting further. Below it, starting a goroutine costs
// more than summing the elements on the current one.
const minForkSize = 1 << 12

// splitWorkers decides how to split a part of size elements handled by workers goroutines (at
// least 2) into two halves: the left half gets leftWorkers of them and the first mid elements.
// Elements are divided in proportion to workers, so with 3 workers the left third goes to one
// goroutine and the right two thirds to two, and every leaf ends up with about size/workers
// elements.
func splitWorkers(size, workers int) (mid, leftWorkers int) {
	leftWorkers = workers / 2
	// size*leftWorkers cannot overflow: sumRecursive caps workers at size, and size <= MaxInt32.
	mid = size * leftWorkers / workers
	return mid, leftWorkers
}

// sumRecursive sums arr with workers goroutines in fork-join style. It splits arr into two halves,
// forks a new goroutine for the left half, sums the right half on the current goroutine, then
// joins by waiting for the left result. Each half is split again the same way, until a part has
// one worker or is too small to be worth splitting.
//
// The left result travels over a channel with room for one value, so the forked goroutine never
// waits, and the receive is the join: it waits for the goroutine and hands over its sum in one
// step.
func sumRecursive(arr []int32, workers int) int64 {
	workers = min(workers, len(arr))
	if workers <= 1 || len(arr) < minForkSize {
		return Sequential(arr)
	}

	mid, leftWorkers := splitWorkers(len(arr), workers)
	leftSum := make(chan int64, 1)
	go func() {
		leftSum <- sumRecursive(arr[:mid], leftWorkers)
	}()
	right := sumRecursive(arr[mid:], workers-leftWorkers)
	return <-leftSum + right
}
