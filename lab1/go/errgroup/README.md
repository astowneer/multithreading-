# Lab 1 - Go - errgroup

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part of the array, managed by `errgroup.Group` from
`golang.org/x/sync`. The array is split into contiguous parts, `g.Go` starts a goroutine per part,
each goroutine writes its partial sum into its own slot of a slice, and `g.Wait()` waits for all of
them and returns the first error. The goroutines share a context that is cancelled on the first
error or on Ctrl+C, and they stop early when it is. The result is compared with a sequential sum,
and the timings of both and the speedup (sequential time divided by multithreaded time) are
printed.

## What errgroup adds over a WaitGroup

| | `sync.WaitGroup` | `errgroup.Group` |
|---|---|---|
| Start | `wg.Add(1)` + `go func() { defer wg.Done(); … }()` | `g.Go(func() error { … })` |
| Wait | `wg.Wait()` | `err := g.Wait()`: the first non-nil error |
| Errors | collect them yourself | returned by `Wait` |
| Cancel the others | build it yourself | `errgroup.WithContext`: the context is cancelled on the first error |
| Limit concurrency | a semaphore of your own | `g.SetLimit(n)` |

`golang.org/x/...` packages are maintained by the Go team but sit outside the standard library,
so they are versioned in `go.mod` like any other dependency.

## Cancellation

```go
g, ctx := errgroup.WithContext(parent) // ctx is cancelled when any g.Go func fails, or parent is
for i, r := range ranges {
	g.Go(func() error {
		sum, err := sumContext(ctx, arr[r.start:r.end]) // checks ctx.Err() every 65 536 elements
		partials[i] = sum
		return err
	})
}
err := g.Wait()
```

Go cannot kill a goroutine from outside. A goroutine stops only if it checks a signal itself, and
`ctx.Err()` is the conventional signal. `main` gets its context from
`signal.NotifyContext(…, os.Interrupt)`, so Ctrl+C during a long sum stops all the goroutines
within microseconds and the program prints `interrupted` and exits with code `130`. To try it, press
Ctrl+C while `go run .` fills the default 1-billion-element array. The signal is caught, so the
fill finishes, and then the sum stops at once.

The loop uses Go 1.22+ per-iteration loop variables: each `g.Go` closure gets its own `i` and `r`,
so they don't need to be passed as arguments.

## Requirements

- Go 1.26 or newer (`brew install go` on macOS), which `golang.org/x/sync` needs. It is the only
  dependency, and `go` downloads it by itself.

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
`2` for invalid arguments and `130` when stopped with Ctrl+C. `go run` prints `exit status 2` and exits with `1` itself; run the
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

`-race` turns on Go's data race detector. The cancellation tests check that an expired
deadline stops the sum early with `context.DeadlineExceeded`.
`staticcheck` is a linter, the same one CI runs.

## Benchmarks

```sh
go test -run '^$' -bench . ./internal/arraysum
```

Sums a 10-million-element array sequentially and with 1, 2, 4 and 8 workers, repeating each
measurement many times. This is steadier than the single measurement printed by the program.

## Layout

```
go.mod                       module definition; requires golang.org/x/sync
main.go                      command-line entry point and timing
main_test.go                 tests for the command line
internal/arraysum/
  arraysum.go                Sequential, Parallel, ParallelContext and FillRandomParallel
  cancel_test.go             tests for stopping early on a cancelled context
  chunk.go                   splits [0, size) into balanced parts (lengths differ by at most 1)
  *_test.go                  tests and benchmarks, next to the code they cover
```
