package arraysum

import (
	"fmt"
	"testing"
)

func TestSplitWorkersDividesElementsInProportionToWorkers(t *testing.T) {
	cases := []struct {
		size, workers, wantMid, wantLeft int
	}{
		{100, 2, 50, 1},
		{100, 3, 33, 1},
		{100, 4, 50, 2},
		{101, 2, 50, 1},
		{90, 9, 40, 4},
	}
	for _, c := range cases {
		mid, left := splitWorkers(c.size, c.workers)
		if mid != c.wantMid || left != c.wantLeft {
			t.Errorf("splitWorkers(%d, %d) = (%d, %d), want (%d, %d)",
				c.size, c.workers, mid, left, c.wantMid, c.wantLeft)
		}
	}
}

// leafSizes replays sumRecursive's splitting and returns the size of every part summed
// sequentially, in order.
func leafSizes(size, workers int) []int {
	workers = min(workers, size)
	if workers <= 1 || size < minForkSize {
		return []int{size}
	}
	mid, left := splitWorkers(size, workers)
	return append(leafSizes(mid, left), leafSizes(size-mid, workers-left)...)
}

func TestRecursionMakesOneBalancedLeafPerWorker(t *testing.T) {
	const size = 1_000_003
	for _, workers := range []int{1, 2, 3, 4, 7, 16} {
		t.Run(fmt.Sprintf("workers=%d", workers), func(t *testing.T) {
			leaves := leafSizes(size, workers)

			if len(leaves) != workers {
				t.Fatalf("%d leaves, want %d", len(leaves), workers)
			}
			total, shortest, longest := 0, leaves[0], leaves[0]
			for _, n := range leaves {
				total += n
				shortest, longest = min(shortest, n), max(longest, n)
			}
			if total != size {
				t.Errorf("leaves cover %d elements, want %d", total, size)
			}
			if longest-shortest > 1 {
				t.Errorf("leaf sizes range from %d to %d", shortest, longest)
			}
		})
	}
}

func TestSumRecursiveDoesNotSplitSmallParts(t *testing.T) {
	if got := leafSizes(minForkSize-1, 8); len(got) != 1 {
		t.Errorf("a part smaller than minForkSize was split into %d leaves", len(got))
	}
}
