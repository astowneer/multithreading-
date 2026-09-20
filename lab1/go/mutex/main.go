// Command lab1-array-sum fills an array, sums it with goroutines and without, and compares the
// results.
package main

import (
	"errors"
	"fmt"
	"io"
	"os"
	"strconv"
	"time"
)

const (
	defaultSize    = 1_000_000_000
	defaultThreads = 4

	exitOK      = 0
	exitFailure = 1
	exitUsage   = 2

	usage = "Usage: go run . [size] [threads]"
)

func main() {
	os.Exit(run(os.Args[1:], os.Stdout, os.Stderr))
}

func run(args []string, stdout, stderr io.Writer) int {
	size, threads, err := parseArgs(args)
	if err != nil {
		fmt.Fprintln(stderr, err)
		fmt.Fprintln(stderr, usage)
		return exitUsage
	}

	arr := make([]int32, size)
	if err := fillRandomParallel(arr, threads); err != nil {
		fmt.Fprintln(stderr, err)
		return exitFailure
	}

	start := time.Now()
	parallel, err := parallelSum(arr, threads)
	if err != nil {
		fmt.Fprintln(stderr, err)
		return exitFailure
	}
	parallelSeconds := time.Since(start).Seconds()
	fmt.Fprintf(stdout, "Multithreaded time (seconds): %.6f\n", parallelSeconds)
	fmt.Fprintf(stdout, "SUM multithreaded: %d\n", parallel)

	start = time.Now()
	sequential := sequentialSum(arr)
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

func parseArgs(args []string) (size, threads int, err error) {
	if len(args) > 2 {
		return 0, 0, errors.New("Too many arguments")
	}

	size, threads = defaultSize, defaultThreads
	if len(args) > 0 {
		if size, err = parsePositive(args[0], "size"); err != nil {
			return 0, 0, err
		}
	}
	if len(args) > 1 {
		if threads, err = parsePositive(args[1], "threads"); err != nil {
			return 0, 0, err
		}
	}
	return size, threads, nil
}

// parsePositive parses a positive 32-bit integer, the same range as a Java int.
func parsePositive(text, name string) (int, error) {
	value, err := strconv.ParseInt(text, 10, 32)
	if err != nil || value < 1 {
		return 0, fmt.Errorf("%s must be a positive integer, was '%s'", name, text)
	}
	return int(value), nil
}
