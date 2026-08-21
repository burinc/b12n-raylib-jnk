# The example catalog — 209 raylib demos in jank

A map of the whole suite. Each example is one namespace under
`raylib-examples/src/raylib_examples/`, runnable by a friendly `bb <name>`
task or the underlying `lein with-profile +<name> run --disable-sandbox`.
`bb info` prints this grouping live; this page adds the "how it's wired"
recipe at the end.

Run one, list them, or reel through all of them:

```sh
bb <name>          # e.g. bb starfield   (opens a window)
bb examples        # flat list with descriptions
bb info            # the grouped cheat-sheet below
bb run-all [secs]  # every example, N seconds each (unattended)
```

## core: window, input, cameras, files (46)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/input-keys.gif" width="80">](../demos/input-keys.gif) | `input-keys` | Steer a ball with the arrow keys |
| [<img src="../demos/input-mouse.gif" width="80">](../demos/input-mouse.gif) | `input-mouse` | A ball follows the mouse; click to recolor |
| [<img src="../demos/input-mouse-wheel.gif" width="80">](../demos/input-mouse-wheel.gif) | `input-mouse-wheel` | Scroll a box with the mouse wheel |
| [<img src="../demos/random-values.gif" width="80">](../demos/random-values.gif) | `random-values` | A new random value every two seconds |
| [<img src="../demos/camera-2d.gif" width="80">](../demos/camera-2d.gif) | `camera-2d` | A free 2D camera over a skyline |
| [<img src="../demos/basic-window.gif" width="80">](../demos/basic-window.gif) | `basic-window` | The minimal raylib window + text |
| [<img src="../demos/scissor-test.gif" width="80">](../demos/scissor-test.gif) | `scissor-test` | A scissor rectangle reveals text |
| [<img src="../demos/window-should-close.gif" width="80">](../demos/window-should-close.gif) | `window-should-close` | Confirm-before-exit on window close |
| [<img src="../demos/delta-time.gif" width="80">](../demos/delta-time.gif) | `delta-time` | Delta-time vs per-frame movement |
| [<img src="../demos/basic-screen-manager.gif" width="80">](../demos/basic-screen-manager.gif) | `basic-screen-manager` | A LOGO/TITLE/GAMEPLAY/ENDING flow |
| [<img src="../demos/camera-2d-platformer.gif" width="80">](../demos/camera-2d-platformer.gif) | `camera-2d-platformer` | A platformer with 5 camera-follow modes |
| — | `input-gestures` | Log detected mouse/touch gestures |
| — | `window-letterbox` | A fixed 640x480 game letterboxed on resize |
| [<img src="../demos/camera-2d-split-screen.gif" width="80">](../demos/camera-2d-split-screen.gif) | `camera-2d-split-screen` | Two players, two cameras, split screen |
| [<img src="../demos/smooth-pixelperfect.gif" width="80">](../demos/smooth-pixelperfect.gif) | `smooth-pixelperfect` | Sub-pixel smoothing of upscaled pixel art |
| [<img src="../demos/camera-2d-mouse-zoom.gif" width="80">](../demos/camera-2d-mouse-zoom.gif) | `camera-2d-mouse-zoom` | Pan + zoom-to-cursor a 2D camera |
| [<img src="../demos/world-screen.gif" width="80">](../demos/world-screen.gif) | `world-screen` | A 2D label tracking a 3D cube (GetWorldToScreen) |
| [<img src="../demos/camera-3d.gif" width="80">](../demos/camera-3d.gif) | `camera-3d` | A red cube on a grid through a fixed 3D camera |
| [<img src="../demos/picking-3d.gif" width="80">](../demos/picking-3d.gif) | `picking-3d` | Click a 3D box to pick it with a world-space ray |
| [<img src="../demos/input-multitouch.gif" width="80">](../demos/input-multitouch.gif) | `input-multitouch` | A ball at every active touch/mouse point |
| [<img src="../demos/input-virtual-controls.gif" width="80">](../demos/input-virtual-controls.gif) | `input-virtual-controls` | An on-screen D-pad moving a player circle |
| — | `window-flags` | Toggle window state flags live with a bouncing ball |
| [<img src="../demos/render-texture.gif" width="80">](../demos/render-texture.gif) | `render-texture` | A ball bouncing inside a rotated off-screen render texture |
| [<img src="../demos/monitor-detector.gif" width="80">](../demos/monitor-detector.gif) | `monitor-detector` | A scaled map of every attached monitor with its specs |
| [<img src="../demos/input-actions.gif" width="80">](../demos/input-actions.gif) | `input-actions` | Remappable abstract actions (WASD/arrows) via a keyset map |
| [<img src="../demos/highdpi-demo.gif" width="80">](../demos/highdpi-demo.gif) | `highdpi-demo` | Logical-points vs physical-pixels grids with live DPI scale |
| — | `highdpi-testbed` | A HighDPI diagnostic overlay: grid, monitor/DPI info, crosshair |
| [<img src="../demos/random-sequence.gif" width="80">](../demos/random-sequence.gif) | `random-sequence` | Colored bars in a random no-repeat permutation (LoadRandomSequence) |
| [<img src="../demos/clipboard-text.gif" width="80">](../demos/clipboard-text.gif) | `clipboard-text` | Type + cut/copy/paste with the system clipboard |
| [<img src="../demos/undo-redo.gif" width="80">](../demos/undo-redo.gif) | `undo-redo` | A grid player with a 26-slot undo/redo ring buffer |
| [<img src="../demos/directory-files.gif" width="80">](../demos/directory-files.gif) | `directory-files` | A keyboard file browser over the working directory |
| [<img src="../demos/custom-logging.gif" width="80">](../demos/custom-logging.gif) | `custom-logging` | A custom trace-log callback timestamps + tags every raylib log line |
| [<img src="../demos/drop-files.gif" width="80">](../demos/drop-files.gif) | `drop-files` | Drag files onto the window to list their paths |
| [<img src="../demos/text-file-loading.gif" width="80">](../demos/text-file-loading.gif) | `text-file-loading` | Load + word-wrap a text file, scroll it |
| [<img src="../demos/compute-hash.gif" width="80">](../demos/compute-hash.gif) | `compute-hash` | CRC32/MD5/SHA1/SHA256 + Base64 of typed text |
| [<img src="../demos/storage-values.gif" width="80">](../demos/storage-values.gif) | `storage-values` | Save/load a score pair to a binary storage file |
| [<img src="../demos/keyboard-testbed.gif" width="80">](../demos/keyboard-testbed.gif) | `keyboard-testbed` | An on-screen ENG-US keyboard highlighting held keys |
| [<img src="../demos/input-gestures-testbed.gif" width="80">](../demos/input-gestures-testbed.gif) | `input-gestures-testbed` | A gesture dashboard with log, indicators and protractor |
| [<img src="../demos/viewport-scaling.gif" width="80">](../demos/viewport-scaling.gif) | `viewport-scaling` | A fixed game resolution scaled into a resizable window |
| [<img src="../demos/camera-3d-free.gif" width="80">](../demos/camera-3d-free.gif) | `camera-3d-free` | A free-look 3D camera around a cube |
| [<img src="../demos/camera-3d-first-person.gif" width="80">](../demos/camera-3d-first-person.gif) | `camera-3d-first-person` | Walk a yard of random columns in first person |
| [<img src="../demos/camera-3d-split-screen.gif" width="80">](../demos/camera-3d-split-screen.gif) | `camera-3d-split-screen` | Two players, two 3D cameras, split screen |
| [<img src="../demos/camera-3d-fps.gif" width="80">](../demos/camera-3d-fps.gif) | `camera-3d-fps` | A physics FPS controller with head-bob, lean and strafe-accel |
| [<img src="../demos/vr-simulator.gif" width="80">](../demos/vr-simulator.gif) | `vr-simulator` | A 3D scene in stereo through a simulated Oculus Rift + lens-distortion shader |
| [<img src="../demos/automation-events.gif" width="80">](../demos/automation-events.gif) | `automation-events` | A 2D platformer with input record/replay via AutomationEventList |
| [<img src="../demos/input-gamepad.gif" width="80">](../demos/input-gamepad.gif) | `input-gamepad` | A live controller diagram: buttons/sticks/triggers light up (Xbox/PS/generic) |

