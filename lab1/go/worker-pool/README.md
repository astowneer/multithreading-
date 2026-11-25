# Lab 1 - Go - worker-pool

Implementation of [Lab 1](../../README.md) in Go.

**Approach:** a fixed pool of worker goroutines fed by channels. The array is split into
`workers × ChunksPerWorker` (4) contiguous parts. A producer goroutine sends the parts into a
`jobs` channel, `workers` goroutines take parts from it one at a time and send each part's sum into
a `results` channel, and the calling goroutine adds up everything it receives. The result is
compared with a sequential sum, and the timings of both and the speedup (sequential time divided
by multithreaded time) are printed.

## How the pool is wired

```
               jobs (chan chunkRange)            results (chan int64)
producer ──▶ ┌──────────────────────┐ ──▶ worker 1 ──▶ ┌────────────────┐
  sends      │ part part part part …│ ──▶ worker 2 ──▶ │ sum sum sum …  │ ──▶ caller adds up
  all parts, └──────────────────────┘ ──▶ worker N ──▶ └────────────────┘
  closes jobs                                          closer: wg.Wait(), close(results)
```

- **`for job := range jobs`** in each worker ends when `jobs` is closed and empty. Closing a channel
  is how the producer says "no more work".
- **The closer goroutine** waits for every worker (`sync.WaitGroup`) and then closes `results`, so
  the caller's `for result := range results` ends after the last sum.
- **Each channel is closed by its only sender, after its last send.** Close it earlier, or from a
  receiver, and some goroutine panics with "send on closed channel".

## Why more parts than workers

In the other approaches every goroutine gets exactly one part. If one core is slower (busy with
another program, or an efficiency core on Apple silicon) the whole sum waits for it. Here a worker
that finishes early takes the next part from the channel, so the work spreads itself across the
fast cores. This is load balancing, and it matters most when parts take unequal time. For an
array sum the parts are equal, so the gain is small and the extra channel operations cost a
little.

The pool also caps the number of goroutines at `workers` however many parts there are. That is
the reason to use a pool when the jobs are many: thousands of files or requests, not 4 array
parts.

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

`-race` turns on Go's data race detector. `TestRunPoolNeverRunsMoreTasksThanWorkers` also
checks that at most `workers` tasks ever run at the same moment.
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
  pool.go                    runPool: fixed pool of workers with jobs and results channels
  chunk.go                   splits [0, size) into balanced parts (lengths differ by at most 1)
  *_test.go                  tests and benchmarks, next to the code they cover
```
