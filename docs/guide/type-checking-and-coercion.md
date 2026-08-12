# Type-checking and coercion

jank's compiler enforces the native-value-lifetime rule
([`native-value-lifetimes.md`](native-value-lifetimes.md)) strictly at
compile time. This page covers the sharp edges that fall out of that
strictness: `if`/`cond` branch type-checking, numeric coercion between
jank and native number types, and constructing native structs from jank
data. Each lesson names the committed example that proves it.

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

