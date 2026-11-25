# Lab 1 - Go - divide-and-conquer

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** recursive divide and conquer in fork-join style. The array is split in two, a new
goroutine sums the left half (*fork*), the current goroutine sums the right half, and then it waits
for the left sum and adds the two (*join*). Each half is split the same way until every part has
one worker or is smaller than 4096 elements. The result is compared with a sequential sum, and the
timings of both and the speedup (sequential time divided by multithreaded time) are printed.

## The recursion

```go
func sumRecursive(arr []int32, workers int) int64 {
	if workers <= 1 || len(arr) < minForkSize {
		return Sequential(arr) // leaf: small enough, sum it here
	}
	mid, leftWorkers := splitWorkers(len(arr), workers)
	leftSum := make(chan int64, 1)
	go func() { leftSum <- sumRecursive(arr[:mid], leftWorkers) }() // fork
	right := sumRecursive(arr[mid:], workers-leftWorkers)           // keep working
	return <-leftSum + right                                         // join
}
```

With `-workers 4` the tree is:

```
                 [0 ............ n)  4 workers
                /                  \
      [0 .. n/2) 2 workers     [n/2 .. n) 2 workers
       /        \                /         \
  [0..n/4)  [n/4..n/2)     [n/2..3n/4)  [3n/4..n)     <- 4 leaves, each summed sequentially
```

- **Elements are split in proportion to workers.** With 3 workers the left third goes to 1 worker
  and the right two thirds to 2, so every leaf gets about `n / workers` elements (the tests check
  that leaves differ by at most one element).
- **The current goroutine does half the work** instead of starting two goroutines and waiting, so
  `workers` leaves need only `workers - 1` new goroutines.
- **The join is a channel receive.** The channel has room for one value, so the forked goroutine
  never waits for the receiver.
- **The threshold** (`minForkSize`) stops splitting parts so small that starting a goroutine costs
  more than summing them.

No memory is shared between goroutines: each half is a separate subslice and each sum comes back
over its own channel.

This is the shape of Java's `ForkJoinPool` with a `RecursiveTask`, and of parallel merge sort and
quicksort. Go needs no special pool for it, because goroutines are cheap enough to fork directly.

## Requirements

- Go 1.22 or newer (`brew install go` on macOS). There are no external dependencies.

## Run

```sh
go run . [-size N] [-workers N]
```

| Flag       | Default         | Meaning                                              |
|------------|-----------------|------------------------------------------------------|
| `-size`    | `1000000000`    | number of array elements (1 to 2147483647)           |
| `-workers` | `4`             | number of worker goroutines (the lab's "threads")    |

`go run . -h` prints the usage.

The default array is `[]int32` of 1 billion elements, about 4 GB of memory. Unlike Java, Go needs no
`-Xmx` flag because the heap grows as needed, but the program is killed if the machine runs out of
memory. For a quick run or on a machine with little memory use a smaller size, e.g.
`go run . -size 100000000 -workers 8`.

To run the compiled program:

```sh
go build -o bin/lab1-array-sum .
./bin/lab1-array-sum -size 100000000 -workers 8
```

The exit code is `0` if the parallel and sequential sums match (or for `-h`), `1` if they differ,
and `2` for invalid arguments. `go run` prints `exit status 2` and exits with `1` itself; run the
compiled program to see the real code.

Example output:

```
Multithreaded time (seconds): 0.014619
SUM multithreaded: 4950119327
No threads time (seconds): 0.041212
SUM no threads: 4950119327
Speedup: 2.82x
Results match: true
```

## Test and check

```sh
gofmt -l .          # lists files that are not formatted; no output means all is well
go vet ./...
go run honnef.co/go/tools/cmd/staticcheck@2026.2.1 ./...
go test -race ./...
```

`-race` turns on Go's data race detector. The recursion tests replay the splitting and check
that it makes one leaf per worker and that leaves differ by at most one element.
`staticcheck` is a linter, the same one CI runs.

## Benchmarks

```sh
go test -run '^$' -bench . ./internal/arraysum
```

Sums a 10-million-element array sequentially and with 1, 2, 4 and 8 workers, repeating each
measurement many times. This is steadier than the single measurement printed by the program.

## Layout

```
go.mod                       module definition (no external dependencies)
main.go                      command-line entry point and timing
main_test.go                 tests for the command line
internal/arraysum/
  arraysum.go                Sequential, Parallel and FillRandomParallel, and the goroutine runner
  recursive.go               sumRecursive: fork-join recursion, and splitWorkers
  chunk.go                   splits [0, size) into balanced parts (lengths differ by at most 1)
  *_test.go                  tests and benchmarks, next to the code they cover
```
