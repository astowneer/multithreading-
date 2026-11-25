# Lab 1 - Go - cond

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part of the array, synchronized with `sync.Mutex` and
`sync.Cond`. The array is split into contiguous parts, each part is summed by its own goroutine,
and each goroutine adds its partial sum to a collector and signals a condition variable. The
calling goroutine sleeps on the condition until all partial sums have arrived. There is no
`WaitGroup`. The result is compared with a sequential sum, and the timings of both and the
speedup (sequential time divided by multithreaded time) are printed.

## How a condition variable works

A `sync.Cond` lets a goroutine sleep until some condition on shared data becomes true. It is
always paired with the mutex that guards that data:

```go
// worker                         // waiting goroutine
c.mu.Lock()                       c.mu.Lock()
c.sum += partial                  for c.arrived < n { // a loop, never a plain if
c.arrived++                           c.cond.Wait()   // unlock mu, sleep, lock mu again
c.mu.Unlock()                     }
c.cond.Broadcast()                total := c.sum
                                  c.mu.Unlock()
```

- **`Wait` unlocks the mutex and goes to sleep in one step**, and locks the mutex again before it
  returns. Without that, a worker could add the last sum and signal in the gap between the check
  and the sleep, and the waiter would sleep forever (a *lost wake-up*).
- **The condition is checked in a `for` loop.** Waking up only means "something changed", not "the
  condition is true". Here every `add` wakes the waiter, but only the last one makes
  `arrived == n`.
- **`Broadcast` wakes every waiter, `Signal` wakes one.** There is one waiter here, so either works.
  `Broadcast` stays correct if more are added.

## Why it is rarely used in Go

`sync.Cond` is how Java's `wait`/`notifyAll` and POSIX `pthread_cond_t` work, and this lab shows
the primitive that higher-level tools are built from. In Go code it is rare: `sync.WaitGroup`
covers "wait for N goroutines" (see [mutex](../mutex)), and a channel covers "wait for a value"
(see [channels](../channels)), both with less room for mistakes. `sync.Cond` is the right tool when
many goroutines wait for a condition on shared state that changes many times, such as a bounded
queue waking producers and consumers.

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

`-race` turns on Go's data race detector. The collector tests check that `wait` does not
return before every partial sum has arrived.
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
  collector.go               collector: partial sums under a mutex, waited for with sync.Cond
  chunk.go                   splits [0, size) into balanced parts (lengths differ by at most 1)
  *_test.go                  tests and benchmarks, next to the code they cover
```
