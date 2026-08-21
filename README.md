# b12n-raylib-jnk

[![examples ported](https://img.shields.io/badge/examples_ported-209%2F217-brightgreen)](raylib-examples/README.md#porting-progress)
[![categories complete](https://img.shields.io/badge/complete-shapes,_shaders,_audio,_text-brightgreen)](raylib-examples/README.md#porting-progress)

209 official [raylib](https://www.raylib.com/examples.html) examples ported to
**[jank](https://jank-lang.org)**, a native Clojure dialect that compiles to
native code via C++/LLVM, not the JVM. Each example is one `.jank` namespace
under `raylib-examples/src/raylib_examples/`; a Leiningen profile picks which
`-main` runs.

raylib comes from the official
[`org.jank-lang.commons/raylib-sys`](https://github.com/jank-lang/commons)
package, so there is no wrapper to build or install here. A `raylib`
submodule is still fetched, but only for the example assets: the shaders,
models, textures and audio that many of the examples load.

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
bb starfield         # run one (fetches example assets on first use)
bb run particles     # same, by argument
bb run-all           # cycle through every example, ~15s each (a demo reel)
bb run-all 40        # ...longer per example (also covers first-run compiles)

bb check             # offline gates: syntax, registration, EDN
bb nrepl             # a jank nREPL with cpp/ interop live in it
bb clean             # remove */target build dirs
```

If the `lein` on your `PATH` can't bootstrap, set `LEIN=/path/to/lein`.

### macOS

There is no `bwrap` on macOS, so Leiningen's build sandbox cannot work there
at all. Every `bb`/`lein` invocation here already passes `--disable-sandbox`
for you. See [`docs/guide/getting-started.md`](docs/guide/getting-started.md).

## Manual usage

```sh
cd raylib-examples && lein with-profile +<example> run --disable-sandbox
```

## The examples

**See [`raylib-examples/README.md`](raylib-examples/README.md) for the full
catalog**, listing every ported example with its official C source and what it
shows, grouped by raylib category (shapes, core, text, textures, shaders,
models, audio). `bb info` gives you the same breakdown from the terminal.
See [`docs/demos/README.md`](docs/demos/README.md) for an animated preview
of every recorded example, recorded via `bb record` (configured by
`scripts/demo_manifest.edn`). `bb record --only
<example-name>` matches an exact id or an id prefix, e.g. `--only
camera-2d` also selects `camera-2d-platformer`, `camera-2d-split-screen`,
and `camera-2d-mouse-zoom`, so double-check the plan
(`bb record --dry-run`) before combining a short `--only` prefix with
`--force`.

`bb record` drives a `screen-grab` capture CLI that is not publicly
released, so recording is maintainer-only. The task will tell you so rather
than failing obscurely. You never need to run it: every GIF under
`docs/demos/` is committed.

## Known limitations

- **`rlgl-compute` does not run out of the box.** It needs OpenGL 4.3
  compute-shader support (`rlLoadShaderProgramCompute`, SSBOs,
  `rlComputeShaderDispatch`). raylib is built by the official `raylib-sys`
  package at OpenGL 3.3, which is what raylib's own Desktop default
  resolves to, and 3.3 is the practical ceiling on macOS anyway (Apple
  caps out at OpenGL 4.1; GLFW rejects a 4.3 context request
  unconditionally there, regardless of any compatibility hint). Running it
  on a platform where 4.3 is real would mean building raylib yourself with
  `OPENGL_VERSION "4.3"` instead of taking the published package.

## Documentation

Full guide at [`docs/guide/index.md`](docs/guide/index.md): the
native-value-lifetime rule that shapes every jank/C++ interop pattern here,
a C-interop toolbox, raylib API coverage notes, the porting workflow, and
the full example catalog.

Rebuilding and republishing that guide as a site is a maintainer task:
`bb docs-sync` (`--no-push` to build and commit without publishing
anything). It needs the sibling `b12n-docs` checkout and AWS credentials.

## Credits

The `jank-raylib-sys` wrapper and the overall `lein-jank`-based project
layout originate from Kyle Cesare's
[`kylc/lein-jank-playground`](https://github.com/kylc/lein-jank-playground).
This repo carries the `raylib-examples` collection and the `jank-raylib-sys`
wrapper forward as a standalone, self-contained project. [raylib](https://www.raylib.com)
itself is by Ramon Santamaria ([@raysan5](https://github.com/raysan5)) and
contributors, vendored here as a git submodule under its own zlib license.

## Changelog

[`CHANGELOG.md`](CHANGELOG.md) records what has changed since the repo went
public.

## Contributing

New examples are welcome. The suite is deliberately mechanical to grow, and
`raylib-examples/README.md` keeps a queue of what is not ported yet. See
[`CONTRIBUTING.md`](CONTRIBUTING.md) for setup, the headless smoke test that
stands in for CI here, and the four places a new example has to be registered.

## License

[zlib](LICENSE), the same license as raylib itself, so the original terms
carry through the ports rather than being replaced by something stricter.

Every file is under that license. [`NOTICE`](NOTICE) records every
attribution and what was altered in each.
