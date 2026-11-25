# Lab 1 - Go - indexed-results

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** one goroutine per part of the array, synchronized only with `sync.WaitGroup`. The
array is split into contiguous parts, each part is summed by its own goroutine, and goroutine `i`
writes its partial sum into `partials[i]`. After `wg.Wait()` the calling goroutine adds the slots
up. The result is compared with a sequential sum, and the timings of both and the speedup
(sequential time divided by multithreaded time) are printed.

## Why no lock is needed

A data race needs two goroutines accessing the *same* memory location, at least one of them
writing. Here no location is shared while the goroutines run:

- each goroutine reads only its own part of `arr`;
- each goroutine writes only its own `partials[i]`, and different slice elements are different
  memory locations;
- the calling goroutine reads `partials` only after `wg.Wait()`. Each `wg.Done()` *happens before*
  the `Wait` it releases returns, so every write is visible by then.

This is the cheapest correct approach: no lock, no atomic instruction and no channel, just one wait
at the end. It is also what the [mutex](../mutex), [atomic](../atomic) and [channels](../channels)
approaches do internally in their local `Sequential` calls. They only differ in how the partial
sums are combined.

**False sharing.** The slots sit next to each other in memory, so several of them share one CPU
cache line (64 bytes on most x86, 128 on Apple silicon). Writes from different cores to the same
cache line make the cores pass the line back and forth. Here every goroutine writes its slot
once, so this costs nothing measurable. Had they written `partials[i] += value` for every element,
it would slow the program down badly, and the fix would be to sum into a local variable, as this
code does, or to pad each slot to a full cache line.

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

`-race` turns on Go's data race detector. Making two goroutines write the same slot (for
example `partials[0]`) makes the tests fail under it.
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
