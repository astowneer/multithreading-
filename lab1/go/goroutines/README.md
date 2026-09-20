# Lab 1 - Go - goroutines

Ibrew install go of [Lab 1](../../README.md) in Go.

**Status:** hello world only, to check that the toolchain works. The array sum is not implemented
yet.

**Planned approach:** one goroutine per part of the array, synchronized with `sync.WaitGroup`, with
the partial sums combined into a total.

## Requirements

- Go 1.21 or newer (`brew install go` on macOS).

## Run

```sh
go run .
```

Expected output:

```
Hello, World!
```
