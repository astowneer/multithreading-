# Lab 1 - Parallel array sum

Course: Parallel Programming for Multiprocessor Computing Systems

**Task:** parallelization of the array element sum calculation algorithm (splitting the array into
parts).

## Implementations

| Language | Approach | Description |
|----------|----------|-------------|
| Java | [manual-threads](java/manual-threads) | One explicit `Thread` per part of the array; partial sums are added into a synchronized shared total. |
| Go | [mutex](go/mutex) | One goroutine per part of the array; partial sums are added into a shared total protected by a `sync.Mutex`, and a `sync.WaitGroup` waits for the goroutines. |
| Go | [indexed-results](go/indexed-results) | One goroutine per part of the array; each goroutine writes its partial sum into its own slice slot, so only a `sync.WaitGroup` is needed. |

Each approach is a self-contained project with its own build files and README.
