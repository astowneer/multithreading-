package arraysum

import "sync/atomic"

// casSum is a lock-free accumulator built on compare-and-swap instead of a ready-made atomic add.
// It shows the retry loop that lock-free algorithms are made of.
type casSum struct {
	sum atomic.Int64
}

// add adds delta to the sum and reports how many times it had to retry because another goroutine
// changed the sum between the read and the swap.
//
// The loop reads the current value, computes the new one and swaps it in only if the sum still
// holds the value that was read. If another goroutine got there first, CompareAndSwap fails and
// nothing is written, so no update is lost; the loop then retries with the fresh value.
func (s *casSum) add(delta int64) (retries int) {
	for {
		old := s.sum.Load()
		if s.sum.CompareAndSwap(old, old+delta) {
			return retries
		}
		retries++
	}
}

func (s *casSum) get() int64 {
	return s.sum.Load()
}