## shapes: 2D drawing, easing, rlgl (41)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/bouncing-ball.gif" width="80">](../demos/bouncing-ball.gif) | `bouncing-ball` | A ball bouncing with optional gravity |
| [<img src="../demos/colors-palette.gif" width="80">](../demos/colors-palette.gif) | `colors-palette` | Every named raylib color in a grid |
| [<img src="../demos/starfield.gif" width="80">](../demos/starfield.gif) | `starfield` | A perspective starfield flying at you |
| [<img src="../demos/mouse-trail.gif" width="80">](../demos/mouse-trail.gif) | `mouse-trail` | A fading trail follows the cursor |
| [<img src="../demos/logo-anim.gif" width="80">](../demos/logo-anim.gif) | `logo-anim` | The raylib logo assembling itself |
| [<img src="../demos/double-pendulum.gif" width="80">](../demos/double-pendulum.gif) | `double-pendulum` | Chaotic double-pendulum motion + trail |
| [<img src="../demos/particles.gif" width="80">](../demos/particles.gif) | `particles` | Water / smoke / fire particle effects |
| [<img src="../demos/collision-area.gif" width="80">](../demos/collision-area.gif) | `collision-area` | AABB collision between a bouncing + mouse box |
| [<img src="../demos/ball-physics.gif" width="80">](../demos/ball-physics.gif) | `ball-physics` | Grab and throw balls under gravity |
| [<img src="../demos/easings-rectangles.gif" width="80">](../demos/easings-rectangles.gif) | `easings-rectangles` | A grid shrinks and spins via easing fns |
| [<img src="../demos/following-eyes.gif" width="80">](../demos/following-eyes.gif) | `following-eyes` | Two eyes track the mouse cursor |
| [<img src="../demos/lines-bezier.gif" width="80">](../demos/lines-bezier.gif) | `lines-bezier` | Drag endpoints to reshape a Bezier curve |
| [<img src="../demos/rectangle-scaling.gif" width="80">](../demos/rectangle-scaling.gif) | `rectangle-scaling` | Resize a rectangle by its corner |
| [<img src="../demos/dashed-line.gif" width="80">](../demos/dashed-line.gif) | `dashed-line` | A dashed line follows the mouse |
| [<img src="../demos/basic-shapes.gif" width="80">](../demos/basic-shapes.gif) | `basic-shapes` | A gallery of raylib's basic shapes |
| [<img src="../demos/logo-raylib.gif" width="80">](../demos/logo-raylib.gif) | `logo-raylib` | The raylib logo from rectangles + text |
| [<img src="../demos/easings-ball.gif" width="80">](../demos/easings-ball.gif) | `easings-ball` | A ball animated through easing stages |
| [<img src="../demos/easings-box.gif" width="80">](../demos/easings-box.gif) | `easings-box` | A box animated through five easing stages |
| [<img src="../demos/math-angle-rotation.gif" width="80">](../demos/math-angle-rotation.gif) | `math-angle-rotation` | Fixed-angle lines + a spinning line |
| [<img src="../demos/ellipse-collision.gif" width="80">](../demos/ellipse-collision.gif) | `ellipse-collision` | Overlap test between two ellipses |
| [<img src="../demos/vector-angle.gif" width="80">](../demos/vector-angle.gif) | `vector-angle` | Two ways to measure an angle |
| [<img src="../demos/penrose-tile.gif" width="80">](../demos/penrose-tile.gif) | `penrose-tile` | A Penrose tiling grown with an L-system |
| [<img src="../demos/digital-clock.gif" width="80">](../demos/digital-clock.gif) | `digital-clock` | A live clock (digital + analogue modes) |
| [<img src="../demos/clock-of-clocks.gif" width="80">](../demos/clock-of-clocks.gif) | `clock-of-clocks` | Digits drawn from grids of little clocks |
| [<img src="../demos/lines-drawing.gif" width="80">](../demos/lines-drawing.gif) | `lines-drawing` | A paint canvas (RenderTexture) |
| [<img src="../demos/easings-testbed.gif" width="80">](../demos/easings-testbed.gif) | `easings-testbed` | An interactive testbed for all 28 easings |
| [<img src="../demos/bullet-hell.gif" width="80">](../demos/bullet-hell.gif) | `bullet-hell` | A magic circle spraying bullet spirals |
| [<img src="../demos/ring-drawing.gif" width="80">](../demos/ring-drawing.gif) | `ring-drawing` | A ring/annulus with adjustable angles |
| [<img src="../demos/circle-sector-drawing.gif" width="80">](../demos/circle-sector-drawing.gif) | `circle-sector-drawing` | A circle sector with adjustable angles |
| [<img src="../demos/rounded-rectangle.gif" width="80">](../demos/rounded-rectangle.gif) | `rounded-rectangle` | A rounded rectangle, size/roundness knobs |
| [<img src="../demos/recursive-tree.gif" width="80">](../demos/recursive-tree.gif) | `recursive-tree` | A binary fractal tree with live knobs |
| [<img src="../demos/triangle-strip.gif" width="80">](../demos/triangle-strip.gif) | `triangle-strip` | A rainbow triangle-strip fan |
| [<img src="../demos/math-sine-cosine.gif" width="80">](../demos/math-sine-cosine.gif) | `math-sine-cosine` | A live unit-circle trig visualization |
| [<img src="../demos/hilbert-curve.gif" width="80">](../demos/hilbert-curve.gif) | `hilbert-curve` | A rainbow Hilbert space-filling curve |
| [<img src="../demos/pie-chart.gif" width="80">](../demos/pie-chart.gif) | `pie-chart` | An interactive pie chart with hover pop |
| [<img src="../demos/kaleidoscope.gif" width="80">](../demos/kaleidoscope.gif) | `kaleidoscope` | Draw strokes mirrored with 6-fold symmetry |
| [<img src="../demos/splines-drawing.gif" width="80">](../demos/splines-drawing.gif) | `splines-drawing` | Draggable spline points, 4 spline types |
| [<img src="../demos/rlgl-triangle.gif" width="80">](../demos/rlgl-triangle.gif) | `rlgl-triangle` | A rainbow triangle via rlgl immediate mode |
| [<img src="../demos/rlgl-color-wheel.gif" width="80">](../demos/rlgl-color-wheel.gif) | `rlgl-color-wheel` | An HSV color picker wheel via rlgl |
| [<img src="../demos/top-down-lights.gif" width="80">](../demos/top-down-lights.gif) | `top-down-lights` | 2D lights casting shadow volumes off boxes |
| [<img src="../demos/rectangle-advanced.gif" width="80">](../demos/rectangle-advanced.gif) | `rectangle-advanced` | Rounded gradient rectangles via rlgl |

