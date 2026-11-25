# Lab 1 - Go - semaphore

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part, with a semaphore limiting how many run at once. The array
is split into `workers × ChunksPerWorker` (4) contiguous parts. Each part gets its own goroutine,
but a counting semaphore with `workers` permits lets only `workers` of them exist at a time. Partial
sums are added into a mutex-protected total, as in the [mutex](../mutex) approach, and a
`sync.WaitGroup` waits for the last goroutine. The result is compared with a sequential sum, and
the timings of both and the speedup (sequential time divided by multithreaded time) are printed.

## A semaphore from a buffered channel

Go has no semaphore type in the standard library, because a buffered channel already is one:

```go
sem := make(chan struct{}, workers) // capacity = number of permits
sem <- struct{}{}                   // acquire: blocks while all permits are taken
<-sem                               // release: frees a permit, wakes one waiting acquire
```

`struct{}` takes no memory. The channel counts tokens, and the values don't matter. The ready-made
version is `golang.org/x/sync/semaphore`, which also lets one acquire take several permits at once.

The loop acquires a permit *before* starting each goroutine, so when all permits are taken the
loop itself waits. That keeps the number of goroutines that exist at `workers`. Acquiring inside
the goroutine would still cap how many *run*, but all of them would be started at once and wait.

## Semaphore or worker pool?

Both cap concurrency at `workers`. The [worker-pool](../worker-pool) starts `workers` long-lived
goroutines that loop over a jobs channel. The semaphore starts a short-lived goroutine per job and
limits how many are alive. The semaphore version is less plumbing (no jobs or results channels to
close). The pool is better when each worker holds something expensive to set up, such as a
connection or a buffer.

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

`-race` turns on Go's data race detector. `TestSemaphoreLimitsConcurrentGoroutines` checks that
no more goroutines than permits ever hold a permit at once.
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
  semaphore.go               counting semaphore built on a buffered channel
  sharedsum.go               mutex-protected accumulator for the partial sums
  chunk.go                   splits [0, size) into balanced parts (lengths differ by at most 1)
  *_test.go                  tests and benchmarks, next to the code they cover
```
