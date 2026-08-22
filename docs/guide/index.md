# b12n-raylib-jnk Guide

User-facing documentation for `b12n-raylib-jnk`: 209 [raylib](https://github.com/raysan5/raylib)
examples ported to **[jank](https://jank-lang.org)**, a native Clojure dialect
(C++/LLVM), not the JVM. Each page below covers one interop pattern or raylib
API surface, citing the example file that proves it.

## Why this exists

jank is young, and almost nothing has been written about using it against a
real C library at this scale. Porting 209 raylib examples surfaced a set of
interop rules that are not obvious from jank's own documentation and that
cost real debugging time to find. Each page here is one of those rules,
written up with the committed example that proves it, so the next person
does not have to rediscover it by bisecting a failing draw loop.

## What b12n-raylib-jnk is

209 of the official raylib examples (shapes, core, text, textures, shaders,
models, and audio), each a small jank namespace under
`raylib-examples/src/raylib_examples/`, each calling raylib's C API directly
through `(:include "raylib.h")`.

It is the **native-Clojure sibling** of [`b12n-raylib-jlt`](https://github.com/burinc/b12n-raylib-jlt)
(raylib in Jolt/Chez Scheme) and of an unreleased JVM-Clojure port over
`coffi`/Panama. All three bind the same C library
directly; what differs is the boundary each language draws between its own
values and C's:

> **jolt and JVM Clojure cross the FFI boundary at the *call*: an FFI call
> marshals values in and out, but a raylib struct can otherwise live
> anywhere a normal value lives. jank draws the boundary at the *value*
> itself: a native C++ value (`Color`, `Vector2`, `Model`, ...) can be
> constructed and used inline, but it cannot cross a jank *function*
> boundary: not returned, not passed as a parameter, not carried through
> `loop`/`recur`. Every pattern in this guide is a consequence of that one
> rule.**

Four things follow from it:

1. **The value can't leave the form that produced it**: construct inline,
   bind in an enclosing `let`, or hold frame-crossing mutable state as an
   opaque box in a jank atom.
   ([`native-value-lifetimes.md`](native-value-lifetimes.md))
2. **The compiler enforces it at compile time, strictly**: `if`/`cond`
   branch type-checking, numeric coercion between jank and native number
   types, and struct construction all have sharp, well-defined rules.
   ([`type-checking-and-coercion.md`](type-checking-and-coercion.md))
3. **A C-interop toolbox reaches everything the rule seems to block**:
   pointer interop (`cpp/&`, `cpp/aget`, `cpp/unsafe-cast`), out-params,
   native arrays, and shared jank helper namespaces. One genuine gap is
   left: a jank fn cannot become a C function pointer.
   ([`cpp-interop-toolbox.md`](cpp-interop-toolbox.md))
4. **The full raylib surface is reachable despite the rule**: fonts,
   models and animations, audio, 3D mode, rlgl, and (platform-permitting)
   compute shaders all work, proven example by example.
   ([`raylib-api-coverage.md`](raylib-api-coverage.md))

Nothing about jank's C++ interop is raylib-specific: `(:include "header.h")`
and `cpp/` reach any C/C++ library. This repo just happens to exercise it
against one real, struct-heavy graphics API across 209 examples.

## Capability pages

### The interop core (the reason this repo is interesting)

- [`native-value-lifetimes.md`](native-value-lifetimes.md): the one rule
  that explains most crashes, frame-crossing mutable state via `cpp/raw`
  statics (and the per-fn-static duplication gotcha), and create-once
  resources via outer-`let` capture.
- [`type-checking-and-coercion.md`](type-checking-and-coercion.md): `if`/`cond`
  branch type-checking (including the `and`/`or` gotcha), the numeric-traps
  table (`mod`/`quot`/`cpp/float`/`min`/`max`), and constructing native
  structs from jank data.
- [`cpp-interop-toolbox.md`](cpp-interop-toolbox.md): pointer interop
  (`cpp/&`, `cpp/aget`, `cpp/unsafe-cast`), `int *` out-params, native
  arrays, shared jank helper namespaces (`rlights`, `shaders`, `models`,
  `rendertex`), and the one construct that is genuinely blocked.
- [`numeric-performance.md`](numeric-performance.md): why a hot loop written
  the ordinary way is ~70x off the same loop through `cpp/` operators, what
  that costs per iteration, and the two loop traps that come with the fix.

### What's proven to work

- [`raylib-api-coverage.md`](raylib-api-coverage.md): fonts, models and
  animations, audio, 3D mode, rlgl + textures, and compute shaders (with
  the platform caveat; see the root README's Known limitations).
- [`jvm-surface-gaps.md`](jvm-surface-gaps.md): what replaces the missing
  JVM surface (`Math/*`, `format`, char literals), what's actually
  available (`clojure.core`/`clojure.string`, with caveats), and a few
  gotchas that save a recompile.

### Orientation

- [`getting-started.md`](getting-started.md): requirements, cloning with
  the submodule, and the `bb` task surface.
- [`porting-workflow.md`](porting-workflow.md): the end-to-end process for
  porting one example: source of truth, file layout, the five-place
  registration, the headless smoke test.
- [`example-catalog.md`](example-catalog.md): a tour of all 209 examples
  grouped by raylib category, and how to add one (now with a preview GIF
  per recorded example; see [`docs/demos/README.md`](../demos/README.md)
  for the full gallery).
- [`raygui-to-keyboard.md`](raygui-to-keyboard.md): the pattern for
  porting raygui-based examples (sliders/checkboxes) to keyboard controls.

## See also

- [`b12n-raylib-jlt`](https://github.com/burinc/b12n-raylib-jlt): the same
  idea in Jolt (Chez Scheme) over `jolt.ffi`. Its FFI boundary is per-*call*,
  not per-*value*: a `Camera3D` can live in an ordinary variable between FFI
  calls, unlike jank's native values.
- An unreleased JVM-Clojure port over `coffi`/Panama takes the same per-call
  boundary as Jolt, plus a garbage collector jank doesn't have to work around.