## text: fonts, unicode, layout (16)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/format-text.gif" width="80">](../demos/format-text.gif) | `format-text` | Zero-padded score/time text readouts |
| [<img src="../demos/writing-anim.gif" width="80">](../demos/writing-anim.gif) | `writing-anim` | A message types itself out |
| [<img src="../demos/input-box.gif" width="80">](../demos/input-box.gif) | `input-box` | A hover-to-type text input box |
| [<img src="../demos/words-alignment.gif" width="80">](../demos/words-alignment.gif) | `words-alignment` | Align a word inside a rectangle |
| [<img src="../demos/font-loading.gif" width="80">](../demos/font-loading.gif) | `font-loading` | Load a BMFont and a TTF font (DrawTextEx) |
| [<img src="../demos/font-filters.gif" width="80">](../demos/font-filters.gif) | `font-filters` | Scale a TTF word, switch texture filters |
| [<img src="../demos/font-spritefont.gif" width="80">](../demos/font-spritefont.gif) | `font-spritefont` | Three colored sprite fonts from PNG atlases |
| [<img src="../demos/sprite-fonts.gif" width="80">](../demos/sprite-fonts.gif) | `sprite-fonts` | A gallery of raylib's eight bundled sprite fonts |
| [<img src="../demos/rectangle-bounds.gif" width="80">](../demos/rectangle-bounds.gif) | `rectangle-bounds` | Word-wrapped text in a mouse-resizable container |
| [<img src="../demos/codepoints-loading.gif" width="80">](../demos/codepoints-loading.gif) | `codepoints-loading` | Japanese text rasterized to a minimal TTF font atlas |
| [<img src="../demos/unicode-ranges.gif" width="80">](../demos/unicode-ranges.gif) | `unicode-ranges` | Grow a multilingual font atlas by unicode range |
| [<img src="../demos/inline-styling.gif" width="80">](../demos/inline-styling.gif) | `inline-styling` | Text with inline color style tags |
| [<img src="../demos/unicode-emojis.gif" width="80">](../demos/unicode-emojis.gif) | `unicode-emojis` | Click emojis for multilingual speech bubbles |
| [<img src="../demos/text-3d-drawing.gif" width="80">](../demos/text-3d-drawing.gif) | `text-3d-drawing` | A bitmap font drawn as textured quads in 3D, waving the `~~World~~`-marked span |
| [<img src="../demos/strings-management.gif" width="80">](../demos/strings-management.gif) | `strings-management` | Drag/slice/shatter/glue text particles; 1-6 run raylib's TextTo* fns |
| [<img src="../demos/font-sdf.gif" width="80">](../demos/font-sdf.gif) | `font-sdf` | Bitmap vs SDF font scaling, the SDF drawn through a shader |

