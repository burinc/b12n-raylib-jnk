# Getting started

This mirrors the root README's Quick start section with a bit more context;
if you only need the commands, the README's shorter version is enough.

## Requirements

- Recent install of the [`jank`](https://jank-lang.org) compiler and the
  `lein-jank` Leiningen plugin (`2026.06-1` or newer; older versions lack
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
bb starfield         # run one (fetches example assets on first use)
bb run particles     # same, by argument
bb run-all           # cycle through every example, ~15s each (a demo reel)
bb run-all 40        # ...longer per example (also covers first-run compiles)

bb check             # offline gates: syntax, registration, EDN
bb nrepl             # a jank nREPL with cpp/ interop live in it
bb clean             # remove */target build dirs
```

If the `lein` on your `PATH` can't bootstrap, set `LEIN=/path/to/lein`.

## Manual usage (without `bb`)

```sh
cd raylib-examples && lein with-profile +<example> run --disable-sandbox
```

## macOS

There is no `bwrap` on macOS, so the native build must run with sandboxing
disabled. Every `bb`/`lein` invocation above already passes
`--disable-sandbox` for you.

**Known limitation:** `rlgl-compute` needs OpenGL 4.3 compute-shader
support that macOS's native GL backend cannot provide (capped at 4.1). This
repo builds `jank-raylib-sys` at `OPENGL_VERSION "3.3"` by default, so every
other example works; `rlgl-compute` does not run out of the box on any
platform against this build. See the root README's "Known limitations"
section for the manual override recipe (which still won't work on macOS).

## Troubleshooting

### A handful of examples fail to compile, naming a raylib header you don't recognise

If a compile error cites a header outside this repo (most often
`/opt/homebrew/include/raylib.h` or `/usr/local/include/raylib.h`), you have
a system-wide raylib installed that is shadowing the vendored one, and it is
an older version than the 6.0 this repo pins.

The tell is the compiler's own diagnostic pointing at the wrong file:

```
/opt/homebrew/include/raylib.h:1155:21: note: 'ComputeSHA1' declared here
```

...while compiling a call to `ComputeSHA256`, which only exists in 6.0.

**Fix:** unlink the system package for the duration of the build.

```sh
brew unlink raylib      # macOS; on Linux, remove or unlink the distro package
bb clean && bb basic-window
brew link raylib        # restore it afterwards if you want it back
```

**Why it happens, and why the project can't fix it from its build config:**
jank's compiler has at least two C++ resolution pathways. The main one
correctly honours the project's own `-I` flags (which the `raylib-sys`
package emits as `jank-build::include-dir=` directives pointing at its
raylib 6.0 headers). A secondary pathway, used for certain overload-resolution and
diagnostic scenarios rather than for every call, does not inherit those
flags and falls back to clang's default system include search, which finds
the Homebrew header instead. jank exposes no flag or environment variable to
control that second pathway's search order.

This is why the failure looks so arbitrary: only examples that call a
function which is new in 6.0 or changed signature since 5.5 can trip it. On
one machine it hit 8 of 209: `basic-shapes`, `top-down-lights` and
`shapes-textures-shader` (all call `DrawCircleGradient`, whose signature
changed), `math-sine-cosine` (`DrawLineDashed`), `compute-hash`
(`ComputeSHA256`), `strings-management`, `font-sdf` (`LoadFontData`), and
`point-rendering` (`rlDisablePointMode`). The other 201 compiled fine with
the same stray header present, which makes this very easy to misdiagnose as
a bug in one example.

Verified as a closed loop on jank `0.1-alpha` against Homebrew raylib 5.5:
unlink → all 8 compile clean; re-link → all 8 fail again, identically.
