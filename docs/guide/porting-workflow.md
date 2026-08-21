# Porting workflow

This guide is the end-to-end process for porting one official raylib example
to jank, as practiced across the first 50 ports. Follow it top to bottom and
a port lands as one self-contained, tested, registered commit.

## 1. Pick from the queue

`raylib-examples/README.md` keeps the prioritized queue under "Not yet
ported". Markers tell you the cost up front:

- *(no marker)*: pure raylib, port directly (these are all done now)
- 🖼️: uses a `RenderTexture` (supported; see `lines_drawing.jank`)
- 🎛️: uses **raygui** controls; swap them for keyboard controls
  (see [raygui-to-keyboard.md](raygui-to-keyboard.md))
- ⚙️: uses the low-level `rlgl` API. Turns out to work directly:
  `rlgl.h` is installed next to `raylib.h` and its functions live in
  `libraylib`, so `(:include "rlgl.h")` is all it takes (proof:
  `rlgl_triangle.jank`)

## 2. Port from the definitive C source

The authoritative originals are raylib's own example programs, under
[`examples/{core,shapes,text,...}/`](https://github.com/raysan5/raylib/tree/master/examples)
(easing functions in `examples/shapes/reasings.h`). You already have a
checkout: the vendored submodule at `jank-raylib-sys/raylib/examples/` is
pinned to the same raylib commit the `raylib-sys` package builds, so it is the copy whose
API actually matches.

Port from the C, never from an intermediate binding. Ports-of-ports drift;
if you find an existing Clojure or Lisp translation of an example, treat it
as a hint and check it against the C.

Keep formulas and update ordering faithful to the C (a code review of the
easings testbed verified all 28 easing formulas term-by-term against
`reasings.h`; that fidelity is the standard). When jank forces a deviation
(no mutable arrays, capped pool sizes, keyboard instead of raygui), say so in
the namespace docstring:

```clojure
(ns raylib-examples.bullet-hell
  "raylib [shapes] example - bullet hell, ported to jank.
  ...controls...
  Based on raylib/examples/shapes/shapes_bullet_hell.c
  (jank-native: bullets are a vector of maps rebuilt per frame instead
  of a mutable C array, and MAX-BULLETS is 5000 rather than the C's
  500000 calloc headroom - the reset-at-cap behavior is the same.)"
  (:include "raylib.h" "math.h"))
```

Docstring format: title line, controls, `Based on <C file>`, then a
`(jank-native: ...)` note for intentional deviations. File names use
underscores, namespaces use kebab: `bullet_hell.jank` →
`raylib-examples.bullet-hell`. Comments must be ASCII (an em-dash trips the
lexer).

Before writing a new construct, grep the existing examples for a sibling that
already uses it; every proven idiom has at least one committed example, and
the [guide pages](index.md) index them by theme.

Every example sets `(cpp/SetConfigFlags cpp/FLAG_WINDOW_HIGHDPI)` before
`InitWindow` so windows scale with the monitor DPI (drawing stays at the
C's logical resolution); include it in new ports. `SetConfigFlags` ORs
each call into its state, so it stacks with `FLAG_MSAA_4X_HINT` etc.
Exceptions: `window-flags` (a flag-state demo) and the two `highdpi-*`
examples (which manage DPI flags themselves).

## 3. Register in all four places (same commit)

1. `raylib-examples/project.clj`: a `:profiles` entry
2. `bb.edn`: a `bb <name>` task
3. `bb/helpers.clj`: a row in the `examples` registry vector, including
   its `:cat` (the raylib category keyword, which drives the `bb info` grouping)
4. `raylib-examples/README.md`: move the example from the queue into the
   ported table, bump the progress counts

The repo-root `README.md` carries no per-example table; it delegates the
catalog to `raylib-examples/README.md`, so nothing there needs touching.

Do not defer any of these; the registration IS part of the port.

## 4. Smoke-test headless

**Check paren balance BEFORE the first compile.** A jank compile costs
30-60 s; a strict reader loop is instant and catches the classic
extra-close-paren at the `recur` tail (which cost one wasted compile on
`input_gestures_testbed`). jank sources read fine with the JVM reader:

```sh
cd raylib-examples
clojure -M -e "
(let [text (slurp \"src/raylib_examples/<name>.jank\")
      r (java.io.PushbackReader. (java.io.StringReader. text))]
  (try
    (loop [forms []]
      (let [form (read {:read-cond :allow :eof ::eof} r)]
        (if (= form ::eof)
          (println :total-forms (count forms) :ok)
          (recur (conj forms form)))))
    (catch Exception e (println :ERROR (.getMessage e)))))"
```

`:total-forms N :ok` means balanced; `:ERROR Unmatched delimiter` means
fix before compiling. (A strict `read` loop, not `read-string` - the
single-form and `(do ...)`-wrapped variants both miss trailing
imbalance.)

macOS has no `timeout` and this harness blocks a foreground `sleep`, so the
reliable one-shot is a perl alarm:

```sh
cd raylib-examples
perl -e 'alarm 25; exec @ARGV' \
  lein with-profile +<name> run --disable-sandbox > /tmp/run.log 2>&1
echo "exit=$?"
grep -icE "error|exception|Mismatched|small_real|small_integer|invalid object" /tmp/run.log
```

Reading the result:

- `exit=142` (SIGALRM): the example compiled, opened its window, and
  survived 25 s of the frame loop. This is the success signal.
- `exit=1` or an early exit: compilation or startup failed; the log has the
  compiler error.
- The `grep` must print `0`. The markers are the jank/raylib failure
  vocabulary: `Mismatched` (if-branch type clash), `small_real` /
  `small_integer` (int/real API mismatch), `invalid object` (bad conversion).

First compile of a new example takes ~60–75 s; cached recompiles ~15 s.

### Key-gated paths need a probe run

A 25 s headless run only exercises code that runs unconditionally. If the
interesting path hides behind input (a hover, a key, a generation count),
temporarily force the state, run, then revert before committing:

- `penrose_tile.jank`: forced `gen 2` + prebuilt tokens to exercise the
  L-system, then reverted.
- `input_box.jank`: forced `on-text? true` and seeded the name from the
  ASCII table so the caret/`MeasureText` path ran, then reverted.

Note the probe in the commit message so reviewers know the gated path was
actually executed.

## 5. Commit

- One example per commit when practical (registry rows interleave if you
  batch two; fine occasionally, but singles keep history greppable).
- Subject: `raylib-examples: port <official_source_name>`.
- Body: the interesting jank-native decisions, and any NEW interop lesson the
  port surfaced.
- Stage files by explicit path; never `git add -A`/`.`/`-u`.
- If the port surfaced a new lesson, add it to the relevant
  [guide page](index.md) in the same commit.

## Debugging a port that won't compile

Bisect: cut the example down to a minimal draw loop, then add one construct
back at a time; each `lein run` recompiles the one changed module in
~30–60 s. The compiler error vocabulary and what each message actually means
is in [type-checking-and-coercion.md](type-checking-and-coercion.md) and
[cpp-interop-toolbox.md](cpp-interop-toolbox.md).
