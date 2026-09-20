# Lab 1 - Go - mutex

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part of the array, synchronized with `sync.WaitGroup` and
`sync.Mutex`. The array is split into contiguous parts, each part is summed by its own goroutine,
and the partial sums are added into a shared total protected by a mutex. The result is compared
with a sequential sum, and the timings of both and the speedup (sequential time divided by
multithreaded time) are printed.

Go has other synchronisation tools (channels, `sync/atomic`), which are meant to be separate
approaches next to this one.

## Requirements

- Go 1.22 or newer (`brew install go` on macOS). There are no external dependencies.

## Build and test

```sh
gofmt -l .          # lists files that are not formatted; no output means all is well
go vet ./...
go test -race ./...
```

`-race` turns on Go's data race detector. The tests fail under it if the mutex is removed.

## Run

```sh
go run . [size] [threads]
```

| Argument  | Default         | Meaning                         |
|-----------|-----------------|---------------------------------|
| `size`    | `1000000000`    | number of array elements        |
| `threads` | `4`             | number of worker goroutines     |

The default array is `[]int32` of 1 billion elements, about 4 GB of memory. Unlike Java, Go needs no
`-Xmx` flag because the heap grows as needed, but the program is killed if the machine runs out of
memory. For a quick run or on a machine with little memory use a smaller size, e.g.
`go run . 100000000 8`.

To run the compiled program:

```sh
go build -o bin/lab1-array-sum .
./bin/lab1-array-sum 100000000 8
```

The exit code is `0` if the parallel and sequential sums match, `1` if they differ, and `2` for
invalid arguments. `go run` prints `exit status 2` and exits with `1` itself; run the compiled
program to see the real code.

Example output:

```
Multithreaded time (seconds): 0.014619
SUM multithreaded: 4950119327
No threads time (seconds): 0.041212
SUM no threads: 4950119327
Speedup: 2.82x
Results match: true
```

## Layout

```
go.mod           module definition (module lab1-array-sum, no external dependencies)
main.go          command-line entry point and timing
arraysum.go      sequential sum, parallel sum, parallel random fill, goroutine runner
chunk.go         splits [0, size) into balanced parts (lengths differ by at most 1)
sharedsum.go     mutex-protected accumulator for the partial sums
*_test.go        tests, next to the code they test
```
