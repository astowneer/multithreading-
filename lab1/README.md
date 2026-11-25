# Lab 1 - Parallel array sum

Course: Parallel Programming for Multiprocessor Computing Systems

**Task:** parallelization of the array element sum calculation algorithm (splitting the array into
parts).

## Implementations

| Language | Approach | Description |
|----------|----------|-------------|
| Java | [manual-threads](java/manual-threads) | One explicit `Thread` per part of the array; partial sums are added into a synchronized shared total. |
| Go | [mutex](go/mutex) | One goroutine per part of the array; partial sums are added into a shared total protected by a `sync.Mutex`, and a `sync.WaitGroup` waits for the goroutines. |
| Go | [divide-and-conquer](go/divide-and-conquer) | Recursive fork-join: the array is halved, a new goroutine sums one half while the current one sums the other, and the halves are joined over a channel. |

Each approach is a self-contained project with its own build files and README.
