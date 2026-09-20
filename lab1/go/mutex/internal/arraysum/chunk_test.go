package arraysum

import (
	"fmt"
	"reflect"
	"testing"
)

func TestSplitRangeCoversEveryIndexOnceAndIsBalanced(t *testing.T) {
	cases := []struct{ size, parts int }{
		{0, 1}, {0, 4}, {1, 4}, {3, 4}, {10, 3}, {10, 8}, {100, 7}, {100000003, 4}, {1000000000, 7},
	}
	for _, c := range cases {
		t.Run(fmt.Sprintf("size=%d parts=%d", c.size, c.parts), func(t *testing.T) {
			ranges, err := splitRange(c.size, c.parts)
			if err != nil {
				t.Fatal(err)
			}

			if len(ranges) != c.parts {
				t.Fatalf("got %d ranges", len(ranges))
			}
			if ranges[0].start != 0 {
				t.Errorf("first range starts at %d", ranges[0].start)
			}
			if last := ranges[c.parts-1].end; last != c.size {
				t.Errorf("last range ends at %d", last)
			}

			shortest, longest := ranges[0].length(), ranges[0].length()
			for i := 1; i < len(ranges); i++ {
				if ranges[i-1].end != ranges[i].start {
					t.Errorf("gap or overlap at part %d", i)
				}
				shortest = min(shortest, ranges[i].length())
				longest = max(longest, ranges[i].length())
			}
			if longest-shortest > 1 {
				t.Errorf("lengths differ by more than one (%d to %d)", shortest, longest)
			}
		})
	}
}

func TestSplitRangeGivesExtraElementsToFirstParts(t *testing.T) {
	got, err := splitRange(10, 3)
	if err != nil {
		t.Fatal(err)
	}
	want := []chunkRange{{0, 4}, {4, 7}, {7, 10}}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("splitRange(10, 3) = %v, want %v", got, want)
	}
}

func TestSplitRangeWithMorePartsThanElementsLeavesTrailingRangesEmpty(t *testing.T) {
	ranges, err := splitRange(2, 4)
	if err != nil {
		t.Fatal(err)
	}
	for i, wantLength := range []int{1, 1, 0, 0} {
		if got := ranges[i].length(); got != wantLength {
			t.Errorf("part %d has length %d, want %d", i, got, wantLength)
		}
	}
}

func TestSplitRangeRejectsInvalidArguments(t *testing.T) {
	if _, err := splitRange(-1, 2); err == nil {
		t.Error("splitRange(-1, 2) returned no error")
	}
	if _, err := splitRange(10, 0); err == nil {
		t.Error("splitRange(10, 0) returned no error")
	}
}
