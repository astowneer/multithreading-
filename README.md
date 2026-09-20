# Multithreading labs

Labs for *Parallel Programming for Multiprocessor Computing Systems*. Each lab can be implemented
in several languages, and each language can have several approaches.

## Labs

| Lab | Task |
|-----|------|
| [lab1](lab1) | Parallel array sum |

## Layout

```
<lab>/<language>/<approach>/
```

```
labN/
  README.md            task description and an index of the implementations
  <language>/          java, go, rust, ...
    <approach>/        one self-contained project per approach
```

For example, `lab1/java/manual-threads/` is Lab 1, in Java, using explicit threads.

## Conventions

- **Lab folders** are `lab` followed by the number: `lab1`, `lab2`, ...
- **Language folders** are the lowercase language name: `java`, `go`, `rust`, ...
- **Approach folders** are lowercase and hyphenated, and name the technique: `manual-threads`,
  `thread-pool`, `parallel-streams`, ...
- **Every approach is self-contained.** It has its own build files (`pom.xml`, `go.mod`,
  `Cargo.toml`, ...), its own README explaining how to build and run it, and, if needed, its own
  `.gitignore` for build output. Nothing is shared between approaches, so each one builds on its own.
- The root `.gitignore` only covers OS and IDE files that apply to every language.

## Adding an implementation

1. Create `<lab>/<language>/<approach>/` (and `<lab>/README.md` if the lab is new).
2. Put the project and a README there. The README says which approach it uses and how to build,
   test and run it.
3. Add a row to the implementations table in `<lab>/README.md`.
4. For a new lab, add it to the table above.
