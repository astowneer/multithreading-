# Lab 1 - Go - atomic

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part of the array, synchronized with `sync.WaitGroup` and
`sync/atomic`. The array is split into contiguous parts, each part is summed by its own goroutine,
and each partial sum is added to a shared `atomic.Int64` with `Add`. The result is compared with a
sequential sum, and the timings of both and the speedup (sequential time divided by multithreaded
time) are printed.

## How it differs from the mutex approach

| | [mutex](../mutex) | atomic |
|---|---|---|
| Shared total | `int64` guarded by `sync.Mutex` | `atomic.Int64` |
| Adding a partial sum | `Lock`, `sum += v`, `Unlock` | `total.Add(v)` |
| What the CPU does | may park the goroutine while another one holds the lock | one indivisible instruction (`LOCK XADD` on amd64, `LDADD` on arm64) |
| Blocking | yes, goroutines can wait for the lock | no, this is lock-free |
| Works for | any critical section, however long | a single number: add, load, store, swap, compare-and-swap |

Atomics are the cheapest way to share one counter. They don't scale to anything bigger: two
values that must change together still need a mutex.

Here each goroutine touches the shared total only once, after summing its part locally, so the
speed is about the same as the mutex version. The difference would show if goroutines added every
element to the shared total. The Java counterpart is `AtomicLong` (`lab1/java/atomic-long`).

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

`-race` turns on Go's data race detector. Replacing `total.Add` with a plain `+=` on an `int64`
makes the tests fail under it.
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
