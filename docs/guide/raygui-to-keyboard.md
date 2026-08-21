# raygui → keyboard

Many official shapes examples build their UI with **raygui** sliders and
checkboxes. raygui is not part of the `raylib-sys` package, so those examples get a
keyboard-driven port: sliders become held-key adjustments, checkboxes become
toggle keys, and the raygui panel becomes plain `DrawText` lines showing live
values. The pattern was established by `easings_testbed.jank` and refined
across `ring_drawing.jank`, `circle_sector_drawing.jank`,
`rounded_rectangle.jank`, and `recursive_tree.jank`.

## The adj helper

One small clamp helper covers every slider:

```clojure
(defn adj
  "Adjust v by step while dn/up are held, clamped to lo..hi."
  [v dn up step lo hi]
  (let [v (if dn (- v step) v)
        v (if up (+ v step) v)]
    (if (< v lo) lo (if (> v hi) hi v))))
```

Call it once per slider in the frame `let`, feeding `IsKeyDown` (continuous,
slider-like) or `IsKeyPressed` (stepped, for coarse values like a depth of
1..10):

```clojure
(let [sa   (adj sa (cpp/IsKeyDown cpp/KEY_LEFT) (cpp/IsKeyDown cpp/KEY_RIGHT) 2.0 -450.0 450.0)
      segs (adj segs (cpp/IsKeyDown cpp/KEY_O) (cpp/IsKeyDown cpp/KEY_P) 0.5 0.0 100.0)
      depth (adj depth (cpp/IsKeyPressed cpp/KEY_Z) (cpp/IsKeyPressed cpp/KEY_X) 1.0 1.0 10.0)]
  ...)
```

Pick the step so a slider's full range takes ~2–4 seconds of holding at
60 fps (range / step / 60).

## Checkboxes → toggle keys

```clojure
ring? (if (cpp/IsKeyPressed cpp/KEY_R) (not ring?) ring?)
```

## The panel → text lines

Keep the C's panel geometry (the divider line and tinted rectangle), and
replace each `GuiSliderBar` with a `DrawText` line naming the value, its
current reading, and its keys:

```clojure
(cpp/DrawText (str "StartAngle: " (fmt1 sa) "  (LEFT/RIGHT)") 560 40 10 cpp/DARKGRAY)
(cpp/DrawText (str "[R] Draw Ring: " (if ring? "ON" "OFF")) 560 320 10 cpp/DARKGRAY)
```

`fmt1`/`fmt2` are the small decimal formatters (there's no `format` in
jank; see [jvm-surface-gaps.md](jvm-surface-gaps.md)). Keep derived
readouts from the C, like the MANUAL/AUTO segments mode, including its color
switch: `(if (>= segs min-segs) cpp/MAROON cpp/DARKGRAY)`.

## Key allocation conventions

- LEFT/RIGHT and DOWN/UP for the two most-adjusted values (angles, primary
  size).
- A/S, Z/X, O/P as additional +/- pairs (down-key on the left of the
  physical keyboard pair).
- Single letters for toggles, echoing the raygui checkbox label
  (`[R] Draw Ring`, `[B] Bezier`).
- Q quits (the repo-wide convention) UNLESS the example needs Q or types
  free text:
  - `easings_testbed.jank` keeps the C's Q/W duration keys, so only ESC
    quits there. Document that clearly in every registry surface.
  - `input_box.jank` accepts typed characters, so it quits on Q only while
    the mouse is OUTSIDE the box.
- Always list the full mapping in three places: the namespace docstring, the
  `bb/helpers.clj` controls string, and the README table row. Drift between
  those surfaces is a real failure mode a code review caught (the root
  README's easings-testbed row omitted the quit key while helpers said
  "ESC quit").

## Porting checklist for a 🎛️ example

1. Map every `GuiSliderBar` to a key pair + `adj` call (note range and a
   sensible step).
2. Map every `GuiCheckBox` to a toggle key.
3. Keep the panel background/divider, replace controls with text lines.
4. Preserve derived readouts (mode text, computed minimums) and their
   colors.
5. Check Q is actually free before binding it to quit.
6. Smoke-test, registering, committing per
   [porting-workflow.md](porting-workflow.md).
