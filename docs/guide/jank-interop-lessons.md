# jank interop lessons

Every jank/C++ sharp edge we've hit porting the raylib examples, organized by
theme. Each lesson names the committed example that proves it — those files
are the runnable test suite for this document. The distilled version lives in
`AGENTS.md`; this is the fuller story.

jank is **native** Clojure (C++/LLVM). There is no JVM, no Java interop, no
REPL (dev is recompile-and-run), and the compiler statically type-checks the
boundary between jank objects and native C++ values. Most lessons below are
about that boundary.

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
`recur` state can't carry a native value (rule 2), and a create-once
outer-`let` local can't be rebound. Park the value in a `cpp/raw` static
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

## if/cond branch type-checking

jank type-checks every `if` branch. `cond`/`case` expand with an implicit
trailing `nil`, which clashes with a native value type:
`Mismatched 'if' branch types 'Color' and 'nil'`.

- `cond` returning **jank** values (maps, keywords, strings, vectors) is
  fine — state machines and `[x vx]`-vector returns all work.
- Picking a **native** value needs hand-nested `if`s where every branch ends
  in a concrete value: `(if on-text? cpp/RED cpp/DARKGRAY)` as a call
  argument is fine (`input_box.jank`, `bullet_hell.jank`).

**`and`/`or` are `if`s too — a native struct-field bool clashes with a jank
bool.** `(and native-bool jank-bool)` expands to
`(let [a native-bool] (if a jank-bool a))`, so its two implicit branches are
`jank-bool` (plain `bool`) and `native-bool`. A struct-field read like
`(.-hit collision)` types as `bool &` (a reference), not `bool`, so the
combined form errors with `Mismatched 'if' branch types 'bool' and 'bool &'`.
The read is *only* a problem when a boolean-combining macro forces the two
types to unify — using `(.-hit c)` straight as an `if`/`when` condition is
fine (`picking_3d.jank`). Fix: don't `and` a native field bool with a jank
bool; hand-nest the `if`s so the native bool is always just a condition,
never a returned branch value (`basic_voxel.jank`'s ray-pick loop:
`(if (.-hit coll) (if (< d best-d) ...) ...)`).

## Numeric traps

| Trap | Symptom | Fix | Proof |
|---|---|---|---|
| `mod`/`quot` return reals | `expected integer found small_real` at an int param, or a broken `nth` | wrap in `(int ...)` | everywhere; `writing_anim.jank` |
| `cpp/float` wants a REAL arg | `expected real found small_integer` | `(cpp/float (+ 0.0 n))` | `lines_drawing.jank` boxes `GetMouseX` |
| `(/ int int)` shape is unreliable | subtle | precompute constants or `(int (quot ...))` | `window_letterbox.jank` uses `(int (/ GAME-H 10))` |
| `min`/`max` reject a raw C double | `invalid operands to binary expression` deep in math.hpp | box first with `(+ 0.0 x)`, or clamp with `if` | `dashed_line.jank`; isolated to min/max only — `+ - * / < <= =` all take raw doubles (`ellipse_collision.jank`) |
| `min`/`max` also reject a boxed int mixed with `(int ...)`'s unboxed i64 | same math.hpp template error (`oref<small_integer>` vs `long long`) | clamp with `if` comparisons instead (`<` / `>=` take the mix fine) | `first_person_maze.jank` cell clamp |
| `str` with >10 args + a raw `(int ...)` in the tail | codegen error: `member reference base type 'i64' ... .erase()` | build long strings in two `str` calls of ≤10 args | `bullet_hell.jank` status line |
| `cpp/float` on an ALL-native arithmetic chain | codegen error: `convert<float>::from_object` — `no known conversion from 'f64'` | route one operand through a boxed source, e.g. a vector lookup: `(nth [0 fh (* 2 fh)] state)` | `sprite_button.jank` frame offset |
| `(int cpp/KEY_*)` on a C **enum** constant | template error: `member reference base type 'const KeyboardKey'` in `to_int` | cast the enum to a native int first with `(cpp/int cpp/KEY_*)` — the result then boxes fine into jank maps/vectors and round-trips through `int` params like `IsKeyDown` | `keyboard_testbed.jank` ROW data |

