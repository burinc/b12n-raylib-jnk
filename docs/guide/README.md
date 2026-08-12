# Guide

What we learn as we port the official [raylib examples](https://www.raylib.com/examples.html)
to [jank](https://jank-lang.org) (native Clojure on C++/LLVM), written down so
the next port doesn't relearn it. The repo-root `AGENTS.md` holds the terse
rules an AI agent needs in-context; these guides are the fuller story with
worked examples and the history of how each lesson was learned.

| Guide | What it covers |
|---|---|
| [porting-workflow.md](porting-workflow.md) | The end-to-end process for porting one example: source of truth, file layout, five-place registration, the headless smoke test, key-gated probe runs, commit conventions |
| [jank-interop-lessons.md](jank-interop-lessons.md) | Every jank/C++ interop sharp edge we've hit, organized by theme, each citing the example that proves it |
| [raygui-to-keyboard.md](raygui-to-keyboard.md) | The pattern for porting raygui-based examples (sliders/checkboxes) to keyboard controls with a text panel |

## Where things live

- `raylib-examples/src/raylib_examples/*.jank` — one self-contained namespace
  per example. The examples double as the test suite for the interop lessons:
  when a guide cites `bullet_hell.jank`, that file is the running proof.
- `raylib-examples/README.md` — porting progress, the ported table, and the
  prioritized "not yet ported" queue with blocker markers.
- `AGENTS.md` (repo root) — the distilled rules, kept short because agents
  load it into every session.
