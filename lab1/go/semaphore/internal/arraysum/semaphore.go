package arraysum

// semaphore is a counting semaphore made of a buffered channel: the channel's capacity is the
// number of permits. acquire puts a token in and blocks while the channel is full, that is while
// every permit is taken; release takes a token out, which unblocks one waiting acquire.
//
// The tokens are empty structs, which take no memory: only the count matters.
type semaphore chan struct{}

func newSemaphore(permits int) semaphore {
	return make(semaphore, permits)
}

func (s semaphore) acquire() {
	s <- struct{}{}
}

func (s semaphore) release() {
	<-s
}