Boxing idiom: `(+ 0.0 x)` turns a raw C int/float/double into a jank real;
`(int x)` truncates to a jank integer. `cpp/GetFrameTime`, `cpp/GetTime`,
`cpp/GetMouseWheelMove` returns are routinely boxed at the binding site.

The all-native chain trap is the subtle cousin of the `expected real`
one: `(+ 0.0 expr)` only *boxes* when at least one input is already a
jank object. When every value in the chain derives from literals and
native reads (`(.-height tex)`, if-of-literals), jank keeps the whole
expression as an unboxed native `f64`, and `cpp/float`'s generated
`from_object` call cannot take it. The same shape compiles fine when a
`loop`/`recur` variable feeds the chain (loop vars are boxed) — that is
why `sprite_animation.jank`'s near-identical frame math never hit it.
Any jank collection operation re-boxes: `nth` on a vector of the
possible offsets is the cheapest escape hatch.

## Constructing colors and structs from data

- `cpp/Color` (the struct ctor) wants native `unsigned char` fields — jank
  ints don't match: `No matching call to 'Color' constructor`. Struct ctors
  demand exact native types; **functions** coerce jank ints happily.
- So build Colors through *functions*: pack RGBA into an int and call
  `cpp/GetColor` — `(cpp/GetColor (+ (* r 16777216) (* g 65536) (* b 256) a))`
  (`camera_2d.jank` skyline, `recursive_tree.jank` panel) — or use
  `cpp/ColorFromHSV`, `cpp/ColorLerp`, `cpp/Fade`. The C idiom
  `(Color){0,0,0,200}` becomes `(cpp/Fade cpp/BLACK (cpp/float 0.784))`
  (`bullet_hell.jank`).
- Struct ctors compose inline: `cpp/Camera2D` takes two nested `cpp/Vector2`
  plus two `cpp/float` args, passed straight to `cpp/BeginMode2D`
  (`camera_2d.jank`).
- **Struct `int` fields need `(cpp/int n)` casts** — the same "struct ctors
  demand exact native types" rule that bites `cpp/Color` also bites `int`
  fields, but here there IS a cast helper (unlike `unsigned char`). A jank
  int literal reaches the ctor as `small_integer_ref`, which doesn't convert
  to native `int` in the generated braced-init: `No matching call to
  'NPatchInfo' constructor ... argument 1 having type ...small_integer_ref`.
  Wrap each int field in `(cpp/int n)` (mirror of `cpp/float` for reals):
  `(cpp/NPatchInfo (cpp/Rectangle ...) (cpp/int 12) (cpp/int 40) (cpp/int 12)
  (cpp/int 12) cpp/NPATCH_NINE_PATCH)` (`npatch_drawing.jank`). The trailing
  enum constant (`cpp/NPATCH_NINE_PATCH`) converts to the `int` layout field
  on its own. Note the asymmetry vs `cpp/Color`: int-field structs get the
  `cpp/int` escape hatch, so no GetColor-style function detour is needed.
- Field access works with `.-`: `(.-texture render-texture)`,
  `(.-x measured-vec2)`, and on pointer returns `(.-tm_hour lt)`
  (`lines_drawing.jank`, `words_alignment.jank`, `digital_clock.jank`).

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

## Fonts load like textures (from the text arc, 2026-07-03)

**Custom font loading works with zero wrapper changes.** `LoadFont` (BMFont
`.fnt` + its `.png` atlas) and `LoadFontEx` (a TTF rasterized at load, with a
base size + glyph count) both return a `Font` native struct — treat it exactly
like a `Texture2D`: bind it in the outer `let`, draw with `DrawTextEx` inside
the loop, `UnloadFont` after. `LoadFontEx`'s codepoint-array arg is passed as
`cpp/nullptr` for the default set. `.-baseSize` reads back fine (nth-box it for
the `cpp/float` size arg). Proof lines: `FONT: ... Font loaded successfully`,
`FONT: Data loaded successfully (32 pixel size | 184 glyphs)`
(`font_loading.jank`). String literals MAY be UTF-8 (disproving an earlier
version of this note): `codepoints_loading.jank` defs the Japanese Iroha
pangram in-source, and the lexer, `LoadCodepoints`, and `DrawTextEx` all
handle it. The ASCII-only restriction is about COMMENTS (the em-dash
`lex/invalid-unicode` trip).

