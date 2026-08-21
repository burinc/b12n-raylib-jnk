## What this changes

<!-- One or two sentences. If it adds an example, name it and its upstream
     raylib source (e.g. shapes/shapes_bouncing_ball) if it's a port. -->

## Verification

CI runs `bb check` — reader syntax, the four registration touchpoints, and
the EDN data files. It cannot compile anything (jank ships no current prebuilt
binary), so the headless smoke run is still what proves an example works, and
that part is on you. See [CONTRIBUTING.md](https://github.com/burinc/b12n-raylib-jnk/blob/main/CONTRIBUTING.md#before-you-open-a-pr).

- [ ] `bb check` passes locally
- [ ] Ran the affected example(s) headlessly and got `exit=142` with a
      failure-marker grep of `0`

<!-- Paste the two lines here: -->
```
exit=
failure-marker count=
```

- [ ] Also ran any other example my change could plausibly affect
      (`bb run-all [secs]` reels through the whole suite)

## If this adds an example

<!-- Skip this section otherwise. All four are required, or the example won't
     appear in bb info / bb run-all. See docs/guide/porting-workflow.md -->

- [ ] Source namespace under `raylib-examples/src/raylib_examples/`
      (underscored file, kebab namespace)
- [ ] `:profiles` entry in `raylib-examples/project.clj`
- [ ] `bb <name>` task in `bb.edn`
- [ ] Registry row in `bb/helpers.clj`, including its `:cat`
- [ ] Moved from the queue into the ported table in
      `raylib-examples/README.md`, with the progress counts bumped
- [ ] Docstring names the upstream C source it was ported from
- [ ] `SetConfigFlags FLAG_WINDOW_HIGHDPI` before `InitWindow`
- [ ] Comments are ASCII only (an em-dash trips the jank lexer)

<!-- If the interesting path is behind input (a hover, a key, a generation
     count), a 25s headless run won't reach it. Force the state, run, revert
     before committing — and say so below. -->

## Environment you tested on

- OS / arch (`uname -sm`):
- jank version (`jank --version`):
- Any system-wide raylib installed? (`brew list --versions raylib`):

## Notes for the reviewer

<!-- Anything surprising, any deliberate deviation from the C original, any
     key-gated path you forced to exercise it, anything you're unsure about. -->
