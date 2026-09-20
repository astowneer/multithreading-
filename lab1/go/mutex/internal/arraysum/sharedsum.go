package arraysum

import "sync"

// sharedSum is a goroutine-safe accumulator that workers add their partial sums to. The mutex is
// what makes concurrent add calls safe: without it, two goroutines could read the same sum and
// one of the additions would be lost.
type sharedSum struct {
	mu  sync.Mutex
	sum int64
}

func (s *sharedSum) add(value int64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.sum += value
}

func (s *sharedSum) get() int64 {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.sum
}
