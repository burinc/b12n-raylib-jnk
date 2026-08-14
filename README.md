# b12n-raylib-jnk

[![examples ported](https://img.shields.io/badge/examples_ported-209%2F217-brightgreen)](raylib-examples/README.md#porting-progress)
[![categories complete](https://img.shields.io/badge/complete-shapes,_shaders,_audio,_text-brightgreen)](raylib-examples/README.md#porting-progress)

209 official [raylib](https://www.raylib.com/examples.html) examples ported to
**[jank](https://jank-lang.org)** — a native Clojure dialect that compiles to
native code via C++/LLVM, not the JVM. Each example is one `.jank` namespace
under `raylib-examples/src/raylib_examples/`; a Leiningen profile picks which
`-main` runs.

This repo is standalone: it vendors `jank-raylib-sys` (the raylib C-binding
wrapper, with `raylib` itself as a git submodule) so it builds from a fresh
clone with nothing else needed beyond the requirements below.

## Requirements

- Recent install of the [`jank`](https://jank-lang.org) compiler and the
  `lein-jank` Leiningen plugin (`2026.06-1` or newer)
- A C++ compiler
- CMake
- [Babashka](https://babashka.org)

Verified on macOS with jank `0.1-alpha` and `lein-jank 2026.06-1`.

Clone with the submodule:

```sh
git clone --recurse-submodules git@github.com:burinc/b12n-raylib-jnk.git
# or, after a plain clone:
git submodule update --init --recursive
```

## Quick start

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

### macOS

There is no `bwrap` on macOS, so the native build must run with sandboxing
disabled. Every `bb`/`lein` invocation here already passes
`--disable-sandbox` for you — see `AGENTS.md` for why that's safe.

## Manual usage

```sh
cd jank-raylib-sys && lein update-in :prep-tasks empty -- install
cd raylib-examples  && lein with-profile +<example> run --disable-sandbox
```

## The examples

**See [`raylib-examples/README.md`](raylib-examples/README.md) for the full
catalog** — every ported example with its official C source and what it
shows, grouped by raylib category (shapes, core, text, textures, shaders,
models, audio). `bb info` gives you the same breakdown from the terminal.
See [`docs/demos/README.md`](docs/demos/README.md) for an animated preview
of every recorded example, recorded via `bb record` (shelled out to the
[`screen-grab`](https://github.com/burinc/b12n-screen-grab) CLI and
configured by `scripts/demo_manifest.edn`). `bb record --only
<example-name>` matches an exact id or an id prefix — e.g. `--only
camera-2d` also selects `camera-2d-platformer`, `camera-2d-split-screen`,
and `camera-2d-mouse-zoom` — so double-check the plan
(`bb record --dry-run`) before combining a short `--only` prefix with
`--force`.

`bb record` requires the `screen-grab` CLI on your PATH. Install it from
[b12n-screen-grab](https://github.com/burinc/b12n-screen-grab):
`cd ~/dev/b12n-screen-grab && bb install:home`

## Known limitations

- **`rlgl-compute` does not run out of the box.** It needs OpenGL 4.3
  compute-shader support (`rlLoadShaderProgramCompute`, SSBOs,
  `rlComputeShaderDispatch`). This repo builds `jank-raylib-sys` at
  `OPENGL_VERSION "3.3"` by default — the version every other example
  needs, and the version macOS's native GL backend actually supports
  (Apple caps out at OpenGL 4.1; GLFW rejects a 4.3 context request
  unconditionally on macOS, regardless of any compatibility hint). To try
  `rlgl-compute` on a platform where OpenGL 4.3 is real (Linux, Windows),
  change `OPENGL_VERSION` to `"4.3"` in `jank-raylib-sys/jank-build.bb`, then
  `bb install` (consumers resolve `jank-build.bb` from the `~/.m2` jar, not
  the working tree — `bb clean` alone won't pick up the edit), then
  `bb clean` (or delete `raylib-examples/target`) and rebuild. This will not
  work on macOS regardless.

## Documentation

Full guide at [`docs/guide/index.md`](docs/guide/index.md): the
native-value-lifetime rule that shapes every jank/C++ interop pattern here,
a C-interop toolbox, raylib API coverage notes, the porting workflow, and
the full example catalog. `AGENTS.md` carries the terse, in-context version
of the same rules for AI coding agents.

## Credits

The `jank-raylib-sys` wrapper and the overall `lein-jank`-based project
layout originate from Kyle Cesare's
[`kylc/lein-jank-playground`](https://github.com/kylc/lein-jank-playground).
This repo carries the `raylib-examples` collection and the `jank-raylib-sys`
wrapper forward as a standalone, self-contained project. [raylib](https://www.raylib.com)
itself is by Ramon Santamaria ([@raysan5](https://github.com/raysan5)) and
contributors, vendored here as a git submodule under its own zlib license.

## License

[MPL 2.0](LICENSE)
