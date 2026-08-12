# Getting started

This mirrors the root README's Quick start section with a bit more context —
if you only need the commands, the README's shorter version is enough.

## Requirements

- Recent install of the [`jank`](https://jank-lang.org) compiler and the
  `lein-jank` Leiningen plugin (`2026.06-1` or newer — older versions lack
  the native-build middleware)
- A C++ compiler
- CMake
- [Babashka](https://babashka.org)

Verified on macOS with jank `0.1-alpha` and `lein-jank 2026.06-1`.

## Clone with the submodule

`raylib` is vendored as a git submodule of the `jank-raylib-sys` wrapper, so
clone with it:

```sh
git clone --recurse-submodules git@github.com:burinc/b12n-raylib-jnk.git
# or, after a plain clone:
git submodule update --init --recursive
```

## The `bb` task surface

```sh
bb info              # grouped cheat-sheet of everything (start here)
bb examples          # list every runnable example
bb starfield         # run one (installs jank-raylib-sys on first use)
bb run particles     # same, by argument
bb run-all           # cycle through every example, ~15s each (a demo reel)
bb run-all 40        # ...longer per example (also covers first-run compiles)

bb install           # install jank-raylib-sys into ~/.m2
bb clean             # remove */target build dirs
```

If the `lein` on your `PATH` can't bootstrap, set `LEIN=/path/to/lein`.

## Manual usage (without `bb`)

```sh
cd jank-raylib-sys && lein update-in :prep-tasks empty -- install
cd raylib-examples  && lein with-profile +<example> run --disable-sandbox
```

## macOS

There is no `bwrap` on macOS, so the native build must run with sandboxing
disabled. Every `bb`/`lein` invocation above already passes
`--disable-sandbox` for you.

**Known limitation:** `rlgl-compute` needs OpenGL 4.3 compute-shader
support that macOS's native GL backend cannot provide (capped at 4.1). This
repo builds `jank-raylib-sys` at `OPENGL_VERSION "3.3"` by default — every
other example works; `rlgl-compute` does not run out of the box on any
platform against this build. See the root README's "Known limitations"
section for the manual override recipe (which still won't work on macOS).
