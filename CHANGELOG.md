# Changelog

Notable changes to this project. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

This file starts at the point the repo was opened to the public; the 209
example ports that preceded it are in the git history and in
[`raylib-examples/README.md`](raylib-examples/README.md).

## [Unreleased]

### Added

- `raylib-examples.shaders`, shader-uniform helpers in pure jank
  (`set-int!`, `set-float!`, `set-vec2!`/`3!`/`4!`, `set-shader-loc!`).
- `raylib-examples.models`, Model material binding in pure jank
  (`set-texture!`, `set-map-texture!`, `set-shader!`, `set-shader-all!`,
  `set-map-color!`).
- `raylib-examples.rlights`: raylib's `rlights.h` reimplemented as a jank
  namespace, replacing the C header the `-sys` wrapper used to ship.
- `opaque-boxes` example, demonstrating a native `Color` returned from a fn,
  held in an immutable vector, and captured in a closure.
- `bb check` runs the offline gates: reader syntax across every `.jank` source, the
  four registration touchpoints per example, orphaned sources, and the EDN
  data files. Runs in well under a second.
- `bb nrepl` starts a jank nREPL for the examples project, with `cpp/`
  interop live in it.
- `bb lint` / `bb lint:strict` run clj-kondo over every `.jank` source, with a
  `.clj-kondo/config.edn` that teaches it about jank. Without the config the
  suite reports 644 warnings, of which 2 are genuine.
- GitHub Actions CI running `bb check` and `bb lint:strict` on pull requests.
- `CONTRIBUTING.md`, `NOTICE`, `.mailmap`, and issue/PR templates.

### Changed

- **raylib now comes from the official
  [`org.jank-lang.commons/raylib-sys`](https://github.com/jank-lang/commons)
  package** instead of a vendored `-sys` wrapper built from a submodule. It
  pins the same raylib commit (tag 6.0). There is no build or install step
  for it any more.
- **Licence is now zlib**, matching raylib itself, so the terms of the
  original examples carry through the ports. Previously MPL 2.0.
- Demo GIFs re-encoded: **104 MB to 62 MB** with no change to frame counts or
  timing.
- Documentation site homepage rewritten, and the guide corrected throughout
  (see *Fixed*).

### Removed

- The vendored `jank-raylib-sys` wrapper, including its `project.clj`,
  `jank-build.bb`, `jank_rlights.h`, and the macOS OpenGL forward-compat
  patch. The `raylib` submodule remains, but only as the source of example
  assets (shaders, models, textures, audio) that the published jar does not
  carry.
- `bb install`. Getting started is now clone, then run an example.
- The last MPL 2.0 files, with the wrapper. The tree is uniformly zlib.

### Fixed

- **The guide's central claim about native values was wrong.** It said a
  native `cpp` value "only stays native within the form that produced it" and
  that a jank fn cannot return one. Two mechanisms were missing: trait
  conversion (integral types, bools, `std::string` cross freely) and **opaque
  boxes** (`cpp/new` + `cpp/box` + `cpp/unbox`), which carry any native value
  through the runtime. Raised by [@jeaye](https://github.com/jeaye).
- **The guide said jank has no assignment form.** It has `cpp/=`, which takes
  an lvalue and writes both struct fields and array elements.
  `clojure.core/aset` is sugar for the latter. Note that `cpp/aset` does *not*
  exist while `cpp/aget` does. Guessing the symmetric partner is what made
  this look like a missing language feature.
- **The guide said jank has no REPL.** It has both a REPL and nREPL support.
- The example-registration recipe said a new example touches *five* places,
  including a row in a repo-root table that does not exist. It is four.
- `bb docs-sync` could never push a commit it had not made in the same run:
  a clean working tree short-circuited to `:skipped` before the push step,
  leaving branches ahead of origin indefinitely. It also now reports a
  missing upstream instead of silently skipping.
- Homebrew's raylib silently shadowing the vendored headers is documented in
  the troubleshooting guide, with the closed-loop test that confirms it.
- Machine-local `~/dev/` paths and links to private repositories removed from
  the published tree.
- `decals` required `clojure.string` without using it; `undo_redo` had a
  redundant `do` inside a `when`. Both found by the new lint gate.