## Models load like fonts (from the model arc, 2026-07-11)

**Mesh/Model loading works with zero wrapper changes** — the long-assumed
"LoadModel blocker" was never real, exactly as with `Font`. Proven in
`texture_tiling.jank`:

- `(cpp/GenMeshCube (cpp/float 1.0) ...)` returns a `Mesh` by value;
  passing it inline to `cpp/LoadModelFromMesh` yields a `Model` that binds
  as an outer-let local (pointers inside and all) and draws with
  `cpp/DrawModel` inside the loop. Proof line:
  `VAO: [ID 2] Mesh uploaded successfully to VRAM (GPU)`.
- **Material field writes need a pointer shim.** The C idiom
  `model.materials[0].maps[MATERIAL_MAP_DIFFUSE].texture = tex` (and
  `.shader = s`) has no jank spelling; a two-line C shim taking
  `(Model* m, Texture2D tex)` does the assignment, called with
  `(cpp/& model)` — the same address-of pattern as `Image*` /
  `UpdateCamera`.
- `UnloadModel` after the loop, as usual for create-once resources.
- **`LoadModel` from a FILE works too** (`model_loading.jank`, the castle
  OBJ + its .png diffuse): same shape, just returns a `Model`. Reading
  `model.meshes[0]` for `GetMeshBoundingBox` stays a one-line shim through
  `(cpp/& model)` (a struct-array `cpp/aget` would probably also work per
  the GlyphInfo probe, but the shim is certain).
- **GLB works too** (`cel_shading.jank`, the old_car_new.glb toon car):
  `MODEL: ... Model basic data (glb) loaded successfully`. Reading a
  Shader back off the material (`m->materials[0].shader`) is a
  Shader-returning shim bound to a let-local, like any create-once
  native.
- **Animations, deep Mesh edits and bare Materials work too**
  (2026-07-11): `LoadModelAnimations`' `ModelAnimation*` + `int*`
  out-param stay behind C statics with index-based wrappers, and
  `UpdateModelAnimation` runs per frame (`shadowmap_rendering.jank`);
  a `texcoords2` channel can be RL_MALLOC'd, filled and wired to a
  vertex attribute through `(cpp/& mesh)` (`lightmap_rendering.jank`);
  `LoadMaterialDefault` binds by value with `(cpp/& material)`
  field-write shims (`mesh_instancing.jank`, which also proves
  `DrawMeshInstanced` over a C-static Matrix array).
- **Every model format is proven** (2026-07-11): OBJ (`model_loading`),
  GLB (`cel_shading`), IQM incl. separate animation files
  (`loading_iqm`), M3D incl. skeleton access (`loading_m3d`), and VOX
  (`loading_vox`, which also proves `UpdateCameraPro` with inline
  movement/rotation Vector3s and `GetModelBoundingBox`).

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

## Compute shaders work (GL 4.3 wrapper build, 2026-07-11)

`rlgl_compute.jank` proves the whole compute pipeline: compile
(`rlLoadShader src RL_COMPUTE_SHADER` + `rlLoadShaderProgramCompute`,
kept in a path-taking C shim), SSBOs (`rlLoadShaderBuffer` with
`cpp/nullptr` data), `rlBindShaderBuffer`, and
`rlComputeShaderDispatch` — all direct rlgl calls with jank-int ids.
Prerequisites and patterns:

- **jank-raylib-sys must be built with `OPENGL_VERSION "4.3"`** (set in
  its jank-build.bb since 2026-07-11): under the default 3.3 the rlgl
  compute functions compile to no-ops. 4.3 is the same GL 3.3 feature
  set plus compute, so the GLSL-330 examples are unaffected (regression
  checked). Proof line: `GL: Compute shaders supported`.
- **SSBO ids are plain unsigned ints** — hold them as jank ints and the
  classic ping-pong buffer swap (`ssboA <-> ssboB`) is just `recur`
  with the loop vars exchanged. No native value crosses the loop.
