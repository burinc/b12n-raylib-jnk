# Type-checking and coercion

> **Cast and type semantics live in the jank book**:
> [Casting between native types](https://book.jank-lang.org/cpp-interop/cast.html)
> and [Working with native types](https://book.jank-lang.org/cpp-interop/native-types.html).
> Below are the coercion traps 209 raylib ports actually hit, each naming
> the example that proves it.

## if/cond branch type-checking

jank type-checks every `if` branch. `cond`/`case` expand with an implicit
trailing `nil`, which clashes with a native value type:
`Mismatched 'if' branch types 'Color' and 'nil'`.

- `cond` returning **jank** values (maps, keywords, strings, vectors) is
  fine: state machines and `[x vx]`-vector returns all work.
- Picking a **native** value needs hand-nested `if`s where every branch ends
  in a concrete value: `(if on-text? cpp/RED cpp/DARKGRAY)` as a call
  argument is fine (`input_box.jank`, `bullet_hell.jank`).

**`and`/`or` are `if`s too, and a native struct-field bool clashes with a jank
bool.** `(and native-bool jank-bool)` expands to
`(let [a native-bool] (if a jank-bool a))`, so its two implicit branches are
`jank-bool` (plain `bool`) and `native-bool`. A struct-field read like
`(.-hit collision)` types as `bool &` (a reference), not `bool`, so the
combined form errors with `Mismatched 'if' branch types 'bool' and 'bool &'`.
Only a boolean-combining macro forces the unify; `(.-hit c)` straight as an
`if`/`when` condition is fine (`picking_3d.jank`). Fix: hand-nest the `if`s so
the native bool is only ever a condition, never a returned branch value
(`basic_voxel.jank`: `(if (.-hit coll) (if (< d best-d) ...) ...)`).

## Numeric traps

| Trap | Symptom | Fix | Proof |
|---|---|---|---|
| `mod`/`quot`/`rem` return reals | `expected integer found small_real` at an int param, or a broken `nth` | wrap in `(int ...)` | everywhere; `writing_anim.jank` |
| `cpp/float` wants a REAL arg | `expected real found small_integer` | `(cpp/float (+ 0.0 n))` | `lines_drawing.jank` boxes `GetMouseX` |
| `(/ int int)` shape is unreliable | subtle | precompute constants or `(int (quot ...))` | `window_letterbox.jank` uses `(int (/ GAME-H 10))` |
| `min`/`max` reject a raw C double | `invalid operands to binary expression` deep in math.hpp | box first with `(+ 0.0 x)`, or clamp with `if` | `dashed_line.jank`; isolated to min/max only; `+ - * / < <= =` all take raw doubles (`ellipse_collision.jank`) |
| `min`/`max` also reject a boxed int mixed with `(int ...)`'s unboxed i64 | same math.hpp template error (`oref<small_integer>` vs `long long`) | clamp with `if` comparisons instead (`<` / `>=` take the mix fine) | `first_person_maze.jank` cell clamp |
| `str` with >10 args + a raw `(int ...)` in the tail | codegen error: `member reference base type 'i64' ... .erase()` | build long strings in two `str` calls of ≤10 args | `bullet_hell.jank` status line |
| `cpp/float` on an ALL-native arithmetic chain | codegen error: `convert<float>::from_object`, `no known conversion from 'f64'` | route one operand through a boxed source, e.g. a vector lookup: `(nth [0 fh (* 2 fh)] state)` | `sprite_button.jank` frame offset |
| `(int cpp/KEY_*)` on a C **enum** constant | template error: `member reference base type 'const KeyboardKey'` in `to_int` | cast the enum to a native int first with `(cpp/int cpp/KEY_*)`; the result then boxes fine into jank maps/vectors and round-trips through `int` params like `IsKeyDown` | `keyboard_testbed.jank` ROW data |

Boxing idiom: `(+ 0.0 x)` turns a raw C int/float/double into a jank real;
`(int x)` truncates to a jank integer. `cpp/GetFrameTime`, `cpp/GetTime`,
`cpp/GetMouseWheelMove` returns are routinely boxed at the binding site.

The all-native chain trap: `(+ 0.0 expr)` only *boxes* when some input is
already a jank object. If every value derives from literals and native reads
(`(.-height tex)`), the chain stays an unboxed `f64` and `cpp/float`'s
generated `from_object` cannot take it. A `loop`/`recur` variable feeding the
chain boxes it, which is why `sprite_animation.jank`'s near-identical frame
math never hit this. Any collection op re-boxes; `nth` is the cheapest.

## Constructing colors and structs from data

- `cpp/Color` (the struct ctor) wants native `unsigned char` fields, and jank
  ints don't match: `No matching call to 'Color' constructor`. Struct ctors
  demand exact native types; **functions** coerce jank ints happily.
- So build Colors through *functions*: pack RGBA into an int and call
  `cpp/GetColor`, as in `(cpp/GetColor (+ (* r 16777216) (* g 65536) (* b 256) a))`
  (`camera_2d.jank` skyline, `recursive_tree.jank` panel), or use
  `cpp/ColorFromHSV`, `cpp/ColorLerp`, `cpp/Fade`. The C idiom
  `(Color){0,0,0,200}` becomes `(cpp/Fade cpp/BLACK (cpp/float 0.784))`
  (`bullet_hell.jank`).
- Struct ctors compose inline: `cpp/Camera2D` takes two nested `cpp/Vector2`
  plus two `cpp/float` args, passed straight to `cpp/BeginMode2D`
  (`camera_2d.jank`).
- **Struct `int` fields need `(cpp/int n)` casts.** A jank int literal reaches
  the ctor as `small_integer_ref` and fails the braced-init: `No matching call
  to 'NPatchInfo' constructor ... argument 1 having type ...small_integer_ref`.
  Wrap each one (`npatch_drawing.jank`):
  `(cpp/NPatchInfo (cpp/Rectangle ...) (cpp/int 12) (cpp/int 40) (cpp/int 12)
  (cpp/int 12) cpp/NPATCH_NINE_PATCH)`. The trailing enum converts on its own.
  Unlike `unsigned char`, `int` fields have this escape hatch, so they need no
  `GetColor`-style function detour.
- Field access works with `.-`: `(.-texture render-texture)`,
  `(.-x measured-vec2)`, and on pointer returns `(.-tm_hour lt)`
  (`lines_drawing.jank`, `words_alignment.jank`, `digital_clock.jank`).

