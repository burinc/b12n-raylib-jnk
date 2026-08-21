(ns helpers
  "Helpers for the b12n-raylib-jnk bb tasks: install jank-raylib-sys and
  build/run the raylib examples."
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Output
;; ---------------------------------------------------------------------------

(def ^:private C
  {:bold "[1m" :green "[0;32m" :yellow "[1;33m"
   :red "[0;31m" :cyan "[0;36m" :magenta "[0;35m" :reset "[0m"})

(defn- c [k s] (str (C k) s (C :reset)))
(defn info [s] (println (c :cyan (str "ℹ️  " s))))
(defn ok   [s] (println (c :green (str "✅ " s))))
(defn err  [s] (println (c :red (str "❌ " s))))
(defn- header [s] (println) (println (c :bold s)) (println (apply str (repeat (count s) "="))))

;; ---------------------------------------------------------------------------
;; Environment: repo root, a working lein, git submodules
;; ---------------------------------------------------------------------------

;; bb runs with the working dir at the repo root (where bb.edn lives).
(def ^:private home (System/getProperty "user.home"))

;; The mise leiningen shim can be broken on some machines; prefer a Homebrew
;; install, then fall back to whatever `lein` is on PATH. Override with LEIN.
(def ^:private lein
  (delay
    (or (System/getenv "LEIN")
        (first (filter fs/exists? ["/usr/local/bin/lein" "/opt/homebrew/bin/lein"]))
        "lein")))

(defn- lein! [dir & args]
  (apply p/shell {:dir dir} @lein args))

(defn ensure-submodules!
  "Fetch the vendored raylib checkout if it is missing.

  raylib itself now comes from org.jank-lang.commons/raylib-sys on Clojars.
  This submodule is kept only as the ASSET source: 101 examples load shaders,
  models, textures, fonts and audio from raylib/examples/*/resources/, about
  70 MB that the published jar does not carry."
  []
  (when-not (fs/exists? "jank-raylib-sys/raylib/examples")
    (info "Fetching git submodule (raylib example assets)…")
    (p/shell "git" "submodule" "update" "--init" "--recursive")))

;; ---------------------------------------------------------------------------
;; Library installation (idempotent - skips when the jar is already in ~/.m2)
;; ---------------------------------------------------------------------------

(defn- jank-sources
  "Every .jank source, as explicit paths.

  clj-kondo does NOT discover .jank files: `clj-kondo --lint <dir>` scans for
  .clj/.cljs/.cljc, finds nothing, and exits 0 in a few milliseconds. That
  silent pass looks exactly like success, so the file list is always explicit."
  []
  (sort (map str (fs/glob "raylib-examples/src" "**/*.jank"))))

(defn lint!
  "clj-kondo over every .jank source.

  `strict?` makes any finding a non-zero exit; otherwise this reports and
  exits 0. See .clj-kondo/config.edn for why three linters are muted."
  [strict?]
  (if-not (fs/which "clj-kondo")
    (do (info "clj-kondo not on PATH - skipping lint.")
        (info "Install: brew install borkdude/brew/clj-kondo")
        (when strict? (System/exit 1)))
    (let [files (jank-sources)
          _ (info (str "Linting " (count files) " .jank sources"))
          {:keys [exit]} (apply p/shell {:continue true} "clj-kondo" "--lint" files)]
      (if (zero? exit)
        (ok "clj-kondo: no findings.")
        (do (err "clj-kondo reported findings (see above).")
            (when strict? (System/exit exit)))))))

(defn nrepl!
  "Start a jank nREPL server for the examples project.

  jank has nREPL support, so this is a real jank REPL - cpp/ interop
  evaluates in it, not just Clojure. lein writes raylib-examples/.nrepl-port
  for editor tooling to pick up."
  []
  (ensure-submodules!)
  (info "Starting jank nREPL in raylib-examples/ (Ctrl-D to quit)")
  (lein! "raylib-examples" "repl"))

;; ---------------------------------------------------------------------------
;; Examples registry
;; ---------------------------------------------------------------------------

(def examples
  [{:profile "bouncing-ball" :cat :shapes   :desc "A ball bouncing with optional gravity"   :controls "SPACE pause · G gravity · Q quit"}
   {:profile "input-keys" :cat :core      :desc "Steer a ball with the arrow keys"        :controls "ARROWS · Q quit"}
   {:profile "colors-palette" :cat :shapes  :desc "Every named raylib color in a grid"      :controls "mouse hover · SPACE all names · Q"}
   {:profile "starfield" :cat :shapes       :desc "A perspective starfield flying at you"   :controls "MOUSE WHEEL · SPACE mode · Q"}
   {:profile "mouse-trail" :cat :shapes     :desc "A fading trail follows the cursor"       :controls "move the mouse · Q quit"}
   {:profile "logo-anim" :cat :shapes       :desc "The raylib logo assembling itself"       :controls "R replay · Q quit"}
   {:profile "double-pendulum" :cat :shapes :desc "Chaotic double-pendulum motion + trail"  :controls "Q quit"}
   {:profile "particles" :cat :shapes       :desc "Water / smoke / fire particle effects"   :controls "mouse · LEFT/RIGHT type · Q"}
   {:profile "input-mouse" :cat :core     :desc "A ball follows the mouse; click to recolor" :controls "mouse · LEFT/MIDDLE/RIGHT · Q"}
   {:profile "collision-area" :cat :shapes  :desc "AABB collision between a bouncing + mouse box" :controls "mouse · SPACE pause · Q"}
   {:profile "ball-physics" :cat :shapes    :desc "Grab and throw balls under gravity"       :controls "drag · RIGHT spawn · WHEEL gravity · MIDDLE shake · Q"}
   {:profile "easings-rectangles" :cat :shapes :desc "A grid shrinks and spins via easing fns" :controls "SPACE replay · Q quit"}
   {:profile "following-eyes" :cat :shapes  :desc "Two eyes track the mouse cursor"          :controls "move the mouse · Q quit"}
   {:profile "lines-bezier" :cat :shapes    :desc "Drag endpoints to reshape a Bezier curve" :controls "drag endpoints · Q quit"}
   {:profile "rectangle-scaling" :cat :shapes :desc "Resize a rectangle by its corner"       :controls "drag corner · Q quit"}
   {:profile "dashed-line" :cat :shapes     :desc "A dashed line follows the mouse"          :controls "ARROWS size · C color · Q quit"}
   {:profile "basic-shapes" :cat :shapes    :desc "A gallery of raylib's basic shapes"       :controls "Q quit"}
   {:profile "logo-raylib" :cat :shapes     :desc "The raylib logo from rectangles + text"   :controls "Q quit"}
   {:profile "easings-ball" :cat :shapes    :desc "A ball animated through easing stages"     :controls "ENTER replay · R restart · Q quit"}
   {:profile "easings-box" :cat :shapes     :desc "A box animated through five easing stages" :controls "SPACE reset · Q quit"}
   {:profile "math-angle-rotation" :cat :shapes :desc "Fixed-angle lines + a spinning line"   :controls "Q quit"}
   {:profile "ellipse-collision" :cat :shapes   :desc "Overlap test between two ellipses"     :controls "mouse · A/B switch · Q quit"}
   {:profile "vector-angle" :cat :shapes    :desc "Two ways to measure an angle"             :controls "mouse · SPACE mode · RIGHT move · Q"}
   {:profile "penrose-tile" :cat :shapes    :desc "A Penrose tiling grown with an L-system"  :controls "UP/DOWN generations · Q quit"}
   {:profile "input-mouse-wheel" :cat :core :desc "Scroll a box with the mouse wheel"      :controls "WHEEL · Q quit"}
   {:profile "random-values" :cat :core   :desc "A new random value every two seconds"     :controls "Q quit"}
   {:profile "camera-2d" :cat :core       :desc "A free 2D camera over a skyline"          :controls "arrows move · WHEEL zoom · A/S rotate · R reset · Q"}
   {:profile "basic-window" :cat :core    :desc "The minimal raylib window + text"         :controls "Q quit"}
   {:profile "opaque-boxes" :cat :interop :desc "Native Colors carried across fns in opaque boxes" :controls "SPACE cycles · Q quit"}
   {:profile "scissor-test" :cat :core    :desc "A scissor rectangle reveals text"         :controls "mouse · S toggle · Q quit"}
   {:profile "window-should-close" :cat :core :desc "Confirm-before-exit on window close"  :controls "ESC/X then Y/N · Q"}
   {:profile "digital-clock" :cat :shapes   :desc "A live clock (digital + analogue modes)"  :controls "SPACE mode · Q quit"}
   {:profile "clock-of-clocks" :cat :shapes :desc "Digits drawn from grids of little clocks" :controls "SPACE 12/24h · Q quit"}
   {:profile "delta-time" :cat :core      :desc "Delta-time vs per-frame movement"         :controls "WHEEL fps · R reset · Q"}
   {:profile "basic-screen-manager" :cat :core :desc "A LOGO/TITLE/GAMEPLAY/ENDING flow"   :controls "ENTER advance · Q quit"}
   {:profile "lines-drawing" :cat :shapes   :desc "A paint canvas (RenderTexture)"           :controls "LEFT paint · RIGHT erase · MIDDLE clear · WHEEL size · Q"}
   {:profile "easings-testbed" :cat :shapes :desc "An interactive testbed for all 28 easings" :controls "ENTER play/pause · SPACE restart · L/R x-ease · U/D y-ease · Q/W A/S duration · T bounded · ESC quit"}
   {:profile "camera-2d-platformer" :cat :core :desc "A platformer with 5 camera-follow modes" :controls "LEFT/RIGHT move · SPACE jump · WHEEL zoom · C camera · R reset · Q quit"}
   {:profile "input-gestures" :cat :core :desc "Log detected mouse/touch gestures"          :controls "tap/drag/swipe in the area · Q quit"}
   {:profile "window-letterbox" :cat :core :desc "A fixed 640x480 game letterboxed on resize" :controls "resize the window · SPACE recolor · Q quit"}
   {:profile "camera-2d-split-screen" :cat :core :desc "Two players, two cameras, split screen" :controls "W/S/A/D player 1 · ARROWS player 2 · Q quit"}
   {:profile "smooth-pixelperfect" :cat :core :desc "Sub-pixel smoothing of upscaled pixel art" :controls "S smooth · O overscan · Q quit"}
   {:profile "format-text" :cat :text     :desc "Zero-padded score/time text readouts"       :controls "Q quit"}
   {:profile "writing-anim" :cat :text    :desc "A message types itself out"                 :controls "SPACE speed up · ENTER restart · Q quit"}
   {:profile "input-box" :cat :text       :desc "A hover-to-type text input box"             :controls "hover + type · BACKSPACE delete · Q (outside box) quit"}
   {:profile "words-alignment" :cat :text :desc "Align a word inside a rectangle"            :controls "L/R h-align · U/D v-align · Q quit"}
   {:profile "bullet-hell" :cat :shapes     :desc "A magic circle spraying bullet spirals"     :controls "L/R rows · U/D speed · Z/X cooldown · SPACE angle · ENTER method · C clear · Q"}
   {:profile "ring-drawing" :cat :shapes    :desc "A ring/annulus with adjustable angles"      :controls "L/R + U/D angles · A/S Z/X radii · O/P segments · R/L/C toggles · Q"}
   {:profile "circle-sector-drawing" :cat :shapes :desc "A circle sector with adjustable angles" :controls "L/R + U/D angles · A/S radius · O/P segments · Q quit"}
   {:profile "rounded-rectangle" :cat :shapes :desc "A rounded rectangle, size/roundness knobs"  :controls "A/S Z/X size · L/R roundness · U/D thickness · O/P segments · R/L/G · Q"}
   {:profile "recursive-tree" :cat :shapes  :desc "A binary fractal tree with live knobs"       :controls "L/R angle · U/D length · A/S decay · Z/X depth · T/Y thick · B bezier · Q"}
   {:profile "triangle-strip" :cat :shapes  :desc "A rainbow triangle-strip fan"                :controls "L/R segments · O outline · Q quit"}
   {:profile "math-sine-cosine" :cat :shapes :desc "A live unit-circle trig visualization"      :controls "SPACE pause · L/R scrub angle · Q quit"}
   {:profile "hilbert-curve" :cat :shapes   :desc "A rainbow Hilbert space-filling curve"       :controls "Z/X order · U/D size · L/R thickness · A animate · R redraw · Q"}
   {:profile "pie-chart" :cat :shapes       :desc "An interactive pie chart with hover pop"     :controls "hover · Z/X slices · N/M select · L/R value · V/P/D toggles · A/S hole · Q"}
   {:profile "kaleidoscope" :cat :shapes    :desc "Draw strokes mirrored with 6-fold symmetry"  :controls "LEFT mouse draw · L/R history · R reset · Q quit"}
   {:profile "splines-drawing" :cat :shapes :desc "Draggable spline points, 4 spline types"     :controls "1-4 type · LEFT drag · RIGHT add · A/S thickness · H helpers · Q"}
   {:profile "rlgl-triangle" :cat :shapes   :desc "A rainbow triangle via rlgl immediate mode"  :controls "drag corners · SPACE lines · L/R culling · R reset · Q"}
   {:profile "rlgl-color-wheel" :cat :shapes :desc "An HSV color picker wheel via rlgl"         :controls "click/drag pick · WHEEL count · U/D size · A/S value · SPACE wires · CTRL+C · Q"}
   {:profile "top-down-lights" :cat :shapes :desc "2D lights casting shadow volumes off boxes"  :controls "drag light 1 · RIGHT add light · L debug view · Q quit"}
   {:profile "rectangle-advanced" :cat :shapes :desc "Rounded gradient rectangles via rlgl"     :controls "Q quit"}
   {:profile "image-generation" :cat :textures :desc "Nine procedural textures (gradients/noise)" :controls "LEFT mouse / RIGHT arrow cycle · Q quit"}
   {:profile "logo-texture" :cat :textures    :desc "The raylib logo loaded from a PNG file"      :controls "Q quit"}
   {:profile "sprite-animation" :cat :textures :desc "Scarfy runs: 6-frame spritesheet animation" :controls "L/R animation speed · Q quit"}
   {:profile "srcrec-dstrec" :cat :textures   :desc "Rotate + scale a sprite frame (DrawTexturePro)" :controls "Q quit"}
   {:profile "background-scrolling" :cat :textures :desc "Parallax-scrolling cyberpunk street layers" :controls "Q quit"}
   {:profile "image-loading" :cat :textures   :desc "LoadImage (RAM) then LoadTextureFromImage (VRAM)" :controls "Q quit"}
   {:profile "blend-modes" :cat :textures     :desc "Four 2D blend modes over the cyberpunk street" :controls "SPACE mode · Q quit"}
   {:profile "particles-blending" :cat :textures :desc "Spark particles trail the mouse (alpha/additive)" :controls "SPACE blending · Q quit"}
   {:profile "mouse-painting" :cat :textures  :desc "A paint program on a RenderTexture canvas" :controls "LEFT paint · RIGHT erase · WHEEL brush · C clear · S save · Q"}
   {:profile "sprite-button" :cat :textures   :desc "A 3-state sprite button with a click sound" :controls "click the button · Q quit"}
   {:profile "sound-loading" :cat :audio   :desc "Play a WAV and an OGG sound"           :controls "SPACE wav · ENTER ogg · Q quit"}
   {:profile "bunnymark" :cat :textures       :desc "The classic bunny-spawning batching benchmark" :controls "LEFT spawn · P pause · Q quit"}
   {:profile "music-stream" :cat :audio    :desc "Stream an MP3 with pan/volume/progress controls" :controls "SPACE restart · P pause · L/R pan · U/D volume · Q"}
   {:profile "module-playing" :cat :audio  :desc "A chiptune XM module + pulsing circle waves" :controls "SPACE restart · P pause · U/D pitch · Q quit"}
   {:profile "sound-multi" :cat :audio     :desc "Overlapping sound playback via sound aliases" :controls "SPACE play (rapid = overlap) · Q quit"}
   {:profile "fog-of-war" :cat :textures      :desc "A tile map hidden by smooth fog of war" :controls "ARROWS move · Q quit"}
   {:profile "sound-positioning" :cat :audio :desc "Spatial audio around an orbiting 3D sphere" :controls "listen! · Q quit"}
   {:profile "geometric-shapes" :cat :models :desc "3D cubes/spheres/cylinders/capsules on a grid" :controls "Q quit"}
   {:profile "box-collisions" :cat :models  :desc "A player cube colliding with 3D obstacles" :controls "ARROWS move · Q quit"}
   {:profile "billboard-rendering" :cat :models :desc "Camera-facing billboards + an orbiting camera" :controls "watch · Q quit"}
   {:profile "waving-cubes" :cat :models    :desc "3375 rainbow cubes waving in 3D"        :controls "watch · Q quit"}
   {:profile "orthographic-projection" :cat :models :desc "Toggle perspective vs orthographic camera" :controls "SPACE toggle · Q quit"}
   {:profile "tesseract-view" :cat :models  :desc "A rotating 4D hypercube projected to 3D" :controls "watch · Q quit"}
   {:profile "rlgl-solar-system" :cat :models :desc "Sun/Earth/Moon via the rlgl matrix stack" :controls "watch · Q quit"}
   {:profile "camera-2d-mouse-zoom" :cat :core :desc "Pan + zoom-to-cursor a 2D camera" :controls "1/2 mode · LEFT drag · WHEEL/RIGHT zoom · Q"}
   {:profile "world-screen" :cat :core    :desc "A 2D label tracking a 3D cube (GetWorldToScreen)" :controls "watch · Q quit"}
   {:profile "tiled-drawing" :cat :textures   :desc "Tile a texture pattern with scale/rotation/color" :controls "click pattern/color · UP/DOWN scale · L/R rotation · SPACE reset · Q"}
   {:profile "font-loading" :cat :text    :desc "Load a BMFont and a TTF font (DrawTextEx)" :controls "SPACE = TTF · Q quit"}
   {:profile "font-filters" :cat :text    :desc "Scale a TTF word, switch texture filters" :controls "WHEEL size · 1/2/3 filter · L/R move · Q"}
   {:profile "font-spritefont" :cat :text :desc "Three colored sprite fonts from PNG atlases" :controls "watch · Q quit"}
   {:profile "sprite-fonts" :cat :text    :desc "A gallery of raylib's eight bundled sprite fonts" :controls "watch · Q quit"}
   {:profile "camera-3d" :cat :core       :desc "A red cube on a grid through a fixed 3D camera" :controls "watch · Q quit"}
   {:profile "picking-3d" :cat :core      :desc "Click a 3D box to pick it with a world-space ray" :controls "LEFT click box · Q quit"}
   {:profile "sprite-explosion" :cat :textures :desc "Click to play a 5x5 explosion spritesheet + sound" :controls "LEFT click · Q quit"}
   {:profile "input-multitouch" :cat :core :desc "A ball at every active touch/mouse point" :controls "touch/click · Q quit"}
   {:profile "sprite-stacking" :cat :textures :desc "A voxel booth from 122 stacked rotated slices" :controls "A/D spin · WHEEL separation · Q quit"}
   {:profile "npatch-drawing" :cat :textures  :desc "Stretchable 9-patch / 3-patch UI panels" :controls "move mouse · Q quit"}
   {:profile "input-virtual-controls" :cat :core :desc "An on-screen D-pad moving a player circle" :controls "hold LEFT on a button · Q quit"}
   {:profile "image-processing" :cat :textures :desc "Nine CPU image filters via pointer-taking Image* APIs" :controls "click / UP / DOWN · Q quit"}
   {:profile "image-drawing" :cat :textures   :desc "One texture composed from several CPU images" :controls "watch · Q quit"}
   {:profile "image-text" :cat :textures      :desc "Text baked into an image with a TTF font" :controls "SPACE = atlas · Q quit"}
   {:profile "image-rotate" :cat :textures    :desc "The logo rotated +45/+90/-90 in CPU memory" :controls "LEFT click / RIGHT cycle · Q quit"}
   {:profile "image-channel" :cat :textures   :desc "RGBA channels split + alpha-masked" :controls "watch · Q quit"}
   {:profile "image-kernel" :cat :textures    :desc "Sharpen/sobel/gaussian convolution kernels" :controls "watch · Q quit"}
   {:profile "cellular-automata" :cat :textures :desc "Wolfram rule cellular automaton, editable rule" :controls "click rule bits / presets · Q quit"}
   {:profile "magnifying-glass" :cat :textures :desc "A circular magnifier revealing hidden bunnies" :controls "move mouse · Q quit"}
   {:profile "to-image" :cat :textures        :desc "Round-trip an image VRAM<->RAM (LoadImageFromTexture)" :controls "watch · Q quit"}
   {:profile "polygon-drawing" :cat :textures :desc "A cat texture mapped onto a spinning polygon" :controls "watch · Q quit"}
   {:profile "raw-data" :cat :textures        :desc "A .raw pixel dump + a code-generated checkerboard" :controls "watch · Q quit"}
   {:profile "textured-curve" :cat :textures  :desc "A road texture swept along a draggable Bezier" :controls "drag points · SPACE curve · +/- width · L/R segments · Q"}
   {:profile "gif-player" :cat :textures      :desc "An animated GIF streamed frame-by-frame to a texture" :controls "RIGHT/LEFT change speed · Q"}
   {:profile "window-flags" :cat :core    :desc "Toggle window state flags live with a bouncing ball" :controls "F/R/D/H/N/M/U/T/A/V/B toggle flags · ESC"}
   {:profile "render-texture" :cat :core  :desc "A ball bouncing inside a rotated off-screen render texture" :controls "ESC"}
   {:profile "monitor-detector" :cat :core :desc "A scaled map of every attached monitor with its specs" :controls "ENTER next monitor · ESC"}
   {:profile "input-actions" :cat :core   :desc "Remappable abstract actions (WASD/arrows) via a keyset map" :controls "WASD/arrows move · SPACE fire · TAB swap keyset · ESC"}
   {:profile "highdpi-demo" :cat :core    :desc "Logical-points vs physical-pixels grids with live DPI scale" :controls "resize window · N next monitor · ESC"}
   {:profile "highdpi-testbed" :cat :core :desc "A HighDPI diagnostic overlay: grid, monitor/DPI info, crosshair" :controls "SPACE borderless · F fullscreen · ESC"}
   {:profile "textured-cube" :cat :models   :desc "Two rlgl textured 3D cubes from a shared atlas" :controls "ESC"}
   {:profile "directional-billboard" :cat :models :desc "A sprite-sheet billboard that turns as the camera orbits" :controls "ESC"}
   {:profile "random-sequence" :cat :core :desc "Colored bars in a random no-repeat permutation (LoadRandomSequence)" :controls "SPACE shuffle, UP/DOWN count, ESC"}
   {:profile "basic-voxel" :cat :models :desc "An 8x8x8 beige voxel grid; click to ray-pick and remove cubes" :controls "LEFT/RIGHT spin, click remove, R refill, ESC"}
   {:profile "rotating-cube" :cat :models :desc "A textured cube spinning on a tilted axis (rlgl matrix stack)" :controls "ESC"}
   {:profile "clipboard-text" :cat :core :desc "Type + cut/copy/paste with the system clipboard" :controls "type, CTRL+X/C/V/R/D, ESC"}
   {:profile "undo-redo" :cat :core :desc "A grid player with a 26-slot undo/redo ring buffer" :controls "ARROWS move, SPACE color, CTRL+Z/Y undo/redo, ESC"}
   {:profile "directory-files" :cat :core :desc "A keyboard file browser over the working directory" :controls "UP/DOWN select, ENTER open dir, BACKSPACE up, ESC"}
   {:profile "custom-logging" :cat :core :desc "A custom trace-log callback timestamps + tags every raylib log line" :controls "watch the console, ESC"}
   {:profile "drop-files" :cat :core :desc "Drag files onto the window to list their paths" :controls "drag & drop files, ESC"}
   {:profile "text-file-loading" :cat :core :desc "Load + word-wrap a text file, scroll it" :controls "mouse wheel scroll, ESC"}
   {:profile "rectangle-bounds" :cat :text :desc "Word-wrapped text in a mouse-resizable container" :controls "drag corner to resize, SPACE toggle wrap, ESC"}
   {:profile "compute-hash" :cat :core :desc "CRC32/MD5/SHA1/SHA256 + Base64 of typed text" :controls "type, BACKSPACE delete, ENTER compute, ESC"}
   {:profile "storage-values" :cat :core :desc "Save/load a score pair to a binary storage file" :controls "R random, ENTER save, SPACE load, ESC"}
   {:profile "keyboard-testbed" :cat :core :desc "An on-screen ENG-US keyboard highlighting held keys" :controls "hold any keys, mouse hover; close window to quit"}
   {:profile "input-gestures-testbed" :cat :core :desc "A gesture dashboard with log, indicators and protractor" :controls "tap/drag/swipe with the mouse, click Hide buttons, ESC"}
   {:profile "viewport-scaling" :cat :core :desc "A fixed game resolution scaled into a resizable window" :controls "resize window, click < > buttons, ESC"}
   {:profile "codepoints-loading" :cat :text :desc "Japanese text rasterized to a minimal TTF font atlas" :controls "SPACE toggle atlas view, ESC"}
   {:profile "unicode-ranges" :cat :text :desc "Grow a multilingual font atlas by unicode range" :controls "0-4 select range set, ESC"}
   {:profile "inline-styling" :cat :text :desc "Text with inline color style tags" :controls "watch the colors re-roll, ESC"}
   {:profile "unicode-emojis" :cat :text :desc "Click emojis for multilingual speech bubbles" :controls "hover + click emojis, SPACE re-roll, ESC"}
   {:profile "text-3d-drawing" :cat :text :desc "A bitmap font drawn as textured quads in 3D, waving the `~~World~~`-marked span" :controls "F1/F2 boundaries, F3 camera, arrows size/spacing, Home/End layers, Tab multicolor, click cube, ESC"}
   {:profile "strings-management" :cat :text :desc "Drag/slice/shatter/glue text particles; 1-6 run raylib's TextTo* fns" :controls "L-mouse grab/throw, R-mouse slice (+Shift shatter), M-mouse shake, L-Ctrl glue, 1-6 reset, ESC"}
   {:profile "shapes-textures-shader" :cat :shaders :desc "A grayscale fragment shader over shapes + a sprite" :controls "ESC"}
   {:profile "texture-outline" :cat :shaders :desc "A shader-drawn outline around a sprite" :controls "mouse wheel outline size, ESC"}
   {:profile "camera-3d-free" :cat :core :desc "A free-look 3D camera around a cube" :controls "mouse look, WHEEL zoom, wheel-press pan, Z retarget, ESC"}
   {:profile "camera-3d-first-person" :cat :core :desc "Walk a yard of random columns in first person" :controls "WASD + mouse, 1-4 camera mode, P projection, ESC"}
   {:profile "texture-waves" :cat :shaders :desc "A space texture rippled by an animated wave shader" :controls "ESC"}
   {:profile "camera-3d-split-screen" :cat :core :desc "Two players, two 3D cameras, split screen" :controls "W/S player 1, UP/DOWN player 2, ESC"}
   {:profile "camera-3d-fps" :cat :core :desc "A physics FPS controller with head-bob, lean and strafe-accel" :controls "WASD move, SPACE jump, Left-Ctrl crouch, mouse look, ESC"}
   {:profile "vr-simulator" :cat :core :desc "A 3D scene in stereo through a simulated Oculus Rift + lens-distortion shader" :controls "mouse look (first-person), ESC"}
   {:profile "automation-events" :cat :core :desc "A 2D platformer with input record/replay via AutomationEventList" :controls "LEFT/RIGHT move, SPACE jump, R reset, S record, A replay, ESC"}
   {:profile "input-gamepad" :cat :core :desc "A live controller diagram: buttons/sticks/triggers light up (Xbox/PS/generic)" :controls "plug in a gamepad, LEFT/RIGHT switch slot, click VIBRATE, ESC"}
   {:profile "julia-set" :cat :shaders :desc "A Julia set fractal computed in a fragment shader" :controls "mouse zoom/drift, 1-6 presets, LEFT/RIGHT speed, SPACE, R, F1, ESC"}
   {:profile "framebuffer-rendering" :cat :textures :desc "An observer camera watching a subject camera + frustum" :controls "WASD + mouse observer, R retarget, ESC"}
   {:profile "eratosthenes-sieve" :cat :shaders :desc "The Sieve of Eratosthenes per-pixel in a shader" :controls "ESC"}
   {:profile "mandelbrot-set" :cat :shaders :desc "The Mandelbrot set in a shader, deep-zoom presets" :controls "mouse zoom/drift, 1-6 presets, UP/DOWN iterations, R, F1, ESC"}
   {:profile "rounded-rectangle-shader" :cat :shaders :desc "SDF rounded rectangles (fill/shadow/border) in a fragment shader" :controls "Q/ESC quit"}
   {:profile "raymarching" :cat :shaders :desc "A raymarched SDF scene in a fragment shader, first-person fly-cam" :controls "WASD move, mouse look, Q/E down/up, ESC"}
   {:profile "color-correction" :cat :shaders :desc "A post-process shader tuning contrast/saturation/brightness of a picture" :controls "1-4 picture, Q/W A/S Z/X sliders, R reset, ESC"}
   {:profile "custom-uniform" :cat :shaders :desc "A mouse-steered swirl post-process over a render-textured 3D scene" :controls "move the mouse, ESC"}
   {:profile "ascii-rendering" :cat :shaders :desc "A post-process shader re-rendering the scene as ASCII glyphs" :controls "LEFT/RIGHT cell size, ESC"}
   {:profile "postprocessing" :cat :shaders :desc "Twelve full-screen post-process shaders cycled over a 3D scene" :controls "LEFT/RIGHT cycle effect, ESC"}
   {:profile "texture-rendering" :cat :shaders :desc "A blank texture painted and animated entirely by a fragment shader" :controls "ESC"}
   {:profile "multi-sample2d" :cat :shaders :desc "Two textures blended in a shader via a second sampler2D" :controls "LEFT/RIGHT divider, ESC"}
   {:profile "palette-switch" :cat :shaders :desc "Palette-indexed bands recolored by an ivec3-array shader uniform" :controls "LEFT/RIGHT palette, ESC"}
   {:profile "hot-reloading" :cat :shaders :desc "Hot-swap the reload.fs fragment shader while it runs" :controls "A auto-reload, CLICK reload once, ESC"}
   {:profile "spotlight-rendering" :cat :shaders :desc "Three spotlights alpha-masked over a star field + sprite swarm" :controls "move the mouse, ESC"}
   {:profile "font-sdf" :cat :text :desc "Bitmap vs SDF font scaling, the SDF drawn through a shader" :controls "MOUSE WHEEL scale, hold SPACE for SDF, ESC"}
   {:profile "depth-writing" :cat :shaders :desc "Inverted gl_FragDepth into a custom depth-texture framebuffer" :controls "ESC"}
   {:profile "depth-rendering" :cat :shaders :desc "The scene's depth buffer visualized through a shader" :controls "WASD + mouse look, Z retarget, ESC"}
   {:profile "hybrid-rendering" :cat :shaders :desc "Raymarched spheres + rasterized cubes in one depth-tested scene" :controls "ESC"}
   {:profile "texture-tiling" :cat :shaders :desc "A generated cube model with its texture tiled 3x3 by a shader" :controls "WASD + mouse look, Z retarget, ESC"}
   {:profile "model-loading" :cat :models :desc "The castle OBJ model loaded from disk, ray-pick selection" :controls "LEFT CLICK select, ESC"}
   {:profile "model-shader" :cat :shaders :desc "The watermill OBJ drawn grayscale via a material-bound shader" :controls "WASD + mouse look, ESC"}
   {:profile "heightmap-rendering" :cat :models :desc "Terrain generated from a grayscale heightmap image" :controls "ESC"}
   {:profile "cubicmap-rendering" :cat :models :desc "A cube maze generated from a tiny black-and-white image" :controls "P pause orbit, ESC"}
   {:profile "mesh-generation" :cat :models :desc "All nine procedural mesh generators, checked-textured" :controls "CLICK or LEFT/RIGHT cycle, ESC"}
   {:profile "first-person-maze" :cat :models :desc "Walk the cubicmap maze in first person, wall collision + radar" :controls "WASD + mouse look, ESC"}
   {:profile "basic-lighting" :cat :shaders :desc "A plane + cube lit by four toggleable colored point lights" :controls "Y/R/G/B toggle lights, ESC"}
   {:profile "fog-rendering" :cat :shaders :desc "Torus/cube/sphere models fading into exponential fog" :controls "UP/DOWN fog density, ESC"}
   {:profile "cel-shading" :cat :shaders :desc "A GLB car toon-shaded with quantized bands + outline" :controls "Z cel, C outline, Q/E bands, ESC"}
   {:profile "normalmap-rendering" :cat :shaders :desc "A spinning plane lit through a tangent-space normal map" :controls "N toggle, WASD light, UP/DOWN shininess, ESC"}
   {:profile "simple-mask" :cat :shaders :desc "An animated mask texture eats holes in two models' plasma skin" :controls "WASD + mouse look, ESC"}
   {:profile "vertex-displacement" :cat :shaders :desc "A plane mesh riding Perlin-noise waves in the vertex shader" :controls "WASD + mouse look, ESC"}
   {:profile "rlgl-compute" :cat :shaders :desc "Game of Life stepped entirely on the GPU by compute shaders" :controls "LEFT draw, RIGHT erase, WHEEL brush, ESC"}
   {:profile "mesh-instancing" :cat :shaders :desc "Ten thousand lit cubes in one draw call (DrawMeshInstanced)" :controls "ESC"}
   {:profile "lightmap-rendering" :cat :shaders :desc "A plane lit by a baked lightmap through a second UV channel" :controls "ESC"}
   {:profile "shadowmap-rendering" :cat :shaders :desc "Real shadows: an animated robot under the shadowmapping algorithm" :controls "ARROWS light, F screenshot, ESC"}
   {:profile "basic-pbr" :cat :shaders :desc "The rusty car under physically-based rendering (PBR maps)" :controls "1-4 toggle lights, ESC"}
   {:profile "deferred-rendering" :cat :shaders :desc "A three-target G-buffer + full-screen deferred lighting pass" :controls "Y/R/G/B lights, 1-4 views, ESC"}
   {:profile "game-of-life" :cat :shaders :desc "Conway's Life on a 2048x2048 world: pan/zoom, presets, draw mode" :controls "1-9/0 presets, R/P/D mode, Z/X zoom, F/S speed, drag pan, ESC"}
   {:profile "loading-gltf" :cat :models :desc "The animated glTF robot, switchable animations" :controls "LEFT/RIGHT animation, ESC"}
   {:profile "yaw-pitch-roll" :cat :models :desc "Fly a WWI plane through pitch/yaw/roll, easing back to level" :controls "UP/DOWN pitch, LEFT/RIGHT roll, A/S yaw, ESC"}
   {:profile "mesh-picking" :cat :models :desc "A mouse ray picks the closest quad/triangle/sphere/box/mesh hit" :controls "mouse aim, RIGHT CLICK camera, ESC"}
   {:profile "loading-iqm" :cat :models :desc "The classic IQM guy walking on loop" :controls "ESC"}
   {:profile "loading-m3d" :cat :models :desc "The Cesium Man in Model3D format, skeleton view on SPACE" :controls "LEFT/RIGHT animation, hold SPACE skeleton, ESC"}
   {:profile "loading-vox" :cat :models :desc "Four MagicaVoxel models under a fly camera + voxel lighting" :controls "CLICK cycle, WASD/ARROWS move, MIDDLE-drag rotate, WHEEL zoom, ESC"}
   {:profile "animation-timing" :cat :models :desc "The robot with a playback timeline + adjustable speed" :controls "LEFT/RIGHT anim, UP/DOWN speed, P pause, ESC"}
   {:profile "bone-socket" :cat :models :desc "A hat, sword and shield riding the greenman's skeleton bones" :controls "T/G anim, F/H rotate, 1/2/3 equip, ESC"}
   {:profile "point-rendering" :cat :models :desc "Up to 10 million points: GPU point mode vs per-point draws" :controls "UP/DOWN count, SPACE method, ESC"}
   {:profile "skybox-rendering" :cat :models :desc "A cubemap skybox drawn from inside a unit cube" :controls "WASD + mouse look, ESC"}
   {:profile "animation-blending" :cat :models :desc "SPACE cross-fades the robot between two animations" :controls "SPACE blend, LEFT/RIGHT + UP/DOWN anims, Z/X + N/M speeds, P, ESC"}
   {:profile "animation-blend-custom" :cat :models :desc "Per-bone blending: walking legs + attacking upper body" :controls "SPACE toggle blend mode, ESC"}
   {:profile "decals" :cat :models :desc "Click to splat logo decals clipped onto a character's surface" :controls "CLICK splat, RMB-hold camera, H model, C clear, ESC"}
   {:profile "raw-stream" :cat :audio :desc "A sine wave generated sample-by-sample into a raw audio stream" :controls "UP/DOWN frequency, LEFT/RIGHT pan, ESC"}
   {:profile "mixed-processor" :cat :audio :desc "A DSP distortion callback on the whole audio mix" :controls "LEFT/RIGHT distortion, SPACE coin, ESC"}
   {:profile "stream-effects" :cat :audio :desc "Stackable lowpass + delay effects on one music stream" :controls "F lowpass, D delay, SPACE restart, P pause, ESC"}
   {:profile "stream-callback" :cat :audio :desc "A pull-model synth: sine/square/triangle/sawtooth on demand" :controls "LEFT/RIGHT wave, UP/DOWN frequency, ESC"}
   {:profile "amp-envelope" :cat :audio :desc "An ADSR amplitude envelope on a tone, with a live shape graph" :controls "hold SPACE play, Q/A W/S E/D R/F params, ESC"}
   {:profile "spectrum-visualizer" :cat :audio :desc "A live FFT spectrum of the music through a shader" :controls "ESC"}
   {:profile "screen-buffer" :cat :textures :desc "The classic DOS fire effect in a palette-indexed buffer" :controls "ESC"}])

(defn- find-example [profile]
  (first (filter (fn [e] (= profile (:profile e))) examples)))

;; ---------------------------------------------------------------------------
;; Runners
;; ---------------------------------------------------------------------------

(defn run-example! [profile]
  (if-let [{:keys [desc controls]} (find-example profile)]
    (do
      (ensure-submodules!)
      (header (str "▶ raylib-examples: " profile))
      (info desc)
      (when controls (info (str "Controls: " controls)))
      ;; No bwrap on macOS, so the native build runs with --disable-sandbox.
      (lein! "raylib-examples" "with-profile" (str "+" profile) "run" "--disable-sandbox"))
    (do
      (err (str "Unknown example: " (pr-str profile)))
      (info "Run 'bb examples' to see the list.")
      (System/exit 1))))

;; Run one example for at most `secs` seconds, then kill its process tree and
;; return. Closing the window / pressing Q ends it early. Never throws.
(defn- run-example-timed! [profile secs]
  (let [proc (p/process [@lein "with-profile" (str "+" profile) "run" "--disable-sandbox"]
                        {:dir "raylib-examples" :inherit true})]
    (try
      (when-not (.waitFor ^Process (:proc proc) secs java.util.concurrent.TimeUnit/SECONDS)
        (p/destroy-tree proc)
        (.waitFor ^Process (:proc proc)))
      (catch Exception e
        (err (str profile ": " (ex-message e)))
        (p/destroy-tree proc)))))

(defn run-all!
  "Run every example for a few seconds each (a smoke-test / demo reel).
  Optional first arg = seconds per example (default 15)."
  [secs-arg]
  (ensure-submodules!)
  (let [secs (or (some-> secs-arg parse-long) 15)]
    (header (str "▶ running all " (count examples) " examples · " secs "s each"))
    (info "Press Q or close a window to skip to the next one early.")
    (info "Each example needs ~11s to launch (JVM + jank start) before its")
    (info "window opens, so keep secs above ~13; the first cold run of each")
    (info "also compiles - use a bigger value then, e.g.  bb run-all 40")
    (doseq [{:keys [profile desc]} examples]
      (println)
      (println (c :magenta (str "  ▶ " profile)) "-" desc)
      (run-example-timed! profile secs))
    (println)
    (ok (str "Cycled through " (count examples) " examples."))))

;; ---------------------------------------------------------------------------
;; Misc
;; ---------------------------------------------------------------------------

(defn clean! []
  (doseq [d (fs/glob "." "*/target")]
    (fs/delete-tree (str d)))
  (ok "Removed all */target build dirs."))

(defn print-examples []
  (header "🎮 b12n-raylib-jnk - raylib examples (jank)")
  (doseq [{:keys [profile desc controls]} examples]
    (println (str "  " (c :cyan (format "bb %-16s" profile)) " " desc))
    (when controls (println (str "  " (apply str (repeat 20 " ")) (c :magenta controls)))))
  (println)
  (info "Run one:      bb <name>        e.g.  bb starfield")
  (info "Or by arg:    bb run <name>")
  (info "Also:         bb check · bb nrepl · bb clean")
  (println))

;; ---------------------------------------------------------------------------
;; bb info - a grouped cheat-sheet (easier to scan than the flat `bb tasks`)
;; ---------------------------------------------------------------------------

;; Display order + section titles for the example categories.
(def ^:private cat-order
  [[:core     "core: window, input, cameras, files"]
   [:shapes   "shapes: 2D drawing, easing, rlgl"]
   [:text     "text: fonts, unicode, layout"]
   [:textures "textures: images, sprites, render textures"]
   [:models   "models: meshes, 3D, OBJ/GLB"]
   [:shaders  "shaders: GLSL, uniforms, postprocess, lighting"]
   [:audio    "audio: sounds, music streams"]
   ;; Not a raylib category: original examples demonstrating jank/C++ interop
   ;; itself. Kept separate so the raylib port counts stay comparable upstream.
   [:interop  "interop: jank/C++ mechanics, not raylib ports"]])

(defn- truncate [s n]
  (if (> (count s) n) (str (subs s 0 (- n 1)) "…") s))

(defn- info-line [task desc]
  (println (str "  " (c :cyan (format "bb %-26s" task)) " " (truncate desc 66))))

(defn- info-section [title rows]
  (println)
  (println (c :bold (str "=== " title " ===")))
  (println)
  (doseq [[task desc] rows] (info-line task desc)))

(defn print-info []
  (doseq [[cat title] cat-order]
    (let [rows (filter (fn [e] (= cat (:cat e))) examples)]
      (when (seq rows)
        (info-section (str "Examples: " title " (" (count rows) ")")
                      (map (juxt :profile :desc) rows)))))
  (info-section "Build"
                [["clean" "Remove */target build dirs"]])
  (info-section "Dev"
                [["check" "Offline gates: syntax, registration, EDN (fast, no compile)"]
                 ["lint" "clj-kondo over every .jank source"]
                 ["nrepl" "Start a jank nREPL (cpp/ interop works in it)"]])
  (info-section "Docs (maintainer)"
                [["record" "Batch-record a demo GIF per example (needs screen-grab)"]
                 ["docs-sync" "Rebuild + republish the guide and site (bb docs-sync [--no-push])"]])
  (info-section "Meta"
                [["examples" "Flat list of every example, with controls"]
                 ["run <name>" "Run one example by argument"]
                 ["run-all [secs]" "Cycle through every example (demo reel / smoke test)"]
                 ["info" "This grouped cheat-sheet"]])
  (println)
  (info "Run any example with `bb <name>` · controls: `bb examples` · all tasks: `bb tasks`")
  (println))