## textures: images, sprites, render textures (31)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/image-generation.gif" width="80">](../demos/image-generation.gif) | `image-generation` | Nine procedural textures (gradients/noise) |
| [<img src="../demos/logo-texture.gif" width="80">](../demos/logo-texture.gif) | `logo-texture` | The raylib logo loaded from a PNG file |
| [<img src="../demos/sprite-animation.gif" width="80">](../demos/sprite-animation.gif) | `sprite-animation` | Scarfy runs: 6-frame spritesheet animation |
| [<img src="../demos/srcrec-dstrec.gif" width="80">](../demos/srcrec-dstrec.gif) | `srcrec-dstrec` | Rotate + scale a sprite frame (DrawTexturePro) |
| [<img src="../demos/background-scrolling.gif" width="80">](../demos/background-scrolling.gif) | `background-scrolling` | Parallax-scrolling cyberpunk street layers |
| [<img src="../demos/image-loading.gif" width="80">](../demos/image-loading.gif) | `image-loading` | LoadImage (RAM) then LoadTextureFromImage (VRAM) |
| [<img src="../demos/blend-modes.gif" width="80">](../demos/blend-modes.gif) | `blend-modes` | Four 2D blend modes over the cyberpunk street |
| [<img src="../demos/particles-blending.gif" width="80">](../demos/particles-blending.gif) | `particles-blending` | Spark particles trail the mouse (alpha/additive) |
| [<img src="../demos/mouse-painting.gif" width="80">](../demos/mouse-painting.gif) | `mouse-painting` | A paint program on a RenderTexture canvas |
| [<img src="../demos/sprite-button.gif" width="80">](../demos/sprite-button.gif) | `sprite-button` | A 3-state sprite button with a click sound |
| [<img src="../demos/bunnymark.gif" width="80">](../demos/bunnymark.gif) | `bunnymark` | The classic bunny-spawning batching benchmark |
| [<img src="../demos/fog-of-war.gif" width="80">](../demos/fog-of-war.gif) | `fog-of-war` | A tile map hidden by smooth fog of war |
| [<img src="../demos/tiled-drawing.gif" width="80">](../demos/tiled-drawing.gif) | `tiled-drawing` | Tile a texture pattern with scale/rotation/color |
| [<img src="../demos/sprite-explosion.gif" width="80">](../demos/sprite-explosion.gif) | `sprite-explosion` | Click to play a 5x5 explosion spritesheet + sound |
| [<img src="../demos/sprite-stacking.gif" width="80">](../demos/sprite-stacking.gif) | `sprite-stacking` | A voxel booth from 122 stacked rotated slices |
| [<img src="../demos/npatch-drawing.gif" width="80">](../demos/npatch-drawing.gif) | `npatch-drawing` | Stretchable 9-patch / 3-patch UI panels |
| [<img src="../demos/image-processing.gif" width="80">](../demos/image-processing.gif) | `image-processing` | Nine CPU image filters via pointer-taking Image* APIs |
| [<img src="../demos/image-drawing.gif" width="80">](../demos/image-drawing.gif) | `image-drawing` | One texture composed from several CPU images |
| [<img src="../demos/image-text.gif" width="80">](../demos/image-text.gif) | `image-text` | Text baked into an image with a TTF font |
| [<img src="../demos/image-rotate.gif" width="80">](../demos/image-rotate.gif) | `image-rotate` | The logo rotated +45/+90/-90 in CPU memory |
| [<img src="../demos/image-channel.gif" width="80">](../demos/image-channel.gif) | `image-channel` | RGBA channels split + alpha-masked |
| [<img src="../demos/image-kernel.gif" width="80">](../demos/image-kernel.gif) | `image-kernel` | Sharpen/sobel/gaussian convolution kernels |
| [<img src="../demos/cellular-automata.gif" width="80">](../demos/cellular-automata.gif) | `cellular-automata` | Wolfram rule cellular automaton, editable rule |
| [<img src="../demos/magnifying-glass.gif" width="80">](../demos/magnifying-glass.gif) | `magnifying-glass` | A circular magnifier revealing hidden bunnies |
| [<img src="../demos/to-image.gif" width="80">](../demos/to-image.gif) | `to-image` | Round-trip an image VRAM<->RAM (LoadImageFromTexture) |
| [<img src="../demos/polygon-drawing.gif" width="80">](../demos/polygon-drawing.gif) | `polygon-drawing` | A cat texture mapped onto a spinning polygon |
| [<img src="../demos/raw-data.gif" width="80">](../demos/raw-data.gif) | `raw-data` | A .raw pixel dump + a code-generated checkerboard |
| [<img src="../demos/textured-curve.gif" width="80">](../demos/textured-curve.gif) | `textured-curve` | A road texture swept along a draggable Bezier |
| [<img src="../demos/gif-player.gif" width="80">](../demos/gif-player.gif) | `gif-player` | An animated GIF streamed frame-by-frame to a texture |
| [<img src="../demos/framebuffer-rendering.gif" width="80">](../demos/framebuffer-rendering.gif) | `framebuffer-rendering` | An observer camera watching a subject camera + frustum |
| [<img src="../demos/screen-buffer.gif" width="80">](../demos/screen-buffer.gif) | `screen-buffer` | The classic DOS fire effect in a palette-indexed buffer |

