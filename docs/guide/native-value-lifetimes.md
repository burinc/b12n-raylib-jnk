# Native value lifetimes

> **The semantics live in the jank book**, not here:
> [Working with native values](https://book.jank-lang.org/cpp-interop/native-values.html)
> and [Casting between native types](https://book.jank-lang.org/cpp-interop/cast.html).
> This page records only what 212 ported examples taught us on top of it,
> and names the committed example that proves each one.

## The rule, in one line

A `Color`, `Vector2`, `Rectangle` or `Camera2D` has no conversion trait, so
it cannot implicitly cross a jank fn boundary. Trait-convertible types
(integrals, bools, C strings, `std::string`) cross freely.

Works without ceremony:

```clojure
(cpp/DrawCircleV (cpp/Vector2 x y) r c)   ; constructed inline as an argument
(let [font (cpp/GetFontDefault)] ...)     ; let-local, used in scope
```

Fails:

```clojure
(defn pick-color [i] (if (zero? i) cpp/RED cpp/BLUE))
;; returning a native object of type 'Color', which is not convertible
;; to a jank runtime object
```

Proof: `dashed_line.jank` (return), `digital_clock.jank` and
`tiled_drawing.jank` (parameter), `input_mouse.jank` (`loop`/`recur` state).

## Getting a native value out anyway

`cpp/new` + `cpp/box` + `cpp/unbox`, exactly as the book describes. The one
thing to remember: the box outlives the call, so the value must be
heap-allocated. A `let`-local would be destroyed at end of scope.

```clojure
(defn make-color [r g b]
  (cpp/box (cpp/new cpp/Color (cpp/cast cpp/uint8_t r)
                              (cpp/cast cpp/uint8_t g)
                              (cpp/cast cpp/uint8_t b)
                              (cpp/cast cpp/uint8_t 255))))
```

`opaque_boxes.jank` (`bb opaque-boxes`) returns a `Color` from a fn, keeps
several in an immutable vector, and captures one in a closure.

## Writing to a native value

`cpp/=` takes an lvalue:

```clojure
(cpp/= (.-fovy c) (cpp/float 45.0))
(cpp/= (cpp/aget (.-locs sh) (cpp/int i)) (cpp/int 7))
```

**`cpp/aset` does not exist**, though `cpp/aget` does. `clojure.core/aset`
is the sugar, expanding to the second form. This repo used C shims for
assignment far longer than it needed to for exactly that reason.
Worked example: `models.jank`.

## The four faces of one boundary rule

Same rule each time, but the next one never looks like the last:

1. **A native argument arrives as an `object_ref`.** Pass a `Shader` to a
   jank fn and the native call inside rejects it. Box it.
2. **Boxing cannot be wrapped in a fn.** `(defn box-it [s] (cpp/box (cpp/new cpp/Shader s)))`
   fails identically: by the time the body runs `s` is already an
   `object_ref`. Box at the `let` that produced the value.
3. **A native value cannot be returned**, so you cannot factor out an
   accessor: `(defn- material [m i] (cpp/aget (.-materials m) i))` returns a
   `Material &`. Inline the lookup.
4. **`cpp/=` yields the assigned lvalue.** A fn whose last form is an
   assignment tries to return a native reference. End it in an explicit `nil`.
5. **A closure counts as a fn boundary.** `dotimes` and `doseq` build one, so
   a native pointer captured by either arrives as an `object_ref` and
   `cpp/aget` has no overload for it. `loop`/`recur` is inline and works.
6. **Pointers are values too.** A `float *`, `const char *` or `Model *`
   passed as a jank fn parameter arrives as an `object_ref`, and `cpp/aget`
   and `cpp/+` have no overload for that. Box the pointer, or keep the whole
   loop in one fn. `spectrum_visualizer.jank` boxes its FFT arrays,
   `text_3d_drawing.jank` its `const char *`, `decals.jank` its mesh arrays.

Corollary: a boxed copy **shares the pointer members** of the original.
`Shader` is `{unsigned int id; int *locs;}`, `Model` holds
`Material *materials`, so writing through the box reaches the caller's value.
That is what makes one shared helper namespace viable instead of a shim per
consumer (`shaders.jank`, `models.jank`, `rlights.jank`).

## Two limits that are not about lifetimes

Neither follows from the boundary rule, but both surface while working around
it, and both are compile-time errors with clear messages:

- **A jank fn takes at most 10 parameters.** `text_3d_drawing.jank`'s glyph
  writer needed 11, so its position travels as an `[x y z]` vector.
- **`loop`/`recur` slots are assigned in order, without temporaries.** An
  argument that reads a slot written earlier in the same `recur` sees the
  **new** value. Bind the new values first, or force a copy. See
  [`numeric-performance.md`](numeric-performance.md#two-loop-traps).

## Frame-crossing mutable native state

When a native resource must persist across frames AND be recreated at runtime
with computed sizes, neither usual home works: `loop`/`recur` cannot carry a
native value, and a create-once outer-`let` local cannot be rebound.

**Hold an opaque box in a jank atom.** The box is an ordinary jank object, so
the atom carries it freely; `cpp/new` keeps the value alive past the scope
that made it.

```clojure
(defn- resize-target! [box-atom w h]
  (when-let [old @box-atom]
    (cpp/UnloadRenderTexture (cpp/* (cpp/unbox (:* cpp/RenderTexture2D) old))))
  (reset! box-atom
          (cpp/box (cpp/new cpp/RenderTexture2D (cpp/LoadRenderTexture (cpp/int w) (cpp/int h)))))
  nil)

;; per frame
(let [target (cpp/* (cpp/unbox (:* cpp/RenderTexture2D) @target-box))]
  (cpp/BeginTextureMode target) ...)
```

`viewport_scaling.jank` is the proof, and recreation specifically was measured
rather than assumed: with the resize forced on a timer, a 25s run logged **16
framebuffer create/unload cycles and zero errors**. Creation, per-frame reads
and mid-run reallocation all work.

### The `cpp/raw` static this replaces

Earlier ports parked such values in a `cpp/raw` static behind accessor fns.
Every one of those has since been replaced by a box; only the four
callback-blocked examples still hold C state at all. Prefer the box. The
static carries a trap that cost real debugging time.

**`cpp/raw` statics are duplicated PER JANK FN.** Every jank fn referencing
the shims gets its OWN copy. A helper fn writing the "same" static writes a
private copy the others never see. Probe evidence (2026-07-04): after `-main`
called a load-into-static shim, `-main` read `.glyphCount` 95 from its copy
while a helper `defn-` read 0 from its own. Failure modes are nasty: state
silently "resets" across fn boundaries, and reading through a zeroed struct's
pointer field segfaults.

If you must use one, route EVERY read/write through ONE jank fn (in practice
`-main`), inlining helper logic there. Pure-jank helpers (no shim calls) stay
safe to factor out.

## Create-once native resources

Bind outside the frame loop, use inside it, unload after. Lexical capture
keeps them native.

```clojure
(let [canvas (cpp/LoadRenderTexture WIDTH HEIGHT)]
  (loop [...] ... (cpp/BeginTextureMode canvas) ...)
  (cpp/UnloadRenderTexture canvas))
```

Two at once work (`camera_2d_split_screen.jank`). Blit a RenderTexture with a
**negative source height**: they are stored upside down (`lines_drawing.jank`,
`window_letterbox.jank`).
