package arraysum

import (
	"testing"
	"time"
)

func TestCollectorWaitReturnsOnceEverySumHasArrived(t *testing.T) {
	c := newCollector()
	result := make(chan int64)
	go func() { result <- c.wait(3) }()

	c.add(10)
	c.add(20)
	select {
	case got := <-result:
		t.Fatalf("wait returned %d after 2 of 3 sums", got)
	case <-time.After(20 * time.Millisecond):
	}

	c.add(30)
	select {
	case got := <-result:
		if got != 60 {
			t.Errorf("wait = %d, want 60", got)
		}
	case <-time.After(time.Second):
		t.Fatal("wait still blocked after all 3 sums arrived")
	}
}

func TestCollectorWaitForZeroReturnsAtOnce(t *testing.T) {
	if got := newCollector().wait(0); got != 0 {
		t.Errorf("wait(0) = %d, want 0", got)
	}
}

func TestCollectorWaitReturnsIfSumsArrivedBeforeIt(t *testing.T) {
	c := newCollector()
	c.add(1)
	c.add(2)
	if got := c.wait(2); got != 3 {
		t.Errorf("wait = %d, want 3", got)
	}
}
