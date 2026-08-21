# The C++ interop toolbox

> **The `cpp/` forms themselves are documented in the jank book**:
> [Reaching into C++](https://book.jank-lang.org/cpp-interop/index.html),
> [native functions](https://book.jank-lang.org/cpp-interop/native-functions.html),
> [the C++ DSL](https://book.jank-lang.org/cpp-interop/dsl.html),
> [embedding raw C++](https://book.jank-lang.org/cpp-interop/cpp-raw.html),
> [the cpp namespace](https://book.jank-lang.org/cpp-interop/cpp-ns.html).
> This page is only what porting 209 raylib examples added on top, each
> claim naming the example that proves it.

## Pointer-taking APIs: `(cpp/& x)`

The whole `Image*` mutation family (`ImageFormat`, `ImageColorInvert`,
`ImageBlurGaussian`, `ImageDrawCircle`, ...) takes an `Image *` first arg.
Address-of a **mutable let-local**, mirroring the C:

```clojure
(let [img (cpp/GenImageColor 256 256 cpp/BLANK)]
  (cpp/ImageDrawCircle (cpp/& img) 128 128 100 cpp/RED)  ; mutates in place
  (let [tex (cpp/LoadTextureFromImage img)] ...))        ; picks up the change
```

`image_processing.jank` (nine filters). Nothing outside the example file
changes: every raylib fn is already callable through `(:include "raylib.h")`.

A mutated `Image` is still a native value, so it cannot ride `loop`/`recur`.
Keep build-mutate-read-unload inside one `let` and carry only the index
(`image_processing.jank`).

## `int *` out-params

`LoadImageAnim(fileName, int *frames)` returns the image and writes the count:

```clojure
(let [frames  (cpp/int 0)                          ; native int lvalue
      img     (cpp/LoadImageAnim path (cpp/& frames))
      nframes (int (+ 0.0 frames))]                ; box back to a jank int
  ...)
```

`gif_player.jank`. `(+ 0.0 frames)` re-boxes before `(int …)`; a bare
`(int frames)` on the raw native value can trip codegen the same way an
all-native `f64` chain does (see
[`type-checking-and-coercion.md`](type-checking-and-coercion.md)).

## Pointer arithmetic on a runtime arg needs `cpp/raw`

`cpp/cast` uses `convert`, not `reinterpret_cast`, so it cannot turn a
`void*` into `unsigned char*` for byte math. One shim does it:

```clojure
(cpp/raw "static const void* jank_gif_frame_ptr(void* data, int offset) {
  return (const void*)(((unsigned char*)data) + offset);
}")
(cpp/UpdateTexture tex (cpp/jank_gif_frame_ptr (.-data img) (int offset)))
```

`gif_player.jank`. Keep `offset` an `int`: `mod`/`quot` return reals, which
the shim's `int` param rejects with `expected integer found small_real`.

`cpp/raw` globals are also the escape hatch for array APIs: a `float[9]`
declared in a `cpp/raw` block decays to `float*` when passed straight to
`(cpp/ImageKernelConvolution (cpp/& img) cpp/jank_sharpen_kernel 9)`
(`image_kernel.jank`).

## Shader uniforms: per-type scalar C setters

`SetShaderValue` wants a `const void*` at native `float[]` data, which jank
cannot form from a jank vector. Give each uniform type its own C fn that
builds the array *inside* C:

```c
static void jank_set_vec4(Shader s, int loc, double a, double b, double c, double d) {
  float v[4] = { (float)a, (float)b, (float)c, (float)d };
  SetShaderValue(s, loc, v, SHADER_UNIFORM_VEC4);
}
```

One `cpp/` call per uniform, no shared buffer, and the `(float)` cast happens
in C so you skip the `cpp/float`/`(+ 0.0 …)` dance
(`rounded_rectangle_shader.jank`, `color_correction.jank`).

- **From a native struct**: don't read `.-x`/`.-y`/`.-z` in jank. Pass
  `(cpp/& camera)` and read the fields in C (`raymarching.jank`).
- **Array uniforms** (`SetShaderValueV`) still need a staging buffer: a
  file-level `static int jank_ui_buf[24]`, an index setter, and a send fn.
  Fill it with `loop`/`nth`, then send once. All calls must sit in one fn,
  per the static rule below (`palette_switch.jank`).
- An older static-staging-buffer approach is still in
  `texture_outline.jank`, `texture_waves.jank`, `mandelbrot_set.jank`.
- `GetShaderLocation` returns a plain int; `(int (cpp/GetShaderLocation …))`
  boxes it. `LoadShader cpp/nullptr path` takes the default vertex shader.

## Callback-taking APIs: define the callback in `cpp/raw`

jank cannot form a C function pointer, but a callback defined inside a
`cpp/raw` block is ordinary C, and a sibling wrapper attaches it:

```c
static void jank_process_audio(void *buffer, unsigned int frames) { ... }
static void jank_attach_processor(void) { AttachAudioMixedProcessor(jank_process_audio); }
```

Proven for audio-thread DSP (`mixed_processor.jank`), `SetTraceLogCallback`
(`custom_logging.jank`, installed before `InitWindow`), and raw audio streams
(`raw_stream.jank`). jank tunes parameters through setters and reads results
through accessors, never touching the callback thread.

**Gotcha: a C callback that `printf`s must `fflush(stdout)` itself.** raylib's
own `TraceLog` flushes; your replacement does not inherit that. stdout is
fully buffered when redirected, so the headless smoke recipe (`> log 2>&1`)
loses the whole buffer when `timeout` SIGTERM-kills the process: an empty log
that looks like a broken port. `custom_logging.jank`'s first smoke returned
zero lines; adding `fflush` surfaced all 42.

## Shared helpers are a jank namespace, not a C header

This project once shipped rlights as a C header, on the belief that a jank fn
could neither take nor return a native value. Opaque boxes make that false.
The header was only taking the address of a value (`cpp/new` does that) and
holding structs (a jank map does that).

`rlights.jank` is the result: no header, no `cpp/raw`, nothing to install.
`shaders.jank` and `models.jank` cover shader uniforms and material binding
the same way. The boundary rules that shape these helpers, boxing, `cpp/=`,
and why an accessor cannot be factored out, are in
[`native-value-lifetimes.md`](native-value-lifetimes.md#the-four-faces-of-one-boundary-rule).

## Known-blocked constructs

- **Native array indexing works** (2026-07-04). `(cpp/aget p (cpp/int i))`
  on `unsigned int*` is proven in `compute_hash.jank` (matches the canonical
  CRC32/SHA1/SHA256 vectors; u32 boxes to jank ints without sign damage), and
  on `int*` in `codepoints_loading.jank`. Struct arrays work too:
  `(cpp/aget (.-glyphs font) (cpp/int i))` returns a `GlyphInfo` by value with
  working `.-value`/`.-advanceX` reads. Verified by probe, not yet
  load-bearing in a committed example, so keep the probe habit.
- **`Vector2 *points` array APIs** (`DrawSplineLinear`, `DrawTriangleStrip`)
  are unproven, but the write direction is: `codepoints_loading.jank` fills a
  `cpp/raw` static `int[512]` through a setter shim and hands the pointer to
  `LoadFontEx`. A `Vector2[]` should work the same way, and `cpp/new` +
  `cpp/=` are proven. Visual-equivalent workarounds still apply: per-segment
  `DrawLineEx` (`math_sine_cosine.jank`) or recomputing vertices inline
  (`triangle_strip.jank`). By-value struct APIs like `DrawLineDashed` are fine.
- **`Image` pixel manipulation and the `rlgl` API are NOT blocked.** The
  `raylib-sys` package ships `rlgl.h` next to `raylib.h` and the rlgl
  functions are compiled into `libraylib`, so `(:include "raylib.h" "rlgl.h")`
  just works (`rlgl_triangle.jank`).
- **Mutable C string buffers** (`TextCopy` and friends) are not worth
  simulating; `text_strings_management` was skipped on those grounds.