- A CPU-side staging struct uploaded with `rlUpdateShaderBuffer`
  (`&struct` + sizeof) stays a `cpp/raw` static behind
  buffer/count/flush wrappers, like any frame-crossing native state.

## Shared C helpers ship as wrapper headers (from the rlights arc, 2026-07-11)

When several examples need the same C helper (rlights.h's Light array,
the per-type uniform setters), per-file `cpp/raw` duplication is not the
only option: the `-sys` wrapper can SHIP a header and emit a second
`jank-build::include-dir=` directive pointing at it. Proven with
`jank-raylib-sys/include/jank_rlights.h` + `basic_lighting.jank`, which
has no `cpp/raw` block at all — just
`(:include "raylib.h" "jank_rlights.h")`.

Design constraints for such headers:

- **jank fns can neither take nor return native values**, so a "shared
  jank namespace" wrapping lights is impossible; the reusable unit is a
  C header whose state (the `Light` array) is module-local and whose
  API is index-based with scalar parameters
  (`jank_rl_create_light(type, px, py, pz, ..., shader) -> int`).
- Mark **everything `static`** (functions AND state): each including
  module gets a private copy, so two modules in one binary never
  collide at link time.
- Wrapper packaging: add the dir to `:verbatim-paths` in the wrapper's
  project.clj, emit the directive from jank-build.bb off
  `(:src-dir *input*)` (NOT the cmake out-dir), and `bb install`.
- **Cache gotcha (cost one debug cycle):** consumer projects cache the
  emitted directives in `target/_cache/...-out-.../jank-build-cache.txt`
  and do NOT re-run jank-build.bb just because the artifact changed.
  After editing a wrapper's jank-build.bb or headers: `bb install`,
  then delete the consumer's `target/_cache/` (this forces a full
  raylib rebuild, ~2-4 min). Symptom of staleness:
  `fatal error: 'jank_rlights.h' file not found` even though the new
  jar extracted.

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

## Filling the missing JVM surface

No `Math/*`, `format`, `rand-int`, char literals, or `String` methods. The
replacements, all proven in committed examples:

| JVM habit | jank replacement | Proof |
|---|---|---|
| `Math/sin` etc. | `(:include "math.h")` + `cpp/sin`, `cpp/cos`, `cpp/atan2`, `cpp/sqrt`, `cpp/hypot`, `cpp/pow`, `cpp/ceil`, `cpp/floor`, `cpp/trunc`, `cpp/exp`, `cpp/log` (all double) | throughout |
| `Math/PI` | `(def PI 3.141592653589793)` | `easings_testbed.jank` |
| `rand-int` | `cpp/GetRandomValue` | `camera_2d.jank` |
| `(format "%08d" n)` | a zero-pad `str` loop | `format_text.jank` |
| `(format "%.2f" x)` | round ×100, split with `quot`/`mod` | `format_text.jank` `fmt2` |
| char literals / `(char c)` | `subs` into an ASCII table string: chars 32..126 in order, `(subs ASCII (- c 32) (- c 31))` | `input_box.jank` |
| `TextSubtext` | `subs` with the end clamped to `count` | `writing_anim.jank` |
| string as tokens | vector of one-char strings via a `subs` loop | `penrose_tile.jank` |

Typed input: `(int (cpp/GetCharPressed))` in an inner loop until 0
(`input_box.jank`). System time: `(cpp/time cpp/nullptr)`, `(cpp/& t)`,
`(cpp/localtime ...)` + `.-tm_*` fields (`digital_clock.jank`).

**What IS available: the full clojure.core seq API and `clojure.string`.**
The examples in this repo lean on index-based `loop`/`recur` + `nth`/`count`,
which can read as if the higher-level collection API is missing. It is not.
jank's `clojure/core.jank` defines and self-uses `first`, `rest`, `next`,
`seq`, `empty?`, `second`, `map`, `filter`, `reduce`, `into`, `concat`,
`some`, `every?`, `mapv`, `range`, `repeat`, `partition`, `doseq`, `dotimes`,
`when-let`/`if-let`, etc. — the ordinary Clojure surface. `clojure.string`
ships too (`split`, `split-lines`, `join`, `includes?`, `index-of`,
`trim`, `upper-case`/`lower-case`, ...), backed by native C++.

