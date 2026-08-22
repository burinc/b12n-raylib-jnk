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

## Pointer arithmetic: `cpp/unsafe-cast`

`cpp/cast` uses `convert`, not `reinterpret_cast`, so it cannot turn a `void*`
into `unsigned char*` for byte math. `cpp/unsafe-cast` can:

```clojure
(cpp/UpdateTexture tex
  (cpp/unsafe-cast (:* (:const cpp/void))
    (cpp/+ (cpp/unsafe-cast (:* (:unsigned cpp/char)) (.-data img))
           (cpp/int offset))))
```

`gif_player.jank`. Keep `offset` a native int: `mod`/`quot`/`rem` return reals.

Same pair reaches native arrays. `cpp/MemAlloc` + `cpp/unsafe-cast` gives a
`float*` that `cpp/aget` and `cpp/=` fill element-wise, which is how
`image_kernel.jank` builds its convolution kernels and how
`spectrum_visualizer.jank` holds its FFT buffers. `(cpp/new (:array T n))`
does not work; this is the way around it, and it costs ergonomics rather than
capability.

## Shader uniforms: `shaders.jank`

`SetShaderValue` wants a `const void*` at native `float[]` data. raylib's
`Vector2`/`3`/`4` **are** contiguous float arrays, so one can be constructed
inline as the argument and no staging buffer is needed:

```clojure
(cpp/SetShaderValue (cpp/* s) (int loc)
                    (cpp/new cpp/Vector4 (cpp/float x) (cpp/float y)
                                         (cpp/float z) (cpp/float w))
                    cpp/SHADER_UNIFORM_VEC4)
```

`shaders.jank` wraps that as `set-int!`, `set-float!`, `set-vec2!`/`3!`/`4!`
and `set-shader-loc!`, taking a boxed `Shader`. Every shader example in the
repo uses it; none carries a C setter.

- **From a native struct**, read `.-x`/`.-y`/`.-z` and pass them
  (`raymarching.jank`).
- **Array uniforms** (`SetShaderValueV`) fill a `cpp/MemAlloc` array
  (`palette_switch.jank`).
- `GetShaderLocation` returns a plain int; `(int (cpp/GetShaderLocation …))`
  boxes it. `LoadShader cpp/nullptr path` takes the default vertex shader.

## Callback-taking APIs: the one genuine gap

**There is no way to hand a jank fn to a C API that takes a function
pointer.** Confirmed three ways: the complete set of `cpp/` special forms
(`box cast delete dsl new raw unbox unsafe-cast value`) has nothing for it,
the compiler has no such conversion, and the call is rejected outright:

```
error: No matching call to 'AttachAudioMixedProcessor' function.
       With argument 0 having type 'jank::runtime::object_ref &'.
```

This is the only remaining reason any example here still carries `cpp/raw`.
A callback defined inside a `cpp/raw` block is ordinary C, and a sibling
wrapper attaches it:

```c
static void jank_process_audio(void *buffer, unsigned int frames) { ... }
static void jank_attach_processor(void) { AttachAudioMixedProcessor(jank_process_audio); }
```

Four examples need it: `mixed_processor.jank`
(`AttachAudioMixedProcessor`), `stream_effects.jank`
(`AttachAudioStreamProcessor`), `stream_callback.jank`
(`SetAudioStreamCallback`), and `custom_logging.jank`
(`SetTraceLogCallback`, which is variadic as well, so it is blocked twice
over). jank tunes parameters through setters and reads results through
accessors, never touching the callback thread.

**Being pushed from a callback is the blocker, not audio work.**
`amp_envelope.jank` and `raw_stream.jank` do the same 4096-sample refill in
pure jank because they pull from the main loop instead. Only the push
direction needs C.

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
- **Native arrays are built with `cpp/MemAlloc` + `cpp/unsafe-cast`**, not
  `cpp/new`. `codepoints_loading.jank` fills an `int*` that way and hands it
  to `LoadFontEx`; a `Vector2[]` works the same. Visual-equivalent
  workarounds still apply where they are simpler: per-segment `DrawLineEx`
  (`math_sine_cosine.jank`) or recomputing vertices inline
  (`triangle_strip.jank`).
- **`Image` pixel manipulation and the `rlgl` API are NOT blocked.** The
  `raylib-sys` package ships `rlgl.h` next to `raylib.h` and the rlgl
  functions are compiled into `libraylib`, so `(:include "raylib.h" "rlgl.h")`
  just works (`rlgl_triangle.jank`).
- **Mutable C string buffers** (`TextCopy` and friends) are the subject of
  `strings_management.jank`, which keeps its C on those grounds rather than
  simulating them.
