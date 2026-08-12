# b12n-raylib-jnk — Guide

User-facing documentation for `b12n-raylib-jnk`: 209 [raylib](https://github.com/raysan5/raylib)
examples ported to **[jank](https://jank-lang.org)** — a native Clojure dialect
(C++/LLVM), not the JVM. Each page below covers one interop pattern or raylib
API surface, citing the example file that proves it, and cross-references
sibling projects in the [b12n umbrella wiki](https://github.com/burinc/b12n-wikis).

## Why this exists

When `b12n-raylib-jnk` is mirrored into
[`b12n-wikis`](https://github.com/burinc/b12n-wikis), each page below becomes an
entry under `b12n-wikis/b12n-raylib-jnk/`, and the wiki's cross-project index
can cite them for any jank/native-interop-distinctive pattern.

## What b12n-raylib-jnk is

209 of the official raylib examples — shapes, core, text, textures, shaders,
models, and audio — each a small jank namespace under
`raylib-examples/src/raylib_examples/`, sharing one C-binding wrapper,
`jank-raylib-sys`, that exposes raylib's C API directly.

It is the **native-Clojure sibling** of [`b12n-rljlt`](https://github.com/burinc/b12n-rljlt)
(raylib in Jolt/Chez Scheme) and [`b12n-raylib-clj`](https://github.com/burinc/b12n-raylib-clj)
(raylib in JVM Clojure over `coffi`/Panama). All three bind the same C library
directly; what differs is the boundary each language draws between its own
values and C's:

> **jolt and JVM Clojure cross the FFI boundary at the *call*: an FFI call
> marshals values in and out, but a raylib struct can otherwise live
> anywhere a normal value lives. jank draws the boundary at the *value*
> itself: a native C++ value (`Color`, `Vector2`, `Model`, ...) can be
> constructed and used inline, but it cannot cross a jank *function*
> boundary — not returned, not passed as a parameter, not carried through
> `loop`/`recur`. Every pattern in this guide is a consequence of that one
> rule.**

Four things follow from it:

1. **The value can't leave the form that produced it** — construct inline,
   bind in an enclosing `let`, or park frame-crossing mutable state in a
   `cpp/raw` static. ([`native-value-lifetimes.md`](native-value-lifetimes.md))
2. **The compiler enforces it at compile time, strictly** — `if`/`cond`
   branch type-checking, numeric coercion between jank and native number
   types, and struct construction all have sharp, well-defined rules.
   ([`type-checking-and-coercion.md`](type-checking-and-coercion.md))
3. **A C-interop toolbox reaches everything the rule seems to block** —
   pointer interop (`cpp/&`, `cpp/aget`, `cpp/new`), out-params, callbacks
   defined inside `cpp/raw`, and shared C headers shipped by a wrapper.
   ([`cpp-interop-toolbox.md`](cpp-interop-toolbox.md))
4. **The full raylib surface is reachable despite the rule** — fonts,
   models and animations, audio, 3D mode, rlgl, and (platform-permitting)
   compute shaders all work, proven example by example.
   ([`raylib-api-coverage.md`](raylib-api-coverage.md))

Nothing about jank's C++ interop is raylib-specific — `(:include "header.h")`
and `cpp/` reach any C/C++ library. This repo just happens to exercise it
against one real, struct-heavy graphics API across 209 examples.

## Capability pages

### The interop core (the reason this repo is interesting)

- [`native-value-lifetimes.md`](native-value-lifetimes.md) — the one rule
  that explains most crashes, frame-crossing mutable state via `cpp/raw`
  statics (and the per-fn-static duplication gotcha), and create-once
  resources via outer-`let` capture.
- [`type-checking-and-coercion.md`](type-checking-and-coercion.md) — `if`/`cond`
  branch type-checking (including the `and`/`or` gotcha), the numeric-traps
  table (`mod`/`quot`/`cpp/float`/`min`/`max`), and constructing native
  structs from jank data.
- [`cpp-interop-toolbox.md`](cpp-interop-toolbox.md) — pointer interop
  (`cpp/&`, `cpp/aget`, `cpp/new`, `cpp/raw`), `int *` out-params,
  callback-taking APIs, shared C headers shipped by a wrapper
  (`jank_rlights.h`), and shader-uniform shims.

### What's proven to work

- [`raylib-api-coverage.md`](raylib-api-coverage.md) — fonts, models and
  animations, audio, 3D mode, rlgl + textures, and compute shaders (with
  the platform caveat — see the root README's Known limitations).
- [`jvm-surface-gaps.md`](jvm-surface-gaps.md) — what replaces the missing
  JVM surface (`Math/*`, `format`, char literals), what's actually
  available (`clojure.core`/`clojure.string`, with caveats), and a few
  gotchas that save a recompile.

### Orientation

- [`getting-started.md`](getting-started.md) — requirements, cloning with
  the submodule, and the `bb` task surface.
- [`porting-workflow.md`](porting-workflow.md) — the end-to-end process for
  porting one example: source of truth, file layout, the five-place
  registration, the headless smoke test.
- [`example-catalog.md`](example-catalog.md) — a tour of all 209 examples
  grouped by raylib category, and how to add one.
- [`raygui-to-keyboard.md`](raygui-to-keyboard.md) — the pattern for
  porting raygui-based examples (sliders/checkboxes) to keyboard controls.

## See also

- [`b12n-rljlt`](https://github.com/burinc/b12n-rljlt) — the same idea in
  Jolt (Chez Scheme) over `jolt.ffi`. Its FFI boundary is per-*call*, not
  per-*value* — a `Camera3D` can live in an ordinary variable between FFI
  calls, unlike jank's native values.
- [`b12n-raylib-clj`](https://github.com/burinc/b12n-raylib-clj) — the same
  idea in JVM Clojure over `coffi`/Panama. Same per-call boundary as Jolt,
  plus a JVM garbage collector jank doesn't have to work around.
