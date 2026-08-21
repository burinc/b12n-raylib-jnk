# The C++ interop toolbox

The [native-value-lifetime rule](native-value-lifetimes.md) seems to block
a lot — a native value can't be returned, passed as a parameter, or carried
through `loop`/`recur`. This page is the toolbox that reaches around those
constraints: pointer interop, out-params, callbacks, shared C headers, and
shader-uniform shims — and, in the last section, what's still blocked.
Each lesson names the committed example that proves it.

## Shader uniforms: prefer per-type scalar C setters (from the shader arc, 2026-07-05)

`SetShaderValue` takes a `const void*` pointing at native `float[]` data, which
jank can't form from a jank vector. Two shims solve it; the second is now the
preferred one:

- **Static staging buffer (first approach).** A file-level `static float
  jank_uf_buf[4]`, an index setter `jank_uf_set(int i, double v)`, and a
  `jank_set_uniform(Shader s, int loc, int type)` that calls `SetShaderValue`
  on the buffer. To push a VEC2 you fill slots 0 and 1 then call the setter
  with `SHADER_UNIFORM_VEC2`. Works, but every uniform is 2–5 jank calls and
  all of them must stay in one fn (the buffer is shared mutable state — the
  per-fn static rule). INT needs a parallel `int` slot + setter
  (`texture_outline.jank`, `texture_waves.jank`, `mandelbrot_set.jank`).
- **Per-type scalar setters (preferred).** Give each uniform type its own C
  fn that takes the components as `double`/`int` scalars and builds the small
  array *inside C*:
  ```c
  static void jank_set_vec4(Shader s, int loc, double a, double b, double c, double d) {
    float v[4] = { (float)a, (float)b, (float)c, (float)d };
    SetShaderValue(s, loc, v, SHADER_UNIFORM_VEC4);
  }
  ```
  Now each uniform is **one** `cpp/` call — `(cpp/jank_set_vec4 shader loc r g b a)`
  — with no shared buffer, so it reads cleanly and the per-fn-static constraint
  goes away (`rounded_rectangle_shader.jank` for VEC4/VEC2/FLOAT,
  `color_correction.jank` for FLOAT). Pass jank doubles straight in; the `(float)`
  cast happens in C, so you dodge the `cpp/float`/`(+ 0.0 …)` boxing dance.
