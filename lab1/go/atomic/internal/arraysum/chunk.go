package arraysum

import "fmt"

// chunkRange is a half-open index range [start, end) of a slice.
type chunkRange struct {
	start, end int
}

func (r chunkRange) length() int {
	return r.end - r.start
}

// splitRange splits [0, size) into parts contiguous ranges that together cover every index exactly
// once. Range lengths differ by at most one: the first size%parts ranges get one extra element.
// If parts > size, the trailing ranges are empty.
func splitRange(size, parts int) ([]chunkRange, error) {
	if size < 0 {
		return nil, fmt.Errorf("size must be >= 0, was %d", size)
	}
	if parts < 1 {
		return nil, fmt.Errorf("parts must be >= 1, was %d", parts)
	}

	base := size / parts
	remainder := size % parts

	ranges := make([]chunkRange, parts)
	start := 0
	for i := range ranges {
		end := start + base
		if i < remainder {
			end++
		}
		ranges[i] = chunkRange{start, end}
		start = end
	}
	return ranges, nil
}
