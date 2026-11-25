# Lab 1 - Go - channels

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part of the array, synchronized only with a channel. The array is
split into contiguous parts, each part is summed by its own goroutine, and each goroutine sends its
partial sum into a buffered channel. The calling goroutine receives one value per part and adds
them up. The result is compared with a sequential sum, and the timings of both and the speedup
(sequential time divided by multithreaded time) are printed.

## Why there is no mutex and no WaitGroup

The Go proverb is: *don't communicate by sharing memory; share memory by communicating.*

```go
results := make(chan int64, len(ranges)) // room for every partial sum
for _, r := range ranges {
	go func(part []int32) { results <- Sequential(part) }(arr[r.start:r.end])
}
var total int64
for range ranges { // exactly one receive per goroutine
	total += <-results
}
```

- **No shared total.** Only the receiving goroutine writes `total`. The workers own their partial
  sums until they send them. A send on a channel *happens before* the matching receive completes,
  so the receiver always sees the full value.
- **No WaitGroup.** The loop receives exactly as many values as there are goroutines, so when it
  ends, all of them have finished their work. The receive is both the transfer of the data and
  the wait.
- **Buffered channel.** With capacity `len(ranges)` a send never blocks, so a goroutine sends and
  exits even if the receiver is busy. With an unbuffered channel the program would still be
  correct, but each worker would wait for its receive.

Filling the array still uses `sync.WaitGroup`, because the fill produces no values to send back.

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

`-race` turns on Go's data race detector. Having the goroutines write into a shared `total`
instead of sending makes the tests fail under it.
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
  chunk.go                   splits [0, size) into balanced parts (lengths differ by at most 1)
  *_test.go                  tests and benchmarks, next to the code they cover
```