**Caveat: not every clojure.string / clojure.core fn is implemented yet.**
The var exists (it's declared in `string.jank` / `core.jank`) but some native
backers are stubs that throw at runtime — `str/replace` currently dies with
`TODO: port clojure.string/replace` (hit in `rectangle_bounds.jank`, worked
around by baking the substitution into the source string), and core's
`flush` dies with `TODO: port flesh` (sic; hit probing `compute_hash.jank` —
stdout is block-buffered when redirected, so `println` output can vanish if
the process is killed; there is no working in-jank flush, shim
`fflush(stdout)` via `cpp/raw` if a probe needs it). So a function being
present in the source is not proof it runs; if in doubt, probe it, or grep
its native impl for `TODO`. `split`/`split-lines`/`join` are confirmed
working, as are `peek`/`pop`/`filterv`/`into` (`rectangle_bounds.jank`).
Pull it in the normal way — `:require` coexists with a C++ `:include` in one
`ns` form (jank's own `shell.jank` does exactly this):

```clojure
(ns raylib-examples.foo
  (:require [clojure.string :as str])
  (:include "raylib.h"))
;; then (str/split-lines text), (str/join " " xs), (first coll), etc.
```

Index-based loops are still fine (and sometimes clearer for tight draw
loops), but reach for the seq API / `clojure.string` when it reads better.
`text_file_loading.jank` is the proof in this repo: it `(:require
[clojure.string :as str])` beside `(:include "raylib.h")` and word-wraps
with `str/split-lines`, `str/split line #"\s+"` (regex literals work) and
`filterv` — compiled and ran clean.
Source of truth: `~/dev/jank/compiler+runtime/src/jank/clojure/{core,string}.jank`.

**`const char *` returns fold into `str` directly** (from the core arc,
2026-07-03). A raylib fn that returns a C string (`GetMonitorName`,
`GetClipboardText`, `GetWindowTitle`, ...) can be passed straight to jank's
`str`, which turns it into a jank string:

```clojure
(str "[" (cpp/GetMonitorName 0) "]")   ; => "[Built-in Retina Display]"
```

Proven in `monitor_detector.jank`. No conversion helper needed — the native
`const char *` becomes a jank string at the `str` boundary. (You can also pass
it straight to another C fn that wants `const char *`, e.g.
`(cpp/DrawText (cpp/GetMonitorName 0) ...)`, since that's C->C.)

**GOTCHA: don't wrap an already-boxed jank int in `(int x)` INSIDE a `str`
call.** `monitor_detector` cost real debugging over this. When `x` is already a
jank int (e.g. destructured from a map), writing `(str "Position: " (int x))`
made jank emit C++ that member-accesses an `i64`, and the WHOLE FILE failed to
compile with the misleading `member reference base type 'i64' (aka 'long
long') is not a structure or union` — reported at an unrelated generated line,
with no `.-` in the source at all. Dropping the redundant cast fixed it:

```clojure
;; BAD  — redundant (int x) on an already-jank int inside str -> i64 codegen error
(cpp/DrawText (str "Position: " (int x) " x " (int y)) ...)
;; GOOD — pass the boxed value directly
(cpp/DrawText (str "Position: " x " x " y) ...)
```

The plain `.-x` reads and the `const char *` fold in the same file were both
fine; the cast-inside-`str` was the sole trigger. When a file fails with
`member reference base type 'i64'` and you can't find a matching `.-` access,
suspect an `(int ...)`/cast folded into a `str` (or other builder) call — the
error line is generated-code position, not source, so don't trust it.

## Misc that saves a recompile

- C bools work directly in conditionals: `(cpp/! (cpp/WindowShouldClose))`,
  `(if (cpp/IsKeyDown cpp/KEY_Q) ...)`.
- C constants resolve as `cpp/NAME`: colors, keys, `cpp/MOUSE_CURSOR_IBEAM`,
  `cpp/TEXTURE_FILTER_BILINEAR`, gesture enums (compare as ints:
  `(int (cpp/GetGestureDetected))`, values 1/2/4/.../512 —
  `input_gestures.jank`).
- Flag ORs aren't needed: `SetConfigFlags` ORs each call into its state, so
  call once per flag (`window_letterbox.jank`).
- When camera rotation is 0, skip `GetWorldToScreen2D`/`GetScreenToWorld2D`
  (native Vector2 returns) — the transforms reduce to
  `screen = (world - target)*zoom + offset` in jank math
  (`camera_2d_platformer.jank` does all five camera modes this way).
  When rotation matters, both `GetScreenToWorld2D` and `GetWorldToScreen`
  (3D) DO work — bind the returned native Vector2 to a local and read
  `.-x`/`.-y` (`camera_2d_mouse_zoom.jank`, `world_screen.jank`).
- **A jank fn takes at most 10 parameters** (`analyze/invalid-fn-parameters:
  This function has too many parameters. The max is 10`). Bundle extra args
  into a vector and destructure inside — `tiled_drawing.jank`'s tiling helper
  passed source/dest as two 4-vectors instead of eight scalars. (Moot there
  in the end, since the native-`Texture2D`-param rule forced full inlining,
  but the cap is real and independent.)
- A side-effecting draw-helper `defn` shared by several passes should end
  with an explicit `nil` (`camera_2d_split_screen.jank`'s `draw-scene`).
- `\n` inside a `DrawText` string works (`window_letterbox.jank`).
- Multi-header include: `(:include "raylib.h" "math.h" "time.h")`.

## rlgl and textures (from the shapes-completing arc)

- **rlgl immediate mode works directly** — `(:include "raylib.h" "rlgl.h")`
  gives `rlBegin`/`rlColor4ub`/`rlColor4f`/`rlVertex2f`/`rlEnd`
  (`rlgl_triangle.jank`), custom blend pipelines via `rlSetBlendFactors` +
  `rlSetBlendMode cpp/BLEND_CUSTOM` + `rlDrawRenderBatchActive`
  (`top_down_lights.jank`), and full vertex-colored batches
  (`rectangle_advanced.jank`). Raw GL constants pass as plain ints.
- **The rlgl matrix stack works in 3D**: `rlPushMatrix`/`rlPopMatrix`/
  `rlRotatef`/`rlTranslatef`/`rlScalef` nest hierarchical transforms
  inside `BeginMode3D`, and regular raylib draws (`DrawSphere`) render
  through the same batch so the stack applies to them
  (`rlgl_solar_system.jank` - Sun/Earth/Moon).
- **Color field read-back works**: bind a returned Color to a let-local and
  read `(.-r c)`/`(.-g c)`/`(.-b c)`/`(.-a c)` — feed them native-to-native
  into `rlColor4ub`, or box with `(int (+ 0.0 ...))` to store as jank ints
  (`rlgl_color_wheel.jank`).
- **Image → Texture loading works**: `(cpp/GenImageChecked ...)` →
  `(cpp/LoadTextureFromImage img)` → `(cpp/UnloadImage img)`, with the
  Texture2D held in the outer `let` like a RenderTexture
  (`top_down_lights.jank`). All nine `GenImage*` algorithms work
  (`image_generation.jank`).
- **`LoadTexture` from a PNG file works** — a jank string coerces to the
  `const char*` path. Resource files come from the vendored raylib
  submodule via a path relative to the `raylib-examples/` working dir:
  `../jank-raylib-sys/raylib/examples/textures/resources/...`
  (`logo_texture.jank`; the run log's `FILEIO: ... File loaded
  successfully` is the proof to grep for).
- **N textures = N outer-let bindings + a nested-if dispatch** on the
  current index — the array-of-textures idiom has no direct jank shape
  (`image_generation.jank`'s nine).
- **Per-entity RenderTexture caches don't map to jank** — native handles
  can't live in a jank vector. Restructure to one reused scratch RT plus a
  rebuild-on-dirty pass (`top_down_lights.jank` replaces the C's 16 cached
  per-light masks this way).
- **Variable-winding fans**: when a quad's winding depends on runtime
  geometry (shadow volumes), draw each triangle in BOTH windings or
  backface culling eats half of them (`top_down_lights.jank`'s draw-quad).

## Audio (from the textures arc, 2026-07-03)

**raylib audio works with zero wrapper changes.** `InitAudioDevice`,
`LoadSound`, `PlaySound`, `UnloadSound` and `CloseAudioDevice` are plain
`raylib.h` functions compiled into `libraylib` (miniaudio / Core Audio
backend on macOS). The `Sound` value is a native struct — same rules as
`Texture2D`: bind it in the outer `let`, use it inside the frame loop via
lexical capture, unload after (`sprite_button.jank`). The run log's
`AUDIO: Device initialized successfully` + `WAVE: Data loaded
successfully` are the proof lines to grep for. OGG decoding works too
(`sound_loading.jank`), and so do music streams: `LoadMusicStream` (MP3),
`UpdateMusicStream` once per frame, `Play`/`Stop`/`Pause`/`Resume`,
`SetMusicPan`/`SetMusicVolume` (jank real through `cpp/float`), and
`GetMusicTimePlayed`/`GetMusicTimeLength` boxed at the binding site
(`music_stream.jank`; proof line `STREAM: Initialized successfully`).
The remaining audio surface to probe is the callback-taking APIs
(`SetAudioStreamCallback`, audio processors) — likely a real blocker,
same class as C function pointers elsewhere.

## 3D mode (from the sound-positioning port, 2026-07-03)

**Basic 3D works.** `cpp/Camera3D` constructs inline from three nested
`cpp/Vector3` args + fovy + `cpp/CAMERA_PERSPECTIVE`, binds as an
outer-let local, and drives `BeginMode3D`/`EndMode3D`; `DrawGrid` and
`DrawSphere` (Vector3 built inline as a call arg) render inside it
(`sound_positioning.jank`). Two constraints shape 3D ports:

- **Free-look cameras WORK now** (2026-07-05): `UpdateCamera
  ((cpp/& camera) mode)` forms the pointer with the image-processing
  address-of pattern on an OUTER-let Camera3D, and the mutation
  persists across frames (probe: 100 orbital frames drifted position.x
  from 10.0 to 14.03) — `camera_3d_free.jank`. Struct FIELD writes
  still have no jank syntax; a one-line pointer shim does them
  (`jank_cam_retarget`). The older per-frame-rebuild workaround remains
  valid and simpler when the camera path is fully jank-driven:
  `billboard_rendering.jank` constructs the whole `cpp/Camera3D` as a
  frame-let local from an accumulated angle, and `DrawBillboard*`
  accept it by value. `BoundingBox` also constructs inline from two nested
  `cpp/Vector3`s for `CheckCollisionBoxes` / `CheckCollisionBoxSphere`
  (`box_collisions.jank`).
- **No raymath vector helpers through jank fns**: `Vector3Subtract` etc.
  return native structs, fine inline, but a chain of them can't thread
  jank helper fns — do the vector math as scalar jank arithmetic on
  plain reals instead (the attenuation/pan math in
  `sound_positioning.jank` replaces five raymath calls this way).

## Compile-time cost of deeply nested loops

A triple-nested `doseq` with a fat body (`waving_cubes.jank`'s
15x15x15 cube lattice) compiles in ~3-4 MINUTES, versus ~30-60s for a
typical example module. The generated C++ for nested seq iteration with
a large inlined body appears to grow multiplicatively. Budget smoke-test
alarms accordingly (the standard 40s alarm kills such a build
mid-compile and looks like a hang - re-run with a 260s+ alarm before
diagnosing). If compile time matters more than faithfulness, hoist the
inner body into a `defn` taking only jank values.

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

The wider jank cpp-interop toolbox (documented at
`~/dev/opengl-with-jank/CPP_INTEROP_DOCUMENTATION.md`, proven in that engine):

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
same way an all-native `f64` chain does (rule 4).

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
return reals (rule 3), which the shim's `int offset` param rejects at runtime
(`expected integer found small_real`). Wrap the frame advance:
`(int (mod (+ cur 1) nframes))`.

Lifecycle caveat (rule-2 interaction): a mutated `Image` is still a **native
value**, so it can't be carried in `loop`/`recur` state. Keep the whole
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
