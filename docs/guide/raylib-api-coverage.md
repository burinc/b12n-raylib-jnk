# raylib API coverage

What raylib surface area is proven working against this port, and how.
Each section cites the committed example that proves it — these examples
double as the running test suite for the claims below.

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
  `UpdateCamera` ([cpp-interop-toolbox.md](cpp-interop-toolbox.md)).
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

  > **Added in this repo:** this project's own default was later changed
  > to `OPENGL_VERSION "3.3"` (macOS's native GL backend caps at 4.1, so a
  > global 4.3 build broke window creation for every example on macOS).
  > `rlgl-compute` now needs the manual `4.3` override described above,
  > which will not work on macOS regardless of the override — see the root
  > README's "Known limitations" section.
- **SSBO ids are plain unsigned ints** — hold them as jank ints and the
  classic ping-pong buffer swap (`ssboA <-> ssboB`) is just `recur`
  with the loop vars exchanged. No native value crosses the loop.
- A CPU-side staging struct uploaded with `rlUpdateShaderBuffer`
  (`&struct` + sizeof) stays a `cpp/raw` static behind
  buffer/count/flush wrappers, like any frame-crossing native state.

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
  address-of pattern ([cpp-interop-toolbox.md](cpp-interop-toolbox.md))
  on an OUTER-let Camera3D, and the mutation
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