## models: meshes, 3D, OBJ/GLB (29)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/geometric-shapes.gif" width="80">](../demos/geometric-shapes.gif) | `geometric-shapes` | 3D cubes/spheres/cylinders/capsules on a grid |
| [<img src="../demos/box-collisions.gif" width="80">](../demos/box-collisions.gif) | `box-collisions` | A player cube colliding with 3D obstacles |
| [<img src="../demos/billboard-rendering.gif" width="80">](../demos/billboard-rendering.gif) | `billboard-rendering` | Camera-facing billboards + an orbiting camera |
| — | `waving-cubes` | 3375 rainbow cubes waving in 3D |
| [<img src="../demos/orthographic-projection.gif" width="80">](../demos/orthographic-projection.gif) | `orthographic-projection` | Toggle perspective vs orthographic camera |
| [<img src="../demos/tesseract-view.gif" width="80">](../demos/tesseract-view.gif) | `tesseract-view` | A rotating 4D hypercube projected to 3D |
| [<img src="../demos/rlgl-solar-system.gif" width="80">](../demos/rlgl-solar-system.gif) | `rlgl-solar-system` | Sun/Earth/Moon via the rlgl matrix stack |
| [<img src="../demos/textured-cube.gif" width="80">](../demos/textured-cube.gif) | `textured-cube` | Two rlgl textured 3D cubes from a shared atlas |
| [<img src="../demos/directional-billboard.gif" width="80">](../demos/directional-billboard.gif) | `directional-billboard` | A sprite-sheet billboard that turns as the camera orbits |
| [<img src="../demos/basic-voxel.gif" width="80">](../demos/basic-voxel.gif) | `basic-voxel` | An 8x8x8 beige voxel grid; click to ray-pick and remove cubes |
| [<img src="../demos/rotating-cube.gif" width="80">](../demos/rotating-cube.gif) | `rotating-cube` | A textured cube spinning on a tilted axis (rlgl matrix stack) |
| [<img src="../demos/model-loading.gif" width="80">](../demos/model-loading.gif) | `model-loading` | The castle OBJ model loaded from disk, ray-pick selection |
| [<img src="../demos/heightmap-rendering.gif" width="80">](../demos/heightmap-rendering.gif) | `heightmap-rendering` | Terrain generated from a grayscale heightmap image |
| [<img src="../demos/cubicmap-rendering.gif" width="80">](../demos/cubicmap-rendering.gif) | `cubicmap-rendering` | A cube maze generated from a tiny black-and-white image |
| [<img src="../demos/mesh-generation.gif" width="80">](../demos/mesh-generation.gif) | `mesh-generation` | All nine procedural mesh generators, checked-textured |
| [<img src="../demos/first-person-maze.gif" width="80">](../demos/first-person-maze.gif) | `first-person-maze` | Walk the cubicmap maze in first person, wall collision + radar |
| [<img src="../demos/loading-gltf.gif" width="80">](../demos/loading-gltf.gif) | `loading-gltf` | The animated glTF robot, switchable animations |
| [<img src="../demos/yaw-pitch-roll.gif" width="80">](../demos/yaw-pitch-roll.gif) | `yaw-pitch-roll` | Fly a WWI plane through pitch/yaw/roll, easing back to level |
| [<img src="../demos/mesh-picking.gif" width="80">](../demos/mesh-picking.gif) | `mesh-picking` | A mouse ray picks the closest quad/triangle/sphere/box/mesh hit |
| [<img src="../demos/loading-iqm.gif" width="80">](../demos/loading-iqm.gif) | `loading-iqm` | The classic IQM guy walking on loop |
| [<img src="../demos/loading-m3d.gif" width="80">](../demos/loading-m3d.gif) | `loading-m3d` | The Cesium Man in Model3D format, skeleton view on SPACE |
| [<img src="../demos/loading-vox.gif" width="80">](../demos/loading-vox.gif) | `loading-vox` | Four MagicaVoxel models under a fly camera + voxel lighting |
| [<img src="../demos/animation-timing.gif" width="80">](../demos/animation-timing.gif) | `animation-timing` | The robot with a playback timeline + adjustable speed |
| [<img src="../demos/bone-socket.gif" width="80">](../demos/bone-socket.gif) | `bone-socket` | A hat, sword and shield riding the greenman's skeleton bones |
| [<img src="../demos/point-rendering.gif" width="80">](../demos/point-rendering.gif) | `point-rendering` | Up to 10 million points: GPU point mode vs per-point draws |
| [<img src="../demos/skybox-rendering.gif" width="80">](../demos/skybox-rendering.gif) | `skybox-rendering` | A cubemap skybox drawn from inside a unit cube |
| [<img src="../demos/animation-blending.gif" width="80">](../demos/animation-blending.gif) | `animation-blending` | SPACE cross-fades the robot between two animations |
| [<img src="../demos/animation-blend-custom.gif" width="80">](../demos/animation-blend-custom.gif) | `animation-blend-custom` | Per-bone blending: walking legs + attacking upper body |
| [<img src="../demos/decals.gif" width="80">](../demos/decals.gif) | `decals` | Click to splat logo decals clipped onto a character's surface |

