package arraysum

import (
	"fmt"
	"testing"
)

const benchSize = 10_000_000

// sink keeps the compiler from discarding the benchmarked sums as unused.
var sink int64

func newBenchArray(b *testing.B) []int32 {
	b.Helper()
	arr := make([]int32, benchSize)
	if err := FillRandomParallel(arr, 4); err != nil {
		b.Fatal(err)
	}
	return arr
}

func BenchmarkSequential(b *testing.B) {
	arr := newBenchArray(b)
	b.SetBytes(int64(len(arr)) * 4)
	b.ResetTimer()

	for i := 0; i < b.N; i++ {
		sink = Sequential(arr)
	}
}

func BenchmarkParallel(b *testing.B) {
	arr := newBenchArray(b)

	for _, workers := range []int{1, 2, 4, 8} {
		b.Run(fmt.Sprintf("workers=%d", workers), func(b *testing.B) {
			b.SetBytes(int64(len(arr)) * 4)
			b.ResetTimer()

			for i := 0; i < b.N; i++ {
				sum, err := Parallel(arr, workers)
				if err != nil {
					b.Fatal(err)
				}
				sink = sum
			}
		})
	}
}
