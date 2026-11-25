# Lab 1 - Parallel array sum

Course: Parallel Programming for Multiprocessor Computing Systems

**Task:** parallelization of the array element sum calculation algorithm (splitting the array into
parts).

## Implementations

| Language | Approach | Description |
|----------|----------|-------------|
| Java | [manual-threads](java/manual-threads) | One explicit `Thread` per part of the array; partial sums are added into a synchronized shared total. |
| Java | [atomic-long](java/atomic-long) | One `Thread` per part; partial sums are added into a lock-free `AtomicLong` (CAS). |
| Java | [long-adder](java/long-adder) | One `Thread` per part; partial sums are added into a striped `LongAdder`. |
| Java | [reentrant-lock](java/reentrant-lock) | One `Thread` per part; the shared total is guarded by an explicit `ReentrantLock`. |
| Java | [partial-sums](java/partial-sums) | One `Thread` per part, no shared state: each task keeps its own result, and `main` adds them after `join()`. |
| Java | [executor-service](java/executor-service) | A fixed thread pool runs one `Callable<Long>` per part; results are collected from `Future`s. |
| Java | [completable-future](java/completable-future) | `supplyAsync` per part on a pool; the partial results are combined with `thenCombine`. |
| Java | [fork-join](java/fork-join) | A `RecursiveTask` halves the range until it is small, on a `ForkJoinPool` with work stealing. |
| Java | [parallel-streams](java/parallel-streams) | `Arrays.stream(arr).parallel().asLongStream().sum()`, on the common pool or a dedicated one. |
| Java | [parallel-prefix](java/parallel-prefix) | `Arrays.parallelPrefix` computes every running total in parallel; the last one is the sum. |
| Java | [virtual-threads](java/virtual-threads) | One virtual thread per part (Java 21), with a `Thread.Builder` and with a virtual-thread-per-task executor. |
| Java | [countdown-latch](java/countdown-latch) | One `Thread` per part; `main` waits on a `CountDownLatch` instead of `join()`. |
| Java | [cyclic-barrier](java/cyclic-barrier) | Workers meet at a `CyclicBarrier`, whose barrier action adds the partial sums. |
| Java | [phaser](java/phaser) | Workers register with a `Phaser` at runtime and deregister when done; `main` waits for the phase to end. |
| Go | [mutex](go/mutex) | One goroutine per part of the array; partial sums are added into a shared total protected by a `sync.Mutex`, and a `sync.WaitGroup` waits for the goroutines. |

Each approach is a self-contained project with its own build files and README.

## Java approaches at a glance

Every Java approach is a copy of `manual-threads` with only the parallel technique changed, so
`diff -ru java/manual-threads/src java/<approach>/src` shows exactly what each technique needs.

The approaches fall into four groups, from lowest to highest level:

| Group | Approaches | What you learn |
|---|---|---|
| How to combine partial sums safely | manual-threads (`synchronized`), reentrant-lock, atomic-long, long-adder, partial-sums | mutual exclusion, CAS, contention, happens-before, why to avoid shared state |
| How to wait for the workers | countdown-latch, cyclic-barrier, phaser (and `join()` everywhere else) | the `java.util.concurrent` synchronizers |
| Who owns the threads | executor-service, completable-future, virtual-threads | thread pools, `Callable`/`Future`, async composition, cheap threads |
| Let the framework split the work | fork-join, parallel-streams, parallel-prefix | divide and conquer, work stealing, reductions and scans |

What to reach for in real code:

- **CPU-bound work on an array or collection**: `parallel-streams` first. Use `fork-join` when the
  problem is naturally recursive.
- **Independent tasks with results**: `executor-service`, or `completable-future` when the tasks
  form a pipeline.
- **Many blocking tasks (I/O)**: `virtual-threads`.
- **A shared counter**: `long-adder` if it is written often and read rarely, `atomic-long`
  otherwise, and a lock only when several fields must change together.
- **Coordinating phases**: `cyclic-barrier`, or `phaser` when the number of parties changes.

For a 1-billion-element sum the parallel time is limited by **memory bandwidth**, not by the
technique. Once all cores are streaming from RAM, every approach takes roughly the same time.
