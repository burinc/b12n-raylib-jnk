# The missing JVM surface, and other odds and ends

jank has no `Math/*`, `format`, `rand-int`, char literals, or `String`
methods — this page covers what replaces them, what's actually available
(more of `clojure.core`/`clojure.string` than it looks), and a few
miscellaneous gotchas that save a recompile.

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
Source of truth: jank's own [`compiler+runtime/src/jank/clojure/`](https://github.com/jank-lang/jank/tree/main/compiler%2Bruntime/src/jank/clojure)
(`core.jank`, `string.jank`) — what is implemented there is what you can call.

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

## Compile-time cost of deeply nested loops

A triple-nested `doseq` with a fat body (`waving_cubes.jank`'s
15x15x15 cube lattice) compiles in ~3-4 MINUTES, versus ~30-60s for a
typical example module. The generated C++ for nested seq iteration with
a large inlined body appears to grow multiplicatively. Budget smoke-test
alarms accordingly (the standard 40s alarm kills such a build
mid-compile and looks like a hang - re-run with a 260s+ alarm before
diagnosing). If compile time matters more than faithfulness, hoist the
inner body into a `defn` taking only jank values.
