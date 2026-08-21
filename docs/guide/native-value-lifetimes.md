# Native value lifetimes

jank is **native** Clojure (C++/LLVM), with no JVM and no Java interop. The
compiler statically type-checks the boundary between jank objects and native
C++ values, and this page is about what that boundary actually enforces.
Each lesson names the committed example that proves it; those files are the
running test suite for this document.

## The rule that explains most crashes

The boundary is about **convertibility**, not about scope. C++ has no common
base class for all types, so jank cannot type-erase an arbitrary C++ value
into a runtime object the way the JVM can. What follows from that:

- **Trait-convertible types cross freely.** C++ intrinsic integral types,
  bools, C strings and some standard-library types such as `std::string`
  have conversion traits, so jank converts them to and from runtime objects
  automatically. Passing and returning those costs a conversion, not an
  error.
- **Everything else cannot cross *implicitly*.** A `Color`, `Vector2`,
  `Rectangle` or `Camera2D` has no such trait, so returning one from a jank
  fn is a compile error: `returning a native object of type 'Color', which
  is not convertible to a jank runtime object`.

That second case is the one this repo lives in, and the rest of this page is
about it. **It is a restriction on implicit conversion, not a life sentence
for the value**. See [Getting a native value out anyway](#getting-a-native-value-out-anyway)
below, which is the supported way through.

### Where a non-convertible value works without any ceremony

A `Color`, `Vector2`, `Rectangle`, `Camera2D`, ... may be:

- constructed inline as a call argument: `(cpp/DrawCircleV (cpp/Vector2 ...) ...)` ✅
- bound to a `let`-local and used in the same scope ✅
- bound in a `let` OUTSIDE the frame loop and used inside it (lexical
  capture) ✅. This is how create-once GPU resources live
  (`lines_drawing.jank`'s RenderTexture, `words_alignment.jank`'s Font)

### Where it fails, if you do nothing about it

- returned from a fn ❌: `returning a native object of type 'Color', which
  is not convertible to a jank runtime object`. Even via nested `if`
  (`dashed_line.jank` learned this).
- passed as a fn parameter and then used in a native call ❌: it boxes to an
  `object_ref` and the native call rejects it (`digital_clock.jank`'s
  draw-hand; `tiled_drawing.jank` hit this trying to pass a `Texture2D` to a
  `draw-tiled` helper, `No matching call to 'DrawTexturePro' ... argument 0
  having type 'jank::runtime::object_ref &'`, and had to inline the helper so
  the texture stayed a captured `let`-local).
- carried through `loop`/`recur` state ❌ (`input_mouse.jank`).

**The pattern this repo uses:** thread plain jank data (ints, reals,
keywords, maps) and resolve the native value inline at the use site.
`camera_2d_platformer.jank` threads the camera as five scalars and rebuilds
`(cpp/Camera2D ...)` each frame; `input_mouse.jank` threads a `color-id` int
and picks the `Color` with a nested `if` at draw time.

That is a **performance choice, not the only option**. These are per-frame
draw calls at 60 FPS, where the alternative costs a heap allocation per
value. When the value is a long-lived resource rather than a per-frame
scalar, box it instead.

## Getting a native value out anyway

jank has opaque boxes for exactly this. `cpp/box` puts a raw pointer into an
ordinary jank object that can then travel anywhere in the runtime;
`cpp/unbox` takes it back out, and jank checks the type you ask for against
the one that went in. Because the box can outlive the call, the value it
points at must be heap-allocated with `cpp/new`; a `let`-local would be
destroyed at the end of its scope, leaving the box dangling.

```clojure
(defn make-color [r g b]
  (cpp/box (cpp/new cpp/Color (cpp/cast cpp/uint8_t r)
                              (cpp/cast cpp/uint8_t g)
                              (cpp/cast cpp/uint8_t b)
                              (cpp/cast cpp/uint8_t 255))))

(defn swatch-text [box]
  (let [c (cpp/unbox (:* cpp/Color) box)]
    (str "rgb(" (.-r c) "," (.-g c) "," (.-b c) ")")))
```

`opaque_boxes.jank` is the proof, and it demonstrates the three things the
list above says are impossible for a bare `Color`: a jank fn **returns** one,
several sit in a plain immutable jank vector built with `mapv`, and one is
**captured in a closure**. Run it with `bb opaque-boxes`.

Unboxing as the wrong type is a runtime error that names both types rather
than corrupting memory:

```
error: This opaque box holds a 'Color*', but it was unboxed as a 'Vector2*'.
```

The other way through is to teach jank the type: implementing
`jank::runtime::convert<T>` for it makes the type trait-convertible, after
which it crosses implicitly like `std::string` does. That is C++ template
work and only worth it for a type you cross the boundary with constantly.
See the jank book's
[Working with native values](https://book.jank-lang.org/cpp-interop/native-values.html)
for both mechanisms.

## Writing to a native value

Reading a member gives you a reference (`(.-fovy c)`); **`cpp/=` writes one**.
It takes an lvalue, so both shapes work:

```clojure
(cpp/= (.-fovy c) (cpp/float 45.0))                     ; field, through a pointer
(cpp/= (cpp/aget (.-locs sh) (cpp/int i)) (cpp/int 7))  ; array element
```

`clojure.core/aset` is sugar for the second; it expands to exactly that.
**`cpp/aset` does not exist**, which is a trap, because `cpp/aget` does and
the pair looks symmetric. This project used C shims for assignment far longer
than it needed to for precisely that reason.

`raylib-examples/src/raylib_examples/models.jank` is the worked example:
binding a texture or shader into a `Model` material, the most repeated bit
of C in this suite, is now ordinary jank.

## The four faces of one boundary rule

These are all the same rule (a native value has no runtime representation),
and each one cost a compile to learn. Worth reading together, because the
next one never looks like the last:

1. **A native argument arrives as an `object_ref`.** Pass a `Shader` to a
   jank fn and the native call inside rejects it. Box it.
2. **Boxing cannot be wrapped in a fn.** A `(defn box-it [s] (cpp/box (cpp/new cpp/Shader s)))`
   fails identically: by the time the body runs, `s` is already an
   `object_ref` and `cpp/new` has nothing to copy from. **Box where the value
   is still native**, at the `let` that produced it.
3. **A native value cannot be returned.** So you cannot factor out an
   accessor: `(defn- material [m i] (cpp/aget (.-materials m) i))` would
   return a `Material &`. Inline the lookup.
4. **`cpp/=` yields the assigned lvalue**, which is a native reference. A fn
   whose last form is an assignment tries to return it and fails. End such
   fns in an explicit `nil`.

A useful corollary: a boxed copy **shares the pointer members** of the
original. `Shader` is `{unsigned int id; int *locs;}` and `Model` holds
`Material *materials`, so writing through the box reaches the caller's value.
That is what makes one shared helper namespace viable instead of a shim per
consumer.

## Frame-crossing mutable native state: park it in a cpp/raw static

> **This section predates opaque boxes and may be obsolete.** A box held in
> an atom should be able to carry recreated native state across frames, which
> is what the static exists for. That has **not** been probed; three
> examples still use the static pattern (`viewport_scaling`, `game_of_life`,
> `hot_reloading`). Treat what follows as the pattern that is known to work,
> not as the only one.

When a native resource must BOTH persist across frames AND be recreated at
runtime with computed sizes, neither of the two usual homes works: `loop`/
`recur` state can't carry a native value (the same rule as above, that a native
value can't cross a fn boundary, applied to loop/recur state), and a
create-once outer-`let` local can't be rebound. Park the value in a `cpp/raw` static
with tiny accessor fns instead:

```clojure
(cpp/raw "static RenderTexture2D jank_target = { 0 };
static void jank_resize_target(int w, int h) {
  UnloadRenderTexture(jank_target);
  jank_target = LoadRenderTexture(w, h);
}
static RenderTexture2D jank_get_target(void) { return jank_target; }")
```

jank calls `(cpp/jank_resize_target w h)` on change events and re-fetches
`(let [target (cpp/jank_get_target)] ...)` each frame -- the struct comes
back by value into a let-local and never crosses a jank fn boundary.
Proof: `viewport_scaling.jank`, whose RenderTexture is recreated on every
window resize / resolution / viewport-mode change with sizes computed from
the current window state. (`UnloadRenderTexture` guards id 0 internally,
so the first call against the zero-initialized static is safe.) Reach for
this only when recreation is genuinely dynamic; a fixed-size resource
should stay a create-once outer-let local (`lines_drawing.jank`).

**CRITICAL: cpp/raw statics are duplicated PER JANK FN.** Every jank fn
that references the shims gets its OWN copy of the raw block's statics --
a helper fn that writes the "same" static writes a private copy the other
fns never see. Probe evidence (2026-07-04): after `-main` called a
load-into-static shim, `-main` read `.glyphCount` 95 from its copy while a
helper `defn-` read 0 from its own; the helper's writes were likewise
invisible to `-main`. Failure modes are nasty: state silently "resets"
across fn boundaries, and reading through a zeroed struct's pointer field
segfaults. Rule: route EVERY read/write of a mutable raw static through
one single jank fn (in practice `-main`), inlining helper logic there --
`unicode_ranges.jank` inlines the C's AddCodepointRange into `-main`'s
rebuild block for exactly this reason. Pure-jank helpers (no shim calls)
remain safe to factor out. (`viewport_scaling.jank` was unaffected only
because all its shim calls already sat in `-main`; `compute_hash.jank` /
`storage_values.jank` / `codepoints_loading.jank` are safe because their
statics are written and read within one fn call's dynamic extent, not
across fns.)

## Create-once native resources

`LoadRenderTexture` / `GetFontDefault` style resources bind in a `let`
outside the frame loop and get used inside it; lexical capture keeps them
native. Unload after the loop.

```clojure
(let [canvas (cpp/LoadRenderTexture WIDTH HEIGHT)]
  (loop [...]
    ... (cpp/BeginTextureMode canvas) ...)
  (cpp/UnloadRenderTexture canvas))
```

Two RenderTextures at once work (`camera_2d_split_screen.jank`). Blit a
RenderTexture with a **negative source height**: RTs are stored upside
down (`lines_drawing.jank`, `window_letterbox.jank`).
