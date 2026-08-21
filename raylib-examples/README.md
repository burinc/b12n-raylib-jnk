# raylib-examples (jank)

[![examples ported](https://img.shields.io/badge/examples_ported-209%2F217-brightgreen)](#porting-progress)
[![categories complete](https://img.shields.io/badge/complete-shapes,_shaders,_audio,_text-brightgreen)](#porting-progress)

Official [raylib](https://www.raylib.com/examples.html) examples ported to
**jank** (native Clojure). Each example is one `.jank` namespace under
`src/raylib_examples/`; a Leiningen profile picks which `-main` runs.

```sh
# from the repo root (Babashka, easiest):
bb info                # grouped cheat-sheet (examples by raylib category)
bb examples            # list every runnable example
bb <name>              # run one, e.g.  bb following-eyes
bb run-all [secs]      # cycle through all of them (demo reel)

# or with lein directly, from this directory:
lein with-profile +<name> run --disable-sandbox
```

Ports follow the **definitive C sources** in
[`raysan5/raylib`](https://github.com/raysan5/raylib) (`examples/<category>/`),
not any intermediate binding. For the jank/C++ interop constraints that shape
these ports, see
[`docs/guide/native-value-lifetimes.md`](../docs/guide/native-value-lifetimes.md).
One deliberate deviation: every example sets `FLAG_WINDOW_HIGHDPI`, so
windows scale with your monitor's DPI
(e.g. 2x on a 192-DPI HiDPI display) while the drawing stays at the C's
logical 800x450.

## Porting progress

**209 / 217** official examples ported so far. The `shapes` (41/41),
`shaders` (35/35), `audio` (11/11) and `text` (16/16) categories are
**complete**, and `models` is at 29/30
(the last one needs a raylib recompile - see below), plus `core` (46),
`textures` (31), 3D mode opened via `sound-positioning`,
the rlgl matrix stack in 3D (`rlgl-solar-system`), and font loading
(`font-loading`).

| Category | Ported | Total |
|---|---:|---:|
| shapes   | 41 | 41 |
| core     | 46 | 49 |
| text     | 16 | 16 |
| textures | 31 | 32 |
| shaders  | 35 | 35 |
| models   | 29 | 30 |
| audio    | 11 | 11 |
| others   |  0 |  3 |

## Ported ✅

| `bb` name | Official source | What it shows |
|---|---|---|
| `bouncing-ball`      | `shapes/shapes_bouncing_ball`      | a ball bouncing with optional gravity |
| `input-keys`         | `core/core_input_keys`             | steer a ball with the arrow keys |
| `input-mouse`        | `core/core_input_mouse`            | a ball tracks the cursor; clicks recolor it |
| `colors-palette`     | `shapes/shapes_colors_palette`     | every named raylib color in a grid |
| `starfield`          | `shapes/shapes_starfield_effect`   | a perspective starfield flying toward you |
| `mouse-trail`        | `shapes/shapes_mouse_trail`        | a fading trail following the cursor |
| `logo-anim`          | `shapes/shapes_logo_raylib_anim`   | the raylib logo assembling itself |
| `double-pendulum`    | `shapes/shapes_double_pendulum`    | chaotic double-pendulum motion + trail |
| `particles`          | `shapes/shapes_simple_particles`   | water / smoke / fire particle effects |
| `collision-area`     | `shapes/shapes_collision_area`     | AABB collision between two boxes |
| `ball-physics`       | `shapes/shapes_ball_physics`       | grab and throw balls under gravity |
| `easings-rectangles` | `shapes/shapes_easings_rectangles` | a grid shrinks and spins via easing functions |
| `following-eyes`     | `shapes/shapes_following_eyes`     | two eyes whose irises track the cursor |
| `lines-bezier`       | `shapes/shapes_lines_bezier`       | a cubic Bezier curve reshaped by dragging its ends |
| `rectangle-scaling`  | `shapes/shapes_rectangle_scaling`  | resize a rectangle by its bottom-right corner |
| `dashed-line`        | `shapes/shapes_dashed_line`        | a dashed line to the mouse, adjustable dash/gap/color |
| `basic-shapes`       | `shapes/shapes_basic_shapes`       | a gallery of the basic shapes, with spinning hexagons |
| `logo-raylib`        | `shapes/shapes_logo_raylib`        | the raylib logo drawn from rectangles + text |
| `easings-ball`       | `shapes/shapes_easings_ball`       | a ball animated through elastic + cubic easing stages |
| `easings-box`        | `shapes/shapes_easings_box`        | a box animated through five easing stages |
| `math-angle-rotation`| `shapes/shapes_math_angle_rotation`| fixed-angle lines plus a line spinning through every angle |
| `ellipse-collision`  | `shapes/shapes_ellipse_collision`  | overlap test between two ellipses, one mouse-controlled |
| `vector-angle`       | `shapes/shapes_vector_angle`       | two ways to measure an angle (between-vectors / from-horizontal) |
| `penrose-tile`       | `shapes/shapes_penrose_tile`       | a Penrose tiling grown with an L-system, drawn with a turtle |
| `input-mouse-wheel`  | `core/core_input_mouse_wheel`      | scroll a box up and down with the mouse wheel |
| `random-values`      | `core/core_random_values`          | a new random value shown every two seconds |
| `camera-2d`          | `core/core_2d_camera`              | a free 2D camera (pan/zoom/rotate) over a skyline |
| `basic-window`       | `core/core_basic_window`           | the minimal raylib window with a line of text |
| `scissor-test`       | `core/core_scissor_test`           | a scissor rectangle that reveals text under the mouse |
| `window-should-close`| `core/core_window_should_close`    | a confirm-before-exit dialog on window close |
| `digital-clock`      | `shapes/shapes_digital_clock`      | a live system clock in digital (7-segment) and analogue modes |
| `clock-of-clocks`    | `shapes/shapes_clock_of_clocks`    | time digits drawn from 4x6 grids of little clocks whose hands sweep |
| `delta-time`         | `core/core_delta_time`             | delta-time vs per-frame movement, with an adjustable FPS cap |
| `basic-screen-manager`| `core/core_basic_screen_manager`  | a LOGO -> TITLE -> GAMEPLAY -> ENDING screen state machine |
| `lines-drawing`      | `shapes/shapes_lines_drawing`      | a paint canvas that persists strokes on an offscreen RenderTexture |
| `easings-testbed`    | `shapes/shapes_easings_testbed`    | an interactive testbed animating a ball through all 28 easing functions |
| `camera-2d-platformer`| `core/core_2d_camera_platformer`  | a tiny platformer with five switchable camera-follow modes |
| `input-gestures`     | `core/core_input_gestures`         | a log of detected mouse/touch gestures with a circle at the touch point |
| `window-letterbox`   | `core/core_window_letterbox`       | a fixed 640x480 game scaled + letterboxed into a resizable window |
| `camera-2d-split-screen`| `core/core_2d_camera_split_screen` | two players roaming one grid world, each with a camera in half the screen |
| `smooth-pixelperfect`| `core/core_smooth_pixelperfect`    | spinning rectangles in a 160x90 world upscaled with sub-pixel smoothing |
| `format-text`        | `text/text_format_text`            | zero-padded score/hiscore/lives readouts plus live frame time |
| `writing-anim`       | `text/text_writing_anim`           | a message that types itself out one character at a time |
| `input-box`          | `text/text_input_box`              | a text input box with a blinking caret and an I-beam cursor |
| `words-alignment`    | `text/text_words_alignment`        | a cycling word aligned 9 ways inside a rectangle (MeasureTextEx) |
| `bullet-hell`        | `shapes/shapes_bullet_hell`        | a magic circle spraying bullet spirals, with a draw-method toggle |
| `ring-drawing`       | `shapes/shapes_ring_drawing`       | a ring/annulus with adjustable angles, radii and segments |
| `circle-sector-drawing`| `shapes/shapes_circle_sector_drawing` | a filled circle sector + outline with adjustable angles |
| `rounded-rectangle`  | `shapes/shapes_rounded_rectangle_drawing` | a rounded rectangle with live size/roundness/outline knobs |
| `recursive-tree`     | `shapes/shapes_recursive_tree`     | a binary fractal tree rebuilt every frame from live knobs |
| `triangle-strip`     | `shapes/shapes_triangle_strip`     | a rainbow fan of triangles between two radii |
| `math-sine-cosine`   | `shapes/shapes_math_sine_cosine`   | a live unit-circle visualization of sine/cosine/tangent/cotangent |
| `hilbert-curve`      | `shapes/shapes_hilbert_curve`      | a rainbow Hilbert space-filling curve, animated stroke by stroke |
| `pie-chart`          | `shapes/shapes_pie_chart`          | an interactive pie/donut chart whose hovered slice pops out |
| `kaleidoscope`       | `shapes/shapes_kaleidoscope`       | strokes mirrored with 6-fold symmetry, with drawing history |
| `splines-drawing`    | `shapes/shapes_splines_drawing`    | draggable spline points through linear/b-spline/catmull-rom/bezier |
| `rlgl-triangle`      | `shapes/shapes_rlgl_triangle`      | a rainbow triangle drawn with the low-level rlgl immediate mode |
| `rlgl-color-wheel`   | `shapes/shapes_rlgl_color_wheel`   | an HSV color-picker wheel rendered with rlgl, with hex readout |
| `top-down-lights`    | `shapes/shapes_top_down_lights`    | 2D lights casting shadow volumes off boxes via blended alpha masks |
| `rectangle-advanced` | `shapes/shapes_rectangle_advanced` | rounded rectangles with horizontal gradients, triangle by triangle in rlgl |
| `image-generation`   | `textures/textures_image_generation` | nine procedural textures: gradients, checked, white/perlin noise, cellular |
| `logo-texture`       | `textures/textures_logo_raylib`    | the raylib logo loaded from a PNG file and drawn centered |
| `sprite-animation`   | `textures/textures_sprite_animation` | scarfy running: one frame of a 6-frame spritesheet at a time |
| `srcrec-dstrec`      | `textures/textures_srcrec_dstrec`  | a scarfy frame rotated + scaled 2x with DrawTexturePro |
| `background-scrolling`| `textures/textures_background_scrolling` | parallax-scrolling cyberpunk street layers at three speeds |
| `image-loading`      | `textures/textures_image_loading`  | the two-step LoadImage (RAM) -> LoadTextureFromImage (VRAM) path |
| `blend-modes`        | `textures/textures_blend_modes`    | the foreground blended over the background in four blend modes |
| `particles-blending` | `textures/textures_particles_blending` | spark particles trailing the mouse in alpha/additive blending |
| `mouse-painting`     | `textures/textures_mouse_painting` | a paint program on a RenderTexture canvas with a 23-color palette |
| `sprite-button`      | `textures/textures_sprite_button`  | a 3-state sprite button that plays a click sound (first audio use) |
| `sound-loading`      | `audio/audio_sound_loading`        | a WAV and an OGG played on demand (the first audio-category port) |
| `bunnymark`          | `textures/textures_bunnymark`      | the classic bunny-spawning batching benchmark |
| `music-stream`       | `audio/audio_music_stream`         | an MP3 streamed with live pan/volume/progress controls |
| `module-playing`     | `audio/audio_module_playing`       | a chiptune XM tracker module with pulsing circle waves |
| `sound-multi`        | `audio/audio_sound_multi`          | overlapping playback of one WAV through nine sound aliases |
| `fog-of-war`         | `textures/textures_fog_of_war`     | a tile map hidden by fog, revealed around the player |
| `sound-positioning`  | `audio/audio_sound_positioning`    | spatial audio from a sphere orbiting in 3D (first BeginMode3D) |
| `geometric-shapes`   | `models/models_geometric_shapes`   | 3D cubes, spheres, cylinders and capsules on a grid (first models port) |
| `box-collisions`     | `models/models_box_collisions`     | a player cube turning red against 3D box/sphere obstacles |
| `billboard-rendering`| `models/models_billboard_rendering`| camera-facing billboards while the camera orbits (per-frame Camera3D) |
| `waving-cubes`       | `models/models_waving_cubes`       | a 15x15x15 lattice of rainbow cubes waving while the camera circles |
| `orthographic-projection` | `models/models_orthographic_projection` | the 3D shape gallery through perspective vs orthographic cameras |
| `tesseract-view`     | `models/models_tesseract_view`     | a rotating 4D hypercube projected down to 3D |
| `rlgl-solar-system`  | `models/models_rlgl_solar_system`  | Sun/Earth/Moon nested on the rlgl matrix stack |
| `camera-2d-mouse-zoom` | `core/core_2d_camera_mouse_zoom` | pan and zoom-to-cursor a 2D camera (GetScreenToWorld2D + rlgl) |
| `world-screen`       | `core/core_world_screen`           | a 2D label pinned above a 3D cube via GetWorldToScreen |
| `tiled-drawing`      | `textures/textures_tiled_drawing`  | tile a texture pattern across a panel (reimplemented DrawTextureTiled) |
| `font-loading`       | `text/text_font_loading`           | a BMFont and a TTF font loaded and drawn with DrawTextEx |
| `font-filters`       | `text/text_font_filters`           | scale a TTF word and switch its atlas texture filter |
| `font-spritefont`    | `text/text_font_spritefont`        | three colored sprite fonts loaded from PNG atlases |
| `sprite-fonts`       | `text/text_sprite_fonts`           | a gallery of raylib's eight bundled sprite fonts |
| `camera-3d`          | `core/core_3d_camera_mode`         | a red cube on a grid viewed through a fixed 3D perspective camera |
| `picking-3d`         | `core/core_3d_picking`             | click a 3D box to pick it with a GetScreenToWorldRay / GetRayCollisionBox ray |
| `sprite-explosion`   | `textures/textures_sprite_explosion` | click to play a 5x5 explosion spritesheet (DrawTextureRec) with a boom sound |
| `input-multitouch`   | `core/core_input_multitouch`       | an orange ball at every active touch point (mouse = point 0 on desktop) |
| `sprite-stacking`    | `textures/textures_sprite_stacking` | a voxel-style booth built from 122 stacked rotated spritesheet slices (DrawTexturePro) |
| `npatch-drawing`     | `textures/textures_npatch_drawing` | stretchable 9-patch and 3-patch UI panels via DrawTextureNPatch (first NPatchInfo) |
| `input-virtual-controls` | `core/core_input_virtual_controls` | an on-screen D-pad that moves a player circle (touch or mouse-down) |
| `image-processing`   | `textures/textures_image_processing` | nine CPU image filters via pointer-taking Image* APIs (first `cpp/&` use) |
| `image-drawing`      | `textures/textures_image_drawing`  | one texture composed from several CPU images (crop/flip/resize/ImageDraw/text) |
| `image-text`         | `textures/textures_image_text`     | text baked into an image with a TTF font via ImageDrawTextEx |
| `image-rotate`       | `textures/textures_image_rotate`   | the raylib logo rotated +45/+90/-90 degrees in CPU memory (ImageRotate) |
| `image-channel`      | `textures/textures_image_channel`  | the fudesumi sprite split into R/G/B/A channels, alpha-masked |
| `image-kernel`       | `textures/textures_image_kernel`   | cat put through sharpen / Sobel / Gaussian convolution kernels (cpp/raw float[] kernels) |
| `cellular-automata`  | `textures/textures_cellular_automata` | a Wolfram elementary cellular automaton with a click-editable rule + presets |
| `magnifying-glass`   | `textures/textures_magnifying_glass` | a circular magnifier revealing hidden bunnies (RenderTexture + rlgl custom separate blend) |
| `to-image`           | `textures/textures_to_image`       | round-trips the logo VRAM<->RAM via LoadImageFromTexture |
| `polygon-drawing`    | `textures/textures_polygon_drawing` | a cat texture mapped onto a spinning polygon (DrawTexturePoly reimplemented in rlgl) |
| `raw-data`           | `textures/textures_raw_data`       | a texture from a headerless .raw pixel dump (LoadImageRaw) + a code-generated checkerboard |
| `textured-curve`     | `textures/textures_textured_curve` | a road texture swept along a draggable cubic Bezier spline (rlgl RL_QUADS strip) |
| `gif-player`         | `textures/textures_gif_player`     | an animated GIF streamed frame-by-frame to a texture (LoadImageAnim int* out-param + image.data pointer arithmetic via cpp/raw shim) |
| `window-flags`       | `core/core_window_flags`           | toggle window state flags (resizable, undecorated, topmost, ...) live with a bouncing ball |
| `render-texture`     | `core/core_render_texture`         | a ball bouncing inside a 300x300 off-screen RenderTexture, drawn back rotated + y-flipped via DrawTexturePro |
| `monitor-detector`   | `core/core_monitor_detector`       | a scaled map of every attached monitor + name/resolution/refresh/physical-size (GetMonitorName const char* -> jank str) |
| `input-actions`      | `core/core_input_actions`          | remappable abstract actions (key + gamepad button) via a swappable jank keyset map instead of a native ActionInput[] |
| `highdpi-demo`       | `core/core_highdpi_demo`           | logical-points vs physical-pixels grids + live DPI scale (GetWindowScaleDPI + GetRenderWidth) |
| `highdpi-testbed`    | `core/core_highdpi_testbed`        | a HighDPI diagnostic overlay: labelled grid, monitor/window/screen/render/DPI info, mouse crosshair |
| `textured-cube`      | `models/models_textured_cube`      | two 3D cubes textured from a shared atlas via rlgl immediate mode (rlVertex3f/rlTexCoord2f), whole-texture + source-rect |
| `directional-billboard` | `models/models_directional_billboard` | a sprite-sheet billboard whose facing row is picked from the orbit angle (inline orbital camera; DrawBillboardPro) |
| `random-sequence`   | `core/core_random_sequence`        | colored bars whose heights are a no-repeat permutation from LoadRandomSequence (int* indexed via a cpp/raw shim) |
| `basic-voxel`       | `models/models_basic_voxel`        | an 8x8x8 beige voxel grid; left-click ray-picks and removes the nearest cube (orbital camera; flat-vector grid; DrawCube) |
| `rotating-cube`     | `models/models_rotating_cube`      | a textured cube spinning on a tilted axis, drawn with rlgl immediate mode + rotated on the rlgl matrix stack (no Model load) |
| `clipboard-text`    | `core/core_clipboard_text`         | type + cut/copy/paste against the system clipboard via Get/SetClipboardText (raygui buttons -> CTRL shortcuts) |
| `undo-redo`         | `core/core_undo_redo`              | a grid player with a 26-slot undo/redo ring buffer + buffer viz; the C's memcpy'd struct array becomes a jank vector of maps (memcmp -> =) |
| `directory-files`   | `core/core_directory_files`        | a keyboard file browser; the native FilePathList (char** paths) is snapshotted into a jank vector of maps each load (char** indexed via a cpp/raw shim) |
| `custom-logging`    | `core/core_custom_logging`         | a custom trace-log callback timestamps + tags every raylib log line; `SetTraceLogCallback` gets a fn pointer to a `void(int,const char*,va_list)` defined in cpp/raw (the audio-callback pattern), installed before InitWindow |
| `drop-files`        | `core/core_drop_files`             | drag files onto the window to accumulate + list their paths; LoadDroppedFiles' char** is snapshotted into a growing jank vector (same cpp/raw shim) |
| `text-file-loading` | `core/core_text_file_loading`      | load a text file, word-wrap each line to the window with MeasureText and scroll through it; the C's in-place char-buffer wrap becomes immutable jank string ops |
| `rectangle-bounds`  | `text/text_rectangle_bounds`       | word-wrapped text in a mouse-resizable container (SPACE toggles word/char wrap); the C's font.glyphs[] advance indexing becomes MeasureTextEx on substrings |
| `compute-hash`      | `core/core_compute_hash`           | CRC32/MD5/SHA1/SHA256 + Base64 of typed text (raygui textbox/button -> live typing + ENTER); the static u32 hash arrays are the repo's first proven cpp/aget reads |
| `storage-values`    | `core/core_storage_values`         | save/load a score pair to a binary storage file; the C's realloc byte-position save stays a cpp/raw shim, the load is jank (LoadFileData + int* out-param) |
| `keyboard-testbed`  | `core/core_keyboard_testbed`       | an on-screen ENG-US keyboard lighting up held keys; the C's keycode/width arrays + GetKeyText switch become one vector of key maps (enum codes boxed via cpp/int) |
| `input-gestures-testbed` | `core/core_input_gestures_testbed` | a gesture dashboard (log, last-gesture panel, protractor); the Color-returning switch becomes a packed-RGBA map, the char[20][12] ring log a jank vector + index |
| `viewport-scaling`  | `core/core_viewport_scaling`       | a fixed game resolution scaled into a resizable window (6 policies); the runtime-recreated RenderTexture lives in a cpp/raw static (new frame-crossing-state pattern) |
| `codepoints-loading` | `text/text_codepoints_loading`     | Japanese text scanned for codepoints, deduped in jank, rasterized to a minimal atlas (int* read via cpp/aget, written back via a setter shim; UTF-8 source literal) |
| `unicode-ranges`    | `text/text_unicode_ranges`         | keys 1-4 grow the font atlas by unicode range; glyph values read via struct-array cpp/aget, font in a cpp/raw static (which must be confined to -main: per-fn duplication) |
| `inline-styling`    | `text/text_inline_styling`         | inline [c..]/[b..]/[r] style tags drawn per glyph (DrawTextCodepoint + glyphs[i].advanceX via aget); the C's duplicated tag scanner becomes one pure-jank tokenizer |
| `unicode-emojis`    | `text/text_unicode_emojis`         | click font-glyph emojis for multilingual speech bubbles; the C's \\0-separated UTF-8 blob becomes a codepoint vector, DrawTextBoxed becomes LoadCodepoints + glyph-advance wrap |
| `text-3d-drawing`   | `text/text_3d_drawing`             | a bitmap font drawn as textured rlgl quads in 3D with a waving `~~World~~`; the whole glyph-quad engine (native Font/Color/WaveTextConfig, rlBegin(RL_QUADS), the SHOW_*_BOUNDRY toggles, drag-drop font swap and live typing) rides in one cpp/raw shim jank drives per-frame |
| `strings-management`| `text/text_strings_management`     | draggable text-particle physics (grab/slice/shatter/glue) showcasing raylib's TextCopy/TextSubtext/TextSplit/TextTo{Upper,Lower,Pascal,Snake,Camel}; the TextParticle[100] pool + seven helpers + the string ops (which return C strings, not jank strings) ride in one cpp/raw shim - proving raylib's whole TextX API is reachable from jank |
| `shapes-textures-shader` | `shaders/shaders_shapes_textures` | a grayscale fragment shader toggled around shapes/sprites; proves LoadShader(nullptr, path) + BeginShaderMode work with zero wrapper changes |
| `texture-outline`   | `shaders/shaders_texture_outline`  | a shader-drawn sprite outline, wheel-resizable; proves shader uniforms (SetShaderValue's void* staged through a cpp/raw static float buffer + setter shim) |
| `camera-3d-free`    | `core/core_3d_camera_free`         | a free-look 3D camera around a cube; proves UpdateCamera((cpp/& camera) mode) mutates the outer-let Camera3D persistently across frames |
| `camera-3d-first-person` | `core/core_3d_camera_first_person` | first-person walk with 4 camera modes + isometric toggle; camera field writes via pointer shims, CameraYaw/Pitch from rcamera.h link directly |
| `texture-waves`     | `shaders/shaders_texture_waves`    | a sine-wave fragment shader over a space texture, animated by a per-frame seconds uniform (7 float uniforms + a vec2 through the staging shim) |
| `camera-3d-split-screen` | `core/core_3d_camera_split_screen` | two players, two half-screen RenderTextures; the C's camera field nudges become jank scalars + per-frame Camera3D rebuilds, the duplicated scene a shared helper |
| `camera-3d-fps`     | `core/core_3d_camera_fps`          | a physics FPS controller (gravity/jump/crouch/strafe-accel/head-bob/lean); the all-native Body struct + Vector2/Vector3 globals + raymath (Vector3RotateByAxisAngle/Lerp/Clamp/...) ride in one cpp/raw shim jank drives per-frame |
| `vr-simulator`      | `core/core_vr_simulator`           | a 3D scene rendered in stereo through a simulated Oculus Rift CV1 + lens-distortion/chroma shader; the native VrDeviceInfo/VrStereoConfig, the config.leftLensCenter/device.lensDistortionValues float-array uniforms, the HMD RenderTexture and first-person Camera all live in one cpp/raw shim (no headset needed) |
| `automation-events` | `core/core_automation_events`      | a 2D platformer with input record/replay; the native Player/EnvElement[5]/Camera2D + the AutomationEventList (whose `.events[]`/`.count` the replay loop reads, PlayAutomationEvent by value) live in a cpp/raw shim - the record→export→replay API path verified end-to-end (writes a valid .rae) |
| `input-gamepad`     | `core/core_input_gamepad`          | a live controller diagram (Xbox/PS/generic) whose buttons/sticks/triggers light up with input; the two controller-photo Texture2Ds + the ~200-line button/axis draw ride in a cpp/raw shim (no controller on this box, so only the NOT-DETECTED path is smoke-tested; the interaction branches are verbatim-C-in-shim, compile-verified) |
| `julia-set`         | `shaders/shaders_julia_set`        | a mouse-zoomable animated Julia set fractal in a fragment shader (c/zoom/offset uniforms re-sent per frame from jank-threaded state) |
| `framebuffer-rendering` | `textures/textures_framebuffer_rendering` | two UpdateCamera cameras at once (free observer + orbital subject), two RTs + a crop overlay; the C's raymath frustum-prism helper stays a verbatim cpp/raw shim |
| `eratosthenes-sieve` | `shaders/shaders_eratosthenes_sieve` | the prime sieve per-pixel in a fragment shader; the simplest shader port (no uniforms, the julia-set blank-RT canvas trick) |
| `mandelbrot-set`    | `shaders/shaders_mandelbrot_set`   | deep-zoomable Mandelbrot with adaptive iteration budget; the first INT uniform (an int slot beside the float staging buffer) |
| `rounded-rectangle-shader` | `shaders/shaders_rounded_rectangle` | SDF rounded rectangles (fill/shadow/border) in a fragment shader; the first VEC4 uniforms, via per-type C setters that build the array from scalars |
| `raymarching`       | `shaders/shaders_raymarching_rendering` | a raymarched SDF scene generated per-pixel over one full-screen rectangle; the first VEC3 uniforms (eye/target pushed from the Camera3D pointer via a C shim) driven by a first-person `UpdateCamera` |
| `color-correction`  | `shaders/shaders_color_correction` | the first post-process shader (a shader wrapped around a plain `DrawTexture`); contrast/saturation/brightness FLOAT uniforms, four switchable pictures, raygui sliders → held keys |
| `custom-uniform`    | `shaders/shaders_custom_uniform`   | a swirl post-process over a **RenderTexture** (vs color-correction's direct texture); one mouse-tracked VEC2 uniform, orbital camera, y-flipped texture draw; the OBJ model is simplified to DrawCube primitives |
| `ascii-rendering`   | `shaders/shaders_ascii_rendering`  | a post-process re-rendering the scene as ASCII glyphs; two sprites (one bouncing) into a RenderTexture, resolution VEC2 + adjustable fontSize FLOAT, all animation state threaded as jank doubles |
| `postprocessing`    | `shaders/shaders_postprocessing`   | the postprocess capstone: 12 full-screen effect shaders cycled over a render-textured scene; each is a native Shader let-local picked by a per-index cond (no uniforms set, matching the C); OBJ model → primitives |
| `texture-rendering` | `shaders/shaders_texture_rendering` | a BLANK 1024x1024 texture painted + animated entirely by the cubes_panning shader; one FLOAT uniform (uTime) re-staged each frame from GetTime |
| `multi-sample2d`    | `shaders/shaders_multi_sample2d`   | red + blue textures mixed in a shader; the first SetShaderValueTexture (a second sampler2D, re-bound per frame inside BeginShaderMode), Color literals via GetColor packed ints |
| `palette-switch`    | `shaders/shaders_palette_switch`   | palette-indexed bands recolored by the shader; the first ARRAY uniform (SetShaderValueV, 8 x ivec3) via an int[24] staging buffer filled from a flat jank vector |
| `hot-reloading`     | `shaders/shaders_hot_reloading`    | edit + save reload.fs while it runs and the shader hot-swaps; the runtime-replaced Shader lives in a cpp/raw static (the viewport-scaling pattern), uniform locations re-queried as jank loop state |
| `spotlight-rendering` | `shaders/shaders_spotlight_rendering` | three spotlights alpha-masked over a star field; the first GLSL struct-array uniforms (spots[i].pos member locations from computed jank strings), Star/Spot struct arrays as vectors of maps |
| `font-sdf`          | `text/text_font_sdf`               | bitmap vs SDF font scaling; the pointer-heavy Font assembly (LoadFontData int* out-param, GenImageFontAtlas Rectangle** out-param, struct field writes) stays a C shim returning the finished Font |
| `depth-writing`     | `shaders/shaders_depth_writing`    | inverted gl_FragDepth over an orbital cube scene; the rlgl custom-FBO builder (depth TEXTURE instead of renderbuffer) stays a verbatim C shim pair, TRACELOG swapped for public TraceLog |
| `depth-rendering`   | `shaders/shaders_depth_rendering`  | the depth buffer visualized via SetShaderValueTexture on (.-depth target); LoadModelFromMesh models reduce to DrawCube/DrawPlane primitives, flipY as a scalar INT setter |
| `hybrid-rendering`  | `shaders/shaders_hybrid_rendering` | raymarched spheres + rasterized cubes depth-tested together; camDir = normalize(target-pos)*camDist computed inside the Camera3D-pointer C shim, rlEnableDepthTest called directly |
| `texture-tiling`    | `shaders/shaders_texture_tiling`   | the MODEL blocker falls: GenMeshCube -> LoadModelFromMesh -> DrawModel work with zero wrapper changes; material texture/shader field writes via a (cpp/& model) pointer-shim pair |
| `model-loading`     | `models/models_loading`            | the castle OBJ loaded FROM FILE (LoadModel path proven); GetMeshBoundingBox(meshes[0]) via a one-line shim, ray-pick selection toggles DrawBoundingBox; drag&drop swap dropped |
| `model-shader`      | `shaders/shaders_model_shader`     | the watermill OBJ grayscaled by a shader assigned to its MATERIAL (no BeginShaderMode wrap) - the first model-based shader port after the unblock |
| `heightmap-rendering` | `models/models_heightmap_rendering` | terrain from a grayscale heightmap image: GenMeshHeightmap joins the proven GenMesh* set (Image by value, Vector3 inline, Mesh straight into LoadModelFromMesh) |
| `cubicmap-rendering` | `models/models_cubicmap_rendering` | a cube maze from a tiny black-and-white image via GenMeshCubicmap, atlas-textured; P pauses the orbit (a jank bool gating UpdateCamera) |
| `mesh-generation`   | `models/models_mesh_generation`    | all nine GenMesh* generators as nine Model let-locals dispatched by a per-index cond (the postprocessing pattern); the hand-built triangle mesh stays a verbatim C shim |
| `first-person-maze` | `models/models_first_person_maze`  | first-person maze walking with wall collision; the C's LoadImageColors Color* array becomes a jank 0/1 wall vector via a GetImageColor shim, collision reset via a camera-position write shim |
| `basic-lighting`    | `shaders/shaders_basic_lighting`   | four toggleable point lights via the `rlights` namespace - rlights.h reimplemented in pure jank (cpp/new for the uniform pointers, an opaque-boxed Shader), so the example has no cpp/raw block; first two-path LoadShader (lighting.vs + .fs) |
| `fog-rendering`     | `shaders/shaders_fog_rendering`    | models dissolving into adjustable fog; another `rlights` consumer - only per-file shims are the model helpers (material writes + a raymath MatrixMultiply transform spin) |
| `cel-shading`       | `shaders/shaders_cel_shading`      | a toon-shaded GLB car (first GLB LoadModel) with an inverted-hull outline pass; the default material shader read back via a Shader-returning shim, rlSetCullFace direct, moving light via jank_rl_set_light_pos |
| `normalmap-rendering` | `shaders/shaders_normalmap_rendering` | per-pixel normal-mapped lighting on a spinning plane; first MATERIAL_MAP_NORMAL (a map-slot setup shim doing assign + GenTextureMipmaps + trilinear filter), absolute transform set via MatrixRotateY |
| `simple-mask`       | `shaders/shaders_simple_mask`      | an animated mask as a second sampler on the spare MATERIAL_MAP_EMISSION slot; MatrixRotateXYZ absolute set from three jank-threaded angles, DrawModelEx called directly |
| `vertex-displacement` | `shaders/shaders_vertex_displacement` | vertices displaced by scrolling Perlin-noise lookups in the VERTEX shader; the noise texture bound to sampler slot 1 once via raw rlgl (rlActiveTextureSlot + rlSetUniformSampler, .-id reads) |
| `rlgl-compute`      | `shaders/shaders_rlgl_compute`     | the first COMPUTE SHADERS (wrapper rebuilt with OPENGL_VERSION 4.3): GoL stepped by rlComputeShaderDispatch over ping-ponged SSBOs whose ids swap as jank loop vars; the command SSBO struct stays a cpp/raw static |
| `mesh-instancing`   | `shaders/shaders_mesh_instancing`  | 10000 cubes in one DrawMeshInstanced call; the Matrix transform array is a 640KB C static built by a shim, LoadMaterialDefault's Material a let-local with (cpp/& mat) field-write shims |
| `lightmap-rendering` | `shaders/shaders_lightmap_rendering` | a baked-lightmap plane; the first deep Mesh edit (a verbatim shim RL_MALLOCs texcoords2, uploads it with rlLoadVertexBuffer and wires vertex attribute 5 through (cpp/& mesh)) |
| `shadowmap-rendering` | `shaders/shaders_shadowmap_rendering` | the first MODEL ANIMATIONS (LoadModelAnimations behind C statics, UpdateModelAnimation by index+frame) + a depth-only FBO, a two-pass light/player render, and per-frame light Camera3D rebuilds |
| `basic-pbr`         | `shaders/shaders_basic_pbr`        | the PBR pipeline: albedo/normal/packed-MRA/emissive maps wired into material slots, the example's OWN intensity-carrying light struct as a per-file cpp/raw block, and the C's shader-unbind teardown kept as a shim |
| `deferred-rendering` | `shaders/shaders_deferred_rendering` | the first MULTI-RENDER-TARGET framebuffer (3 color textures + depth renderbuffer, rlActiveDrawBuffers 3) behind C-static id accessors; depth blit + sampler-unit init shims, rlLoadDrawQuad direct |
| `game-of-life`      | `shaders/shaders_game_of_life`     | the shaders capstone (category COMPLETE): a 2048x2048 Life world stepped by a fragment shader over flip?-selected ping-pong RTs; raygui panel -> key legend, the malloc'd draw-mode Image + preset loaders as C-static shims |
| `loading-gltf`      | `models/models_loading_gltf`       | the animated glTF robot with switchable animations; the shadowmap animation shims + the anim's char[32] name folded to a jank string via a const-char* accessor |
| `yaw-pitch-roll`    | `models/models_yaw_pitch_roll`     | the WWI plane flown through pitch/yaw/roll as jank reals with ease-back-to-level conds; MatrixRotateXYZ absolute-set shim fed DEG2RAD-scaled angles |
| `mesh-picking`      | `models/models_mesh_picking`       | closest-hit picking over quad/triangle/sphere/box/mesh: each native RayCollision boxed to a jank map on read, the accumulation pure jank; the per-mesh loop a RayCollision-returning shim |
| `loading-iqm`       | `models/models_loading_iqm`        | first IQM format: animated mesh + animation data from separate .iqm files, stood upright with a -90 X DrawModelEx |
| `loading-m3d`       | `models/models_loading_m3d`        | first M3D format; the C's DrawModelSkeleton (skeleton.bones[] + keyframePoses[frame][] walks) stays a verbatim shim on SPACE |
| `loading-vox`       | `models/models_loading_vox`        | first VOX format (every model format now proven); bounding-box XZ-centering shim, first UpdateCameraPro with inline movement/rotation Vector3s, four models cycled by cond |
| `animation-timing`  | `models/models_animation_timing`   | fractional-frame playback (the update shim takes the frame as a double); raygui dropdown/slider/progress-bar -> keyboard + a hand-drawn timeline with keyframe ticks |
| `bone-socket`       | `models/models_bone_socket`        | equipment riding named skeleton bones: bone lookup by name + the per-socket quaternion math (frame rotation vs bind pose) as verbatim shims ending in DrawMesh on materials[1] |
| `point-rendering`   | `models/models_point_rendering`    | up to 10M points; the count-change-replaced Mesh+Model live as C statics (the hot-reloading pattern), rlEnablePointMode wraps one DrawModel vs a per-point DrawPoint3D shim loop |
| `skybox-rendering`  | `models/models_skybox_rendering`   | first TextureCubemap (LoadTextureCubemap from a layout image into MATERIAL_MAP_CUBEMAP); drawn from inside the cube with culling + depth writes off; HDR panorama + drag&drop paths dropped |
| `animation-blending` | `models/models_animation_blending` | first UpdateModelAnimationEx (two anims + a blend factor through one shim); the transition state machine as pure jank loop vars mirroring the side-effecting update branch |
| `animation-blend-custom` | `models/models_animation_blend_custom` | per-bone blending: the C's custom UpdateModelAnimationBones (Vector3Lerp/QuaternionSlerp per bone + a full CPU-skinning re-upload) and its bone-name classifier stay verbatim shims |
| `decals`            | `models/models_decals`             | the models capstone: the ~290-line decal generator (MeshBuilder + 6-plane clipping + UV projection) verbatim; up to 256 runtime-created decal Models in a C static array behind add/draw/clear/stat wrappers |
| `raw-stream`        | `audio/audio_raw_stream`           | the first RAW AUDIO STREAM: LoadAudioStream as a let-local, IsAudioStreamProcessed gating a C-shim refill (the oscillator state + float buffer feeding UpdateAudioStream) |
| `mixed-processor`   | `audio/audio_mixed_processor`      | the first AUDIO CALLBACK: the DSP fn defined in cpp/raw is ordinary C, so a sibling wrapper attaches its pointer with AttachAudioMixedProcessor; jank tunes the exponent + reads the volume history via statics |
| `stream-effects`    | `audio/audio_stream_effects`       | per-STREAM callbacks (AttachAudioStreamProcessor on music.stream): the 70Hz lowpass + one-second ring-buffer delay verbatim in cpp/raw, toggled live by attach/detach wrappers |
| `stream-callback`   | `audio/audio_stream_callback`      | the pull model: SetAudioStreamCallback generators (four waveforms sharing oscillator statics) picked by index from a C callback array; the plot reads a one-second history buffer |
| `amp-envelope`      | `audio/audio_amp_envelope`         | the per-sample ADSR state machine + sine fill as a C shim over the raw-stream pattern; raygui sliders -> key pairs + drawn bars, the shape graph pure jank (with hand-folded literals per the all-native-chain trap) |
| `spectrum-visualizer` | `audio/audio_spectrum_visualizer` | the audio capstone (category COMPLETE): the whole FFT pipeline (Cooley-Tukey, Blackman window, dB smoothing, PCM feed off a decoded Wave) verbatim in cpp/raw; jank owns setup + per-frame calls |
| `screen-buffer`     | `textures/textures_screen_buffer`  | the DOS fire: the ~90k-cell palette-indexed simulation (RL_CALLOC'd buffers, four loops, ImageDrawPixel blit) as one C step-shim per frame |

## Not yet ported

### shapes, complete! 🎉

All 41 `shapes` examples are ported: the pure-raylib ones directly, the
raygui-marked ones with keyboard controls
(`docs/guide/raygui-to-keyboard.md`), the RenderTexture ones via
`LoadRenderTexture`, and the rlgl ones via a direct
`(:include "rlgl.h")` (no wrapper changes were needed).

### core, 2D-friendly candidates (portable now)

The rest of `core` is mostly 3D free-look cameras, VR, web, and platform
I/O, not reachable with the current wrappers. Static/inline-rebuilt 3D
cameras DO work (`camera-3d`, `picking-3d`), as do the plain 2D input
demos (`input-multitouch`, `input-virtual-controls`).
`core_random_sequence`'s `int*` from `LoadRandomSequence` is now ported
(`random-sequence`) via the same `cpp/raw` subscript shim gif-player uses.
Platform clipboard I/O also works: `Get`/`SetClipboardText` round-trip
through jank strings (`clipboard-text`). Directory I/O works too:
`LoadDirectoryFilesEx` returns a `FilePathList` whose `char **` paths are
indexed through the `cpp/raw` subscript shim and snapshotted into jank
data each load (`directory-files`).

`UpdateCamera(&camera, mode)` is now proven too (2026-07-05):
`(cpp/& camera)` on an outer-let Camera3D forms the pointer and the
mutation persists across frames (`camera-3d-free`), so the free-look
camera family is a set of straight ports now (`camera-3d-first-person`,
`camera-3d-split-screen` and `camera-3d-fps` - the full physics
character controller over raymath, its Body struct + Vector globals in
a `cpp/raw` shim - all landed). Callback-registering APIs are no longer
a blocker either: `SetTraceLogCallback` takes a fn pointer to a
`void(int,const char*,va_list)` defined in a `cpp/raw` block (the
audio-callback pattern), so `core_custom_logging` is ported
(`custom-logging`). The VR simulator is ported too (`vr-simulator`):
`LoadVrStereoConfig` / `BeginVrStereoMode` need no headset, so the
native VrDeviceInfo + VrStereoConfig + distortion shader + HMD
RenderTexture ride in a `cpp/raw` shim. Automation-event record/replay
is ported too (`automation-events`): the native AutomationEventList +
`LoadAutomationEventList`/`StartAutomationEventRecording`/`PlayAutomationEvent`
sit in a `cpp/raw` shim (the record→export→replay path verified
end-to-end, writing a valid `.rae`). `core_input_gamepad` is ported too
(`input-gamepad`): the live controller diagram runs (the NOT-DETECTED
path smoke-tests here since this box has no pad; the button/axis
branches are verbatim-C-in-shim). The genuinely remaining `core`
blockers are web (emscripten), `core_screen_recording` (bundles the
`msf_gif.h` encoder, which isn't on the wrapper's include path), and
`core_custom_frame_control`, which needs raylib recompiled with
`SUPPORT_CUSTOM_FRAME_CONTROL`.

### text, COMPLETE (16/16)

Font loading now works: `LoadFont` (BMFont .fnt + .png) and `LoadFontEx`
(TTF rasterized at load) return a `Font` native struct held as an outer-let
local, drawn with `DrawTextEx` (see `font-loading`).
`text_rectangle_bounds` is now ported too (`rectangle-bounds`): rather than
indexing `font.glyphs[i]` / `font.recs[i]` for per-glyph advances, layout
measures ordinary substrings with `MeasureTextEx` and re-wraps to the
container width each frame. Remaining leftovers:

- `text_strings_management` is now ported too (`strings-management`):
  the current raylib version is a draggable text-particle physics toy,
  not a bare API dump - grab/slice/shatter/glue text boxes while keys
  1-6 run `TextToUpper`/`Lower`/`Pascal`/`Snake`/`Camel`. The
  TextParticle[100] pool + string ops ride in a `cpp/raw` shim, proving
  raylib's whole TextX API is reachable from jank (even though jank has
  its own string fns). **The text category is now complete (16/16).**
- The codepoint/unicode family is fully ported (`codepoints-loading`,
  `unicode-ranges`, `inline-styling`, `unicode-emojis`): `int*`
  codepoint arrays read via `cpp/aget`, written back through a
  static-buffer setter shim, glyph metrics read off the
  `GlyphInfo`/`Rectangle` struct arrays, and UTF-8 literals in-source.
  `text_font_sdf` is now ported too (`font-sdf`): the pointer-heavy
  Font assembly stays a C shim, the SDF shader is the ordinary
  BeginShaderMode wrap. `text_3d_drawing` is now ported too
  (`text-3d-drawing`): the whole rlgl glyph-quad engine, native
  `Font`/`Color`/`WaveTextConfig`, `rlBegin(RL_QUADS)` per-glyph quads,
  the two `SHOW_*_BOUNDRY` globals, drag-drop font swap and live typing -
  rides in one `cpp/raw` shim that jank drives per frame. With
  `strings-management` also ported, the text category is complete
  (16/16).

### Blocked on new wrappers (future work)

These categories need capabilities the repo's `-sys` wrappers don't expose yet:

- **textures** (3 left), NO LONGER fully blocked: `GenImage*` +
  `LoadTextureFromImage` (see `image-generation`), `LoadTexture` from a
  PNG file (see `logo-texture`), `LoadImage` from a file
  (`image-loading`), sprite-sheet sub-rectangle drawing
  (`DrawTextureRec`/`DrawTexturePro`, see `sprite-animation` /
  `srcrec-dstrec` / `sprite-stacking`), multi-texture parallax
  (`background-scrolling`), `BeginBlendMode` (`blend-modes`) and
  `DrawTextureNPatch` with inline-built `NPatchInfo` structs
  (`npatch-drawing`, which needed the first `cpp/int` field casts) all
  work. `Image` pixel manipulation is ALSO unblocked now (2026-07-03):
  the pointer-taking `ImageColor*` / `ImageDraw*` / `ImageFormat` /
  `UpdateTexture` APIs all work through jank's `(cpp/& img)` address-of on
  a mutable let-local, no wrapper change (`image-processing`, see the
  pointer-interop section of `docs/guide/cpp-interop-toolbox.md`). `int*`
  out-params are ALSO unblocked now (2026-07-03): `LoadImageAnim`'s
  `&frames` works via `(cpp/int 0)` + `(cpp/& frames)`, and streaming a
  frame from `image.data + offset` works through a tiny `cpp/raw` pointer
  shim (`gif-player`). The 2 remaining textures examples are harder:
  `textures_clipboard_image` (platform clipboard I/O),
  `textures_framebuffer_rendering` (now ported - see the ported table)
  and `textures_screen_buffer` (native
  `RL_CALLOC` buffers + a 90k-cell `aget`/`aset` palette blit).
- **shaders**, COMPLETE (35/35, 2026-07-11) 🎉 The unlock history:
  `LoadShader(cpp/nullptr, path)` + `BeginShaderMode`/`EndShaderMode`
  work with zero wrapper changes (`shapes-textures-shader`), and
  uniforms work too (`texture-outline`): `GetShaderLocation` returns a
  boxable int, and `SetShaderValue`'s `const void*` stages through a
  cpp/raw static `float[4]` + element-setter shim (double param, cast
  to float inside) with a `jank_set_uniform(Shader, int, int)` wrapper -
  all shim calls confined to `-main` per the per-fn static rule. VEC4
  uniforms landed via per-type C setters that build the array from
  scalar args (`rounded-rectangle-shader`, which also loads a real vertex
  shader `base.vs` rather than the default). VEC3 uniforms + a
  first-person `UpdateCamera` fly-cam over a pure raymarch shader also
  work (`raymarching`, eye/target pushed straight from the Camera3D
  pointer by a C shim). The postprocess class is open too
  (`color-correction`: a shader wrapped around a plain `DrawTexture`;
  `custom-uniform`: a swirl over a RenderTexture, its OBJ model swapped
  for DrawCube primitives to sidestep `LoadModel`; `ascii-rendering`:
  two sprites into a RenderTexture, re-rendered as ASCII glyphs;
  `postprocessing`: 12 effect shaders cycled over a render-textured
  scene, each a native Shader let-local picked by a per-index cond).
  The Mesh/Model class is open too (2026-07-11, `texture-tiling`):
  `GenMeshCube` -> `LoadModelFromMesh` -> `DrawModel` work with zero
  wrapper changes, and material field writes
  (`materials[0].maps[..].texture`, `.shader`) go through a
  `(cpp/& model)` pointer-shim pair - and the lighting family is open
  (2026-07-11, `basic-lighting`): rlights.h is reimplemented as the
  `rlights` jank namespace - `cpp/new` for the uniform pointers, an
  opaque-boxed `Shader` - so fog / normalmap / cel-shading /
  mesh_instancing are now port work, not blocker work
- **models** (19 left), partially unblocked (2026-07-03): `Camera3D` +
  `BeginMode3D` + the 3D primitives, `BoundingBox` collision checks
  (`box-collisions`) and billboards (`billboard-rendering`) all work.
  `UpdateCamera(&camera, ...)` is now proven via `(cpp/& camera)`
  (`camera-3d-free`); an ORBITAL camera also falls out of rebuilding the
  Camera3D inline each frame from a jank-tracked angle, and ray-picking voxels/boxes with
  `GetScreenToWorldRay` + `GetRayCollisionBox` works from the mouse
  (`basic-voxel`, which also shows an untextured `DrawModel`+tint reduces
  to a plain `DrawCube`). A `DrawModelEx` spin reduces to an rlgl-immediate
  cube rotated on the `rlPushMatrix`/`rlRotatef` stack (`rotating-cube`).
  Mesh/model loading is NO LONGER a blocker (2026-07-11): `GenMeshCube` +
  `LoadModelFromMesh` + `DrawModel` and material field writes all work
  (`texture-tiling` in the shaders category) - the remaining `models`
  examples are straight port work now. One exception:
  `models_animation_gpu_skinning` needs raylib recompiled with
  `SUPPORT_GPU_SKINNING` (off by default and EXCLUSIVE with the CPU
  skinning every other animation example uses), so it stays unported.
- **audio** (9 left), NO LONGER blocked on a wrapper: `InitAudioDevice` +
  `LoadSound` + `PlaySound` work directly (miniaudio backend is compiled
  into `libraylib`, see `sprite-button` and `sound-loading`, which also
  proves OGG decoding), and so do music streams: `LoadMusicStream` +
  `UpdateMusicStream` + pan/volume setters (`music-stream`, MP3).
  Raw streams and audio CALLBACKS are unblocked too (2026-07-11):
  `LoadAudioStream` + `UpdateAudioStream` work through a C-shim refill
  (`raw-stream`), and `AttachAudioMixedProcessor` accepts a callback
  DEFINED in the example's `cpp/raw` block, jank can't form function
  pointers, but a C-defined callback attached by a sibling C wrapper
  is ordinary C (`mixed-processor`). The remaining audio examples are
  port work.
- most of **core**, 3D cameras, VR simulator, web target, gamepad, file I/O

Adding one of these means first extending a `-sys` wrapper to link/expose the
needed raylib functions, then porting the examples on top of it.
