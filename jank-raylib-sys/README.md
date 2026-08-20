# jank-raylib-sys

The `-sys` wrapper that exposes raylib to jank. It builds the vendored
`raylib` submodule with CMake and emits the `jank-build::include-dir=`,
`link-dir=` and `link-library=` directives that consumer projects compile
against. It also ships `include/jank_rlights.h`, a jank-shaped adaptation of
raylib's `rlights.h`.

Installed into `~/.m2` by `bb install` from the repo root. See the root
`README.md` for the build flow and `patches/README.md` for the one patch
applied to the raylib checkout.

## Licensing in this directory is mixed — read before copying

The repository as a whole is under the **zlib License** (root `LICENSE`), but
two files here are **MPL 2.0** and cannot be relicensed by this project,
because MPL 2.0 is a file-level copyleft and they are derived from Kyle
Cesare's [lein-jank-playground](https://github.com/kylc/lein-jank-playground):

| File | License |
|---|---|
| `project.clj` | MPL 2.0 — derived from lein-jank-playground |
| `jank-build.bb` | MPL 2.0 — derived from lein-jank-playground |
| `include/jank_rlights.h` | zlib — adapted from raylib's `rlights.h` |
| `patches/**` | zlib |

The `LICENSE` file in this directory is the MPL text that the two headers
point at, as MPL 2.0 section 3.4 requires. **It does not cover the whole
directory.** The root `NOTICE` has the full attribution picture.
