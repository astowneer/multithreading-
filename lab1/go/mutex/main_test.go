package main

import (
	"bytes"
	"strings"
	"testing"
)

func runCapturing(args ...string) (code int, stdout, stderr string) {
	var out, errOut bytes.Buffer
	code = run(args, &out, &errOut)
	return code, out.String(), errOut.String()
}

func TestRunPrintsTimingsSumsAndSpeedup(t *testing.T) {
	code, stdout, stderr := runCapturing("100003", "3")

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
	cases := [][]string{
		{"1", "2", "3"},
		{"0"},
		{"-5"},
		{"abc"},
		{"10", "0"},
		{"10", "x"},
		{"3000000000"},
	}
	for _, args := range cases {
		code, stdout, stderr := runCapturing(args...)

		if code != exitUsage {
			t.Errorf("run(%q): exit code = %d, want %d", args, code, exitUsage)
		}
		if stdout != "" {
			t.Errorf("run(%q): unexpected stdout %q", args, stdout)
		}
		if !strings.Contains(stderr, usage) {
			t.Errorf("run(%q): stderr does not show the usage: %q", args, stderr)
		}
	}
}

func TestParseArgsUsesDefaults(t *testing.T) {
	size, threads, err := parseArgs(nil)
	if err != nil {
		t.Fatal(err)
	}
	if size != defaultSize || threads != defaultThreads {
		t.Errorf("parseArgs(nil) = (%d, %d), want (%d, %d)", size, threads, defaultSize, defaultThreads)
	}

	size, threads, err = parseArgs([]string{"50"})
	if err != nil {
		t.Fatal(err)
	}
	if size != 50 || threads != defaultThreads {
		t.Errorf("parseArgs([50]) = (%d, %d), want (50, %d)", size, threads, defaultThreads)
	}
}
