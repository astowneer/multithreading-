# Lab 1 - Go - goroutines

Ibrew install go of [Lab 1](../../README.md) in Go.

**Status:** hello world only, to check that the toolchain works. The array sum is not implemented
yet.

**Planned approach:** one goroutine per part of the array, synchronized with `sync.WaitGroup`, with
the partial sums combined into a total.

## Requirements

- Go 1.21 or newer (`brew install go` on macOS).

## Build, vet and test

```sh
go build ./...
go vet ./...
go test ./...
```

## Run

```sh
go run .
```

Expected output:

```
Hello, World!
```

## Layout

```
go.mod    module definition (module lab1-array-sum, no external dependencies)
main.go   command-line entry point
```

The array sum code will be added as further `.go` files in this folder (package `main`), with
`_test.go` files next to the code they test.
