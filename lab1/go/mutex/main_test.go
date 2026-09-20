package main

import (
	"bytes"
	"io"
	"strings"
	"testing"
)

func runCapturing(args ...string) (code int, stdout, stderr string) {
	var out, errOut bytes.Buffer
	code = run(args, &out, &errOut)
	return code, out.String(), errOut.String()
}

func TestRunPrintsTimingsSumsAndSpeedup(t *testing.T) {
	code, stdout, stderr := runCapturing("-size", "100003", "-workers", "3")

	if code != exitOK {
		t.Fatalf("exit code = %d, want %d (stderr: %q)", code, exitOK, stderr)
	}
	for _, want := range []string{
		"Multithreaded time (seconds): ",
		"SUM multithreaded: ",
		"No threads time (seconds): ",
		"SUM no threads: ",
		"Speedup: ",
		"Results match: true",
	} {
		if !strings.Contains(stdout, want) {
			t.Errorf("stdout is missing %q:\n%s", want, stdout)
		}
	}
}

func TestRunRejectsInvalidArguments(t *testing.T) {
	cases := []struct {
		name string
		args []string
	}{
		{"unexpected positional argument", []string{"100"}},
		{"unknown flag", []string{"-threads", "4"}},
		{"size zero", []string{"-size", "0"}},
		{"size negative", []string{"-size", "-5"}},
		{"size not a number", []string{"-size", "abc"}},
		{"size above the maximum", []string{"-size", "3000000000"}},
		{"workers zero", []string{"-size", "10", "-workers", "0"}},
		{"workers not a number", []string{"-size", "10", "-workers", "x"}},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			code, stdout, stderr := runCapturing(c.args...)

			if code != exitUsage {
				t.Errorf("exit code = %d, want %d", code, exitUsage)
			}
			if stdout != "" {
				t.Errorf("unexpected stdout %q", stdout)
			}
			if !strings.Contains(stderr, "-workers") {
				t.Errorf("stderr does not show the usage: %q", stderr)
			}
		})
	}
}

func TestRunHelpShowsUsageAndSucceeds(t *testing.T) {
	code, _, stderr := runCapturing("-h")

	if code != exitOK {
		t.Errorf("exit code = %d, want %d", code, exitOK)
	}
	for _, want := range []string{"-size", "-workers"} {
		if !strings.Contains(stderr, want) {
			t.Errorf("usage is missing %q: %q", want, stderr)
		}
	}
}

func TestParseFlags(t *testing.T) {
	cases := []struct {
		name        string
		args        []string
		wantSize    int
		wantWorkers int
	}{
		{"defaults", nil, defaultSize, defaultWorkers},
		{"size only", []string{"-size", "50"}, 50, defaultWorkers},
		{"workers only", []string{"-workers", "8"}, defaultSize, 8},
		{"both", []string{"-size", "50", "-workers", "8"}, 50, 8},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			size, workers, err := parseFlags(c.args, io.Discard)

			if err != nil {
				t.Fatal(err)
			}
			if size != c.wantSize || workers != c.wantWorkers {
				t.Errorf("got (%d, %d), want (%d, %d)", size, workers, c.wantSize, c.wantWorkers)
			}
		})
	}
}
