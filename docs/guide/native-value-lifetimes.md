# Native value lifetimes

jank is **native** Clojure (C++/LLVM) — no JVM, no Java interop, no REPL.
The compiler statically type-checks the boundary between jank objects and
native C++ values, and this page is about the single rule that boundary
enforces: **a native cpp value only stays native within the form that
produced it.** Every lesson below is a consequence of that rule. Each
lesson names the committed example that proves it — those files are the
running test suite for this document.

## The one rule that explains most crashes

**A native cpp value only stays native within the form that produced it.**
A `Color`, `Vector2`, `Rectangle`, `Camera2D`, ... may be:

- constructed inline as a call argument — `(cpp/DrawCircleV (cpp/Vector2 ...) ...)` ✅
- bound to a `let`-local and used in the same scope ✅
- bound in a `let` OUTSIDE the frame loop and used inside it (lexical
  capture) ✅ — this is how create-once GPU resources live
  (`lines_drawing.jank`'s RenderTexture, `words_alignment.jank`'s Font)

But it may NOT cross a jank fn boundary:

- returned from a fn ❌ — `returning a native object of type 'Color', which
  is not convertible to a jank runtime object`. Even via nested `if`
  (`dashed_line.jank` learned this).
- passed as a fn parameter and then used in a native call ❌ — it boxes to an
  `object_ref` and the native call rejects it (`digital_clock.jank`'s
  draw-hand; `tiled_drawing.jank` hit this trying to pass a `Texture2D` to a
  `draw-tiled` helper — `No matching call to 'DrawTexturePro' ... argument 0
  having type 'jank::runtime::object_ref &'` — and had to inline the helper so
  the texture stayed a captured `let`-local).
- carried through `loop`/`recur` state ❌ (`input_mouse.jank`).

**Fix:** thread plain jank data (ints, reals, keywords, maps) and resolve the
native value inline at the use site. `camera_2d_platformer.jank` threads the
camera as five scalars and rebuilds `(cpp/Camera2D ...)` each frame;
`input_mouse.jank` threads a `color-id` int and picks the `Color` with a
nested `if` at draw time.

## Frame-crossing mutable native state: park it in a cpp/raw static

When a native resource must BOTH persist across frames AND be recreated at
runtime with computed sizes, neither of the two usual homes works: `loop`/
`recur` state can't carry a native value (the same rule as above — a native
value can't cross a fn boundary — applied to loop/recur state), and a
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
outside the frame loop and get used inside it — lexical capture keeps them
native. Unload after the loop.

```clojure
(let [canvas (cpp/LoadRenderTexture WIDTH HEIGHT)]
  (loop [...]
    ... (cpp/BeginTextureMode canvas) ...)
  (cpp/UnloadRenderTexture canvas))
```

Two RenderTextures at once work (`camera_2d_split_screen.jank`). Blit a
RenderTexture with a **negative source height** — RTs are stored upside
down (`lines_drawing.jank`, `window_letterbox.jank`).