- **VEC uniforms sourced from a native struct** (a Camera3D's position/target):
  don't read `.-x`/`.-y`/`.-z` field-by-field in jank — hand the setter the
  struct pointer via `(cpp/& camera)` (the pointer-taking-APIs pattern) and read
  the fields in C: `jank_set_view(Shader s, int eyeLoc, int centerLoc,
  Camera3D* c)` fills two `float[3]`s from `c->position`/`c->target`
  (`raymarching.jank`, the first VEC3 uniforms).
- **Array uniforms (`SetShaderValueV`) revive the staging buffer.** A
  per-type scalar setter can't take an 8 x ivec3 palette as arguments, so
  arrays go back to approach one: a file-level `static int jank_ui_buf[24]`,
  an index setter, and a `jank_set_uniform_v(Shader s, int loc, int type,
  int count)` that calls `SetShaderValueV` on the buffer with the element
  count. Fill the buffer from a flat jank vector with a `loop`/`nth`, then
  send once (`palette_switch.jank`, the first array uniform — the same
  static-buffer caveat applies: all calls in one fn).
- `GetShaderLocation` returns a plain int — `(int (cpp/GetShaderLocation …))`
  boxes it for a `let`. `LoadShader cpp/nullptr path` uses the default vertex
  shader; pass a real `base.vs` path as the first arg when the example ships one
  (`rounded_rectangle_shader.jank`).

## Callback-taking APIs: define the callback in cpp/raw (2026-07-11)

jank cannot form a C function pointer, but a callback DEFINED inside a
`cpp/raw` block is ordinary C — a sibling wrapper in the same block
attaches its pointer:

```c
static void jank_process_audio(void *buffer, unsigned int frames) { ... }
static void jank_attach_processor(void) { AttachAudioMixedProcessor(jank_process_audio); }
```

Proven with the audio-thread DSP callback in `mixed_processor.jank`
(the callback mutates/reads C statics; jank tunes parameters through
setters and reads results through accessors — never touching the
callback thread directly). The same shape unlocks any
callback-registering raylib API. `SetTraceLogCallback` is now proven
too (`custom_logging.jank`): a `void (int, const char*, va_list)`
callback defined in `cpp/raw` (with its own `time`/`strftime`/`vprintf`
va_list machinery) is handed to `SetTraceLogCallback` by a sibling
wrapper, installed *before* `InitWindow`, and reformats every raylib
log line. Raw audio streams (`LoadAudioStream` + `UpdateAudioStream`
via a C-shim refill) landed in the same arc (`raw_stream.jank`).

**Gotcha — a custom C callback that writes via `printf` must
`fflush(stdout)` itself.** raylib's own `TraceLog` flushes after every
line (`rcore.c`), but your replacement callback doesn't inherit that.
stdout is fully buffered when redirected to a file (as the headless
smoke recipe does, `> log 2>&1`), so without a flush the buffered log
sits unwritten and is *lost entirely* when `timeout` SIGTERM-kills the
process — the smoke log comes back empty and the port looks broken when
it isn't. Add `fflush(stdout);` at the end of any shim callback that
prints. Trace: `custom_logging.jank` first smoke returned zero log
lines; adding `fflush` surfaced all 42 reformatted lines.

## Shared helpers are a jank namespace, not a C header

This page used to advise shipping shared C helpers as a header from the
`-sys` wrapper, and this project did exactly that for rlights. **That was
the wrong call**, and the reasoning behind it was wrong: it rested on the
belief that a jank fn could neither take nor return a native value, so light
state had to live in a static C array behind scalar wrappers.

jank's opaque boxes carry native values across fn boundaries. Stripped down,
the header was only doing two things a jank namespace can do:

- **Taking the address of a value** for an API like `SetShaderValue`.
  `cpp/new` produces that pointer directly:
  `(cpp/new cpp/Vector3 (cpp/float x) (cpp/float y) (cpp/float z))`.
- **Holding structs.** A `Light` is plain data plus five cached `int`
  uniform locations. An ordinary jank map holds that; there was never a
  reason for a C array.

`rlights.jank` is the result — no header, no `cpp/raw`, nothing to install,
and a project that wants lighting copies one file. See
[`native-value-lifetimes.md`](native-value-lifetimes.md#getting-a-native-value-out-anyway).

Three things about boxing that cost a compile each to learn:

- A `Shader` passed as a jank fn argument arrives as an `object_ref` and the
  native call rejects it. Box it: `(cpp/box (cpp/new cpp/Shader shader))`,
  then `cpp/unbox` + `cpp/*` inside the helper.
- **The boxing cannot be wrapped in a helper fn.** By the time a fn body
  runs, its argument is already an `object_ref` and `cpp/new` has nothing to
  copy from. The box must be made where the value is still native.
- A `Camera3D` argument has no readable members for the same reason, so
  destructure it at the call site and pass scalars.

**When a C shim is still unavoidable:** assignment. jank has no assignment
form at all — no `set!`, no `cpp/aset`, no member write — so
`shader.locs[idx] = loc` needs three lines of C. `rlights.jank` keeps
exactly one such shim, and it is the only `cpp/raw` in the lighting path.
Note that a boxed copy shares the pointer members of the original (`Shader`
is `{unsigned int id; int *locs;}`), so writing through the box is visible
to the caller.

## Pointer-taking APIs work via `(cpp/& x)` (from the image arc, 2026-07-03)

**jank has native pointer interop — the pointer-taking raylib APIs were never
actually blocked.** The whole `Image*` mutation family (`ImageFormat`,
`ImageColorGrayscale/Tint/Invert/Contrast/Brightness`, `ImageBlurGaussian`,
`ImageFlipVertical/Horizontal`, `ImageDrawCircle`, `ImageDrawRectangle`, ...)
takes an `Image *` first arg. Form it with `(cpp/& img)` — the address-of a
**mutable let-local** — exactly mirroring the C `ImageColorInvert(&imCopy)`:

```clojure
(let [img (cpp/GenImageColor 256 256 cpp/BLANK)]
  (cpp/ImageDrawCircle (cpp/& img) 128 128 100 cpp/RED) ; mutates img in place
  (let [tex (cpp/LoadTextureFromImage img)] ...))       ; picks up the change
```

Proven end-to-end in `image_processing.jank` (nine filters) and the spike that
preceded it. No `-sys` wrapper change is needed — every raylib fn is already
callable through `(:include "raylib.h")`; the only missing piece was knowing
the address-of form.

The wider jank cpp-interop toolbox — see the
[jank book's cpp-interop chapter](https://github.com/jank-lang/jank/tree/main/book/src/cpp-interop)
for the authoritative reference on each of these:

- `(cpp/& x)` — address-of (unary). `(cpp/* p)` — dereference (unary).
- `(cpp/aget arr (cpp/int i))` / `(aset arr (cpp/int i) v)` — array element
  get/set. Likely unblocks `font.glyphs[i]` / `int*` indexing.
- `(cpp/new T init)` — heap allocation returning a pointer; type DSL `(:* T)`
  for pointer types, `(cpp/cast (:* void) x)` / `(cpp/unbox (:* T) box)`.
- `(cpp/raw "…C++…")` — embed arbitrary C++ (helper fns, out-param shims,
  constant arrays) right in the `.jank` file. The fallback when a jank form
  for some pointer dance doesn't exist yet. PROVEN in `image_kernel.jank`:
  the three `float[9]` convolution kernels + a `jank_normalize_kernel`
  helper are declared as C globals in a `cpp/raw` block, and each global
  array decays to `float*` when passed straight to
  `(cpp/ImageKernelConvolution (cpp/& img) cpp/jank_sharpen_kernel 9)`. This
  is the general escape hatch for the `Vector2 *points` / `int *` array APIs
  (`DrawSplineLinear`, etc.) — build/fill the array in `cpp/raw` and pass the
  global.

**`int *` out-params** (PROVEN in `gif_player.jank`). Some raylib fns write a
scalar back through a pointer arg — `LoadImageAnim(fileName, int *frames)`
returns the image AND writes the frame count into `*frames`. jank makes a
mutable native int, passes its address, then boxes the result for jank code:

```clojure
(let [frames (cpp/int 0)                                   ; a native int lvalue
      img    (cpp/LoadImageAnim path (cpp/& frames))       ; writes *frames
      nframes (int (+ 0.0 frames))]                        ; box back to jank int
  ...)
```

Note `(+ 0.0 frames)` re-boxes the native `int` into a jank object before
`(int …)` — a bare `(int frames)` on the raw native value can trip codegen the
same way an all-native `f64` chain does (the all-native-chain codegen trap —
see `type-checking-and-coercion.md`).

**Runtime-arg pointer arithmetic via `cpp/raw`.** `gif_player` streams each GIF
frame from `image.data + w*h*4*frame`. jank's `cpp/cast` uses `convert`, not
`reinterpret_cast`, so it can't turn the `void*` `image.data` into a
`unsigned char*` for byte math. A one-line `cpp/raw` shim taking the runtime
pointer + offset does the cast+arithmetic in C and hands the frame pointer
straight to `UpdateTexture`:

```clojure
(cpp/raw "static const void* jank_gif_frame_ptr(void* data, int offset) {
  return (const void*)(((unsigned char*)data) + offset);
}")
;; ... per frame:
(cpp/UpdateTexture tex (cpp/jank_gif_frame_ptr (.-data img) (int offset)))
```

Keep `offset` an `int` at the call site — `mod`/`quot` on the frame index
return reals (the mod/quot-returns-reals trap), which the shim's `int offset`
param rejects at runtime
(`expected integer found small_real`). Wrap the frame advance:
`(int (mod (+ cur 1) nframes))`.

Lifecycle caveat (interaction with the native-value-can't-cross-loop/recur
rule): a mutated `Image` is still a **native value**, so it can't be carried
in `loop`/`recur` state. Keep the whole
build-mutate-read-unload cycle inside a `let` in the frame that needs it
(`image_processing`'s reload block rebuilds `imCopy` from `imOrigin`, processes
it via `(cpp/& imCopy)`, reads it back with `LoadImageColors` → `UpdateTexture`,
and `UnloadImage`s it — all in one block; only the process **index** lives in
loop state).

## Known-blocked constructs

- **Native array indexing** — NO LONGER BLOCKED (2026-07-04):
  `(cpp/aget p (cpp/int i))` on an `unsigned int*` is proven in
  `compute_hash.jank`, which reads the static u32 arrays returned by
  `ComputeMD5`/`SHA1`/`SHA256` and matches the canonical CRC32/SHA1/SHA256
  test vectors (the u32 elements box to jank ints without sign damage).
  `codepoints_loading.jank` adds a load-bearing `int*` walk
  (`LoadCodepoints`' array snapshotted into a jank vector).
  **Struct arrays work too**: `(cpp/aget (.-glyphs font) (cpp/int i))` on
  a `GlyphInfo*` returns the struct by value into a let-local with
  working field reads (`.-value`, `.-advanceX`), and the same on the
  `Rectangle*` in `font.recs` — verified by a throwaway probe against
  `GetFontDefault` (2026-07-04, glyph 1 = codepoint 33, rec width 1.0);
  not yet load-bearing in a committed example, so keep the probe habit
  when reaching for it. The earlier `cpp/raw` subscript shims
  (`core_random_sequence`'s `int*`, the `char**` walks) remain fine but
  are no longer the only way in; `text_rectangle_bounds` predates the
  probe and uses the `MeasureTextEx` re-wrap instead. (`int *`
  **out-params** were already proven — see `gif_player` in the
  pointer-interop section.)
- **APIs taking a `Vector2 *points` array** — `DrawSplineLinear`,
  `DrawTriangleStrip`, etc.: for `int*` the write direction IS now proven —
  `codepoints_loading.jank` fills a `cpp/raw` static `int[512]` element-wise
  through a one-line setter shim (`jank_cp_set`) and hands the pointer to
  `LoadFontEx`. A `Vector2[]` should work the same way (a setter shim taking
  x,y scalars), but that's still unprobed; `cpp/new` + `aset` also remain
  unprobed. Workarounds that keep the visual identical still
  apply: draw per-segment `DrawLineEx` between consecutive points
  (`math_sine_cosine.jank`'s waves), or recompute each primitive's vertices
  inline instead of filling an array (`triangle_strip.jank` draws each wedge's
  two `DrawTriangle`s from the angle formulas directly — the wrap-around falls
  out of cos/sin periodicity). By-value struct APIs like `DrawLineDashed
  (raylib 6.0)` work fine.
- ~~**`Image` pixel manipulation**~~ — NOT blocked (2026-07-03): the
  pointer-taking `Image*` / `ImageColor*` / `ImageDraw*` APIs all work via
  `(cpp/& img)` (`image_processing.jank`). See the pointer-interop section
  above.
- ~~**The `rlgl` API**~~ — NOT blocked after all (2026-07-03):
  `jank-raylib-sys` installs `rlgl.h` next to `raylib.h` and the rlgl
  functions are compiled into `libraylib`, so `(:include "raylib.h"
  "rlgl.h")` just works — `rlBegin`/`rlColor4ub`/`rlVertex2f`/`rlEnd` and
  the culling toggles all run directly (`rlgl_triangle.jank`).
- **Mutable C string buffers** — `TextCopy` and friends; not worth
  simulating (`text_strings_management` skipped on these grounds).
