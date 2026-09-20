// Command lab1-array-sum fills an array, sums it with goroutines and without, and compares the
// results.
package main

import (
	"errors"
	"flag"
	"fmt"
	"io"
	"math"
	"os"
	"time"

	"github.com/astowneer/multithreading-/lab1/go/mutex/internal/arraysum"
)

const (
	defaultSize    = 1_000_000_000
	defaultWorkers = 4

	// maxSize is the largest array accepted, the same limit as a Java array.
	maxSize = math.MaxInt32

	exitOK      = 0
	exitFailure = 1
	exitUsage   = 2
)

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr))
}

func run(args []string, stdout, stderr io.Writer) int {
	size, workers, err := parseFlags(args, stderr)
	switch {
	case errors.Is(err, flag.ErrHelp):
		return exitOK
	case err != nil:
		return exitUsage
	}

	arr := make([]int32, size)
	if err := arraysum.FillRandomParallel(arr, workers); err != nil {
		fmt.Fprintln(stderr, err)
		return exitFailure
	}

	start := time.Now()
	parallel, err := arraysum.Parallel(arr, workers)
	if err != nil {
		fmt.Fprintln(stderr, err)
		return exitFailure
	}
	parallelSeconds := time.Since(start).Seconds()
	fmt.Fprintf(stdout, "Multithreaded time (seconds): %.6f\n", parallelSeconds)
	fmt.Fprintf(stdout, "SUM multithreaded: %d\n", parallel)

	start = time.Now()
	sequential := arraysum.Sequential(arr)
	sequentialSeconds := time.Since(start).Seconds()
	fmt.Fprintf(stdout, "No threads time (seconds): %.6f\n", sequentialSeconds)
	fmt.Fprintf(stdout, "SUM no threads: %d\n", sequential)

	fmt.Fprintf(stdout, "Speedup: %.2fx\n", sequentialSeconds/parallelSeconds)

	match := parallel == sequential
	fmt.Fprintf(stdout, "Results match: %t\n", match)
	if !match {
		return exitFailure
	}
	return exitOK
}

// parseFlags reads the -size and -workers flags. On invalid input it prints the problem and the
// usage to output and returns an error; -h prints the usage and returns flag.ErrHelp.
func parseFlags(args []string, output io.Writer) (size, workers int, err error) {
	fs := flag.NewFlagSet("lab1-array-sum", flag.ContinueOnError)
	fs.SetOutput(output)
	fs.IntVar(&size, "size", defaultSize, "number of array elements")
	fs.IntVar(&workers, "workers", defaultWorkers, "number of worker goroutines")

	if err := fs.Parse(args); err != nil {
		return 0, 0, err // Parse has already printed the problem and the usage
	}

	switch {
	case fs.NArg() > 0:
		err = fmt.Errorf("unexpected argument %q", fs.Arg(0))
	case size < 1 || size > maxSize:
		err = fmt.Errorf("size must be between 1 and %d, was %d", maxSize, size)
	case workers < 1:
		err = fmt.Errorf("workers must be at least 1, was %d", workers)
	}
	if err != nil {
		fmt.Fprintln(output, err)
		fs.Usage()
		return 0, 0, err
	}
	return size, workers, nil
}