## shaders: GLSL, uniforms, postprocess, lighting (35)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/shapes-textures-shader.gif" width="80">](../demos/shapes-textures-shader.gif) | `shapes-textures-shader` | A grayscale fragment shader over shapes + a sprite |
| [<img src="../demos/texture-outline.gif" width="80">](../demos/texture-outline.gif) | `texture-outline` | A shader-drawn outline around a sprite |
| [<img src="../demos/texture-waves.gif" width="80">](../demos/texture-waves.gif) | `texture-waves` | A space texture rippled by an animated wave shader |
| [<img src="../demos/julia-set.gif" width="80">](../demos/julia-set.gif) | `julia-set` | A Julia set fractal computed in a fragment shader |
| [<img src="../demos/eratosthenes-sieve.gif" width="80">](../demos/eratosthenes-sieve.gif) | `eratosthenes-sieve` | The Sieve of Eratosthenes per-pixel in a shader |
| [<img src="../demos/mandelbrot-set.gif" width="80">](../demos/mandelbrot-set.gif) | `mandelbrot-set` | The Mandelbrot set in a shader, deep-zoom presets |
| [<img src="../demos/rounded-rectangle-shader.gif" width="80">](../demos/rounded-rectangle-shader.gif) | `rounded-rectangle-shader` | SDF rounded rectangles (fill/shadow/border) in a fragment shader |
| [<img src="../demos/raymarching.gif" width="80">](../demos/raymarching.gif) | `raymarching` | A raymarched SDF scene in a fragment shader, first-person fly-cam |
| [<img src="../demos/color-correction.gif" width="80">](../demos/color-correction.gif) | `color-correction` | A post-process shader tuning contrast/saturation/brightness of a picture |
| [<img src="../demos/custom-uniform.gif" width="80">](../demos/custom-uniform.gif) | `custom-uniform` | A mouse-steered swirl post-process over a render-textured 3D scene |
| [<img src="../demos/ascii-rendering.gif" width="80">](../demos/ascii-rendering.gif) | `ascii-rendering` | A post-process shader re-rendering the scene as ASCII glyphs |
| [<img src="../demos/postprocessing.gif" width="80">](../demos/postprocessing.gif) | `postprocessing` | Twelve full-screen post-process shaders cycled over a 3D scene |
| [<img src="../demos/texture-rendering.gif" width="80">](../demos/texture-rendering.gif) | `texture-rendering` | A blank texture painted and animated entirely by a fragment shader |
| [<img src="../demos/multi-sample2d.gif" width="80">](../demos/multi-sample2d.gif) | `multi-sample2d` | Two textures blended in a shader via a second sampler2D |
| [<img src="../demos/palette-switch.gif" width="80">](../demos/palette-switch.gif) | `palette-switch` | Palette-indexed bands recolored by an ivec3-array shader uniform |
| [<img src="../demos/hot-reloading.gif" width="80">](../demos/hot-reloading.gif) | `hot-reloading` | Hot-swap the reload.fs fragment shader while it runs |
| [<img src="../demos/spotlight-rendering.gif" width="80">](../demos/spotlight-rendering.gif) | `spotlight-rendering` | Three spotlights alpha-masked over a star field + sprite swarm |
| [<img src="../demos/depth-writing.gif" width="80">](../demos/depth-writing.gif) | `depth-writing` | Inverted gl_FragDepth into a custom depth-texture framebuffer |
| [<img src="../demos/depth-rendering.gif" width="80">](../demos/depth-rendering.gif) | `depth-rendering` | The scene's depth buffer visualized through a shader |
| [<img src="../demos/hybrid-rendering.gif" width="80">](../demos/hybrid-rendering.gif) | `hybrid-rendering` | Raymarched spheres + rasterized cubes in one depth-tested scene |
| [<img src="../demos/texture-tiling.gif" width="80">](../demos/texture-tiling.gif) | `texture-tiling` | A generated cube model with its texture tiled 3x3 by a shader |
| [<img src="../demos/model-shader.gif" width="80">](../demos/model-shader.gif) | `model-shader` | The watermill OBJ drawn grayscale via a material-bound shader |
| [<img src="../demos/basic-lighting.gif" width="80">](../demos/basic-lighting.gif) | `basic-lighting` | A plane + cube lit by four toggleable colored point lights |
| [<img src="../demos/fog-rendering.gif" width="80">](../demos/fog-rendering.gif) | `fog-rendering` | Torus/cube/sphere models fading into exponential fog |
| [<img src="../demos/cel-shading.gif" width="80">](../demos/cel-shading.gif) | `cel-shading` | A GLB car toon-shaded with quantized bands + outline |
| [<img src="../demos/normalmap-rendering.gif" width="80">](../demos/normalmap-rendering.gif) | `normalmap-rendering` | A spinning plane lit through a tangent-space normal map |
| [<img src="../demos/simple-mask.gif" width="80">](../demos/simple-mask.gif) | `simple-mask` | An animated mask texture eats holes in two models' plasma skin |
| [<img src="../demos/vertex-displacement.gif" width="80">](../demos/vertex-displacement.gif) | `vertex-displacement` | A plane mesh riding Perlin-noise waves in the vertex shader |
| — | `rlgl-compute` | Game of Life stepped entirely on the GPU by compute shaders |
| [<img src="../demos/mesh-instancing.gif" width="80">](../demos/mesh-instancing.gif) | `mesh-instancing` | Ten thousand lit cubes in one draw call (DrawMeshInstanced) |
| [<img src="../demos/lightmap-rendering.gif" width="80">](../demos/lightmap-rendering.gif) | `lightmap-rendering` | A plane lit by a baked lightmap through a second UV channel |
| [<img src="../demos/shadowmap-rendering.gif" width="80">](../demos/shadowmap-rendering.gif) | `shadowmap-rendering` | Real shadows: an animated robot under the shadowmapping algorithm |
| [<img src="../demos/basic-pbr.gif" width="80">](../demos/basic-pbr.gif) | `basic-pbr` | The rusty car under physically-based rendering (PBR maps) |
| [<img src="../demos/deferred-rendering.gif" width="80">](../demos/deferred-rendering.gif) | `deferred-rendering` | A three-target G-buffer + full-screen deferred lighting pass |
| [<img src="../demos/game-of-life.gif" width="80">](../demos/game-of-life.gif) | `game-of-life` | Conway's Life on a 2048x2048 world: pan/zoom, presets, draw mode |

## audio: sounds, music streams (11)

| preview | `bb` name | shows |
|---|---|---|
| [<img src="../demos/sound-loading.gif" width="80">](../demos/sound-loading.gif) | `sound-loading` | Play a WAV and an OGG sound |
| [<img src="../demos/music-stream.gif" width="80">](../demos/music-stream.gif) | `music-stream` | Stream an MP3 with pan/volume/progress controls |
| [<img src="../demos/module-playing.gif" width="80">](../demos/module-playing.gif) | `module-playing` | A chiptune XM module + pulsing circle waves |
| [<img src="../demos/sound-multi.gif" width="80">](../demos/sound-multi.gif) | `sound-multi` | Overlapping sound playback via sound aliases |
| [<img src="../demos/sound-positioning.gif" width="80">](../demos/sound-positioning.gif) | `sound-positioning` | Spatial audio around an orbiting 3D sphere |
| [<img src="../demos/raw-stream.gif" width="80">](../demos/raw-stream.gif) | `raw-stream` | A sine wave generated sample-by-sample into a raw audio stream |
| [<img src="../demos/mixed-processor.gif" width="80">](../demos/mixed-processor.gif) | `mixed-processor` | A DSP distortion callback on the whole audio mix |
| [<img src="../demos/stream-effects.gif" width="80">](../demos/stream-effects.gif) | `stream-effects` | Stackable lowpass + delay effects on one music stream |
| [<img src="../demos/stream-callback.gif" width="80">](../demos/stream-callback.gif) | `stream-callback` | A pull-model synth: sine/square/triangle/sawtooth on demand |
| [<img src="../demos/amp-envelope.gif" width="80">](../demos/amp-envelope.gif) | `amp-envelope` | An ADSR amplitude envelope on a tone, with a live shape graph |
| [<img src="../demos/spectrum-visualizer.gif" width="80">](../demos/spectrum-visualizer.gif) | `spectrum-visualizer` | A live FFT spectrum of the music through a shader |

## interop: jank/C++ mechanics (1)

Not raylib ports. These demonstrate the language boundary itself, and are
counted separately so the raylib port totals above stay comparable with
upstream.

| | `bb` name | What it shows |
|---|---|---|
| [<img src="../demos/opaque-boxes.gif" width="80">](../demos/opaque-boxes.gif) | `opaque-boxes` | A native `Color` returned from a fn, kept in an immutable vector and captured in a closure, via `cpp/new` + `cpp/box` + `cpp/unbox`. See [`native-value-lifetimes.md`](native-value-lifetimes.md#getting-a-native-value-out-anyway). |

## Adding an example — the four touchpoints

See [`porting-workflow.md`](porting-workflow.md) for the full end-to-end
process (source of truth, docstring format, the headless smoke test). The
short version: a new example touches four places in the same commit —
`raylib-examples/project.clj` (a `:profiles` entry), `bb.edn` (a `bb <name>`
task), `bb/helpers.clj` (a row in the `examples` registry vector, including
its `:cat`), and `raylib-examples/README.md` (move it from the "not yet
ported" queue into the ported table). The repo-root `README.md` carries no
per-example catalog, so it needs no change.

## See also

- [`porting-workflow.md`](porting-workflow.md) — the full registration
  recipe and the headless smoke-test technique this catalog's examples were
  all verified with.
- [`native-value-lifetimes.md`](native-value-lifetimes.md) — the interop
  rule every example in this catalog is written against.
