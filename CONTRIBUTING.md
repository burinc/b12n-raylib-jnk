# Contributing

Thanks for taking an interest. This is a suite of
[raylib](https://github.com/raysan5/raylib) examples written in
[jank](https://jank-lang.org) — a native Clojure dialect that compiles to
native code through C++/LLVM. There is no JVM at runtime; raylib is reached
directly as C++ through jank's `cpp/` interop. jank does have a REPL, and
`lein repl` from `raylib-examples/` gives you a jank nREPL with `cpp/`
interop live in it — see [Using the REPL](#using-the-repl).

New examples are welcome — the suite is deliberately mechanical to grow, and
`raylib-examples/README.md` keeps a queue of what is not ported yet.

## Setting up

```sh
git clone --recurse-submodules git@github.com:burinc/b12n-raylib-jnk.git
cd b12n-raylib-jnk
bb install          # builds raylib and installs jank-raylib-sys into ~/.m2
bb basic-window     # should open a window
```

You need a recent `jank`, the `lein-jank` Leiningen plugin, a C++ compiler,
CMake, and [babashka](https://babashka.org). `bb info` is the cheat-sheet for
everything else. Full setup notes:
[`docs/guide/getting-started.md`](docs/guide/getting-started.md).

Two environment gotchas worth knowing up front, because neither fails in an
obvious way:

- **Use a Homebrew `lein`.** A mise/asdf Leiningen shim can be broken — it
  tries to download a standalone jar that isn't there. `bb/helpers.clj`
  already prefers `/usr/local/bin/lein`, then `/opt/homebrew/bin/lein`;
  override with the `LEIN` environment variable. If you invoke `lein`
  yourself, `export PATH="/usr/local/bin:$PATH"` first.
- **`lein-jank` must be `2026.06-1` or newer** in every `project.clj`. 0.7
  lacks the native build middleware entirely.

### The raylib submodule is meant to look dirty

`git status` will permanently show `jank-raylib-sys/raylib` as modified. That
is intentional, not something to clean up: every `bb` invocation applies
`jank-raylib-sys/patches/macos-opengl43-forward-compat.patch` to the
submodule working tree. See that directory's `README.md` for what the patch
does and why it is a patch rather than a fork. Please don't commit the
submodule pointer as part of an unrelated change.

## Before you open a PR

This project has no automated lint, format, or compile gate yet — jank has no
released binary suitable for CI, so there is nothing to run in Actions and
nothing to run locally beyond the example itself. The gate is the headless
smoke run, and it is on you to do it.

```sh
cd raylib-examples
perl -e 'alarm 25; exec @ARGV' \
  lein with-profile +<name> run --disable-sandbox > /tmp/run.log 2>&1
echo "exit=$?"
grep -icE "error|exception|Mismatched|small_real|small_integer|invalid object" /tmp/run.log
```

`exit=142` (SIGALRM) is the success signal: the example compiled, opened its
window, and survived 25 s of the frame loop. The `grep` must print `0`. macOS
has no `timeout`, hence the `perl` alarm.

Check paren balance *before* the first compile — a jank compile costs 30–60 s
and a strict reader loop is instant. If your example's interesting path hides
behind input (a hover, a key, a generation count), force the state, run, then
revert before committing, and say so in the commit message. Both recipes:
[`docs/guide/porting-workflow.md § 4`](docs/guide/porting-workflow.md#4-smoke-test-headless).

Please run the examples your change could plausibly affect, not only the one
you added. `bb run-all [secs]` reels through the whole suite.

## Using the REPL

jank ships a REPL with nREPL support, and it works in this repo:

```sh
bb nrepl        # or: cd raylib-examples && lein repl
```

It is a genuine jank REPL, not a JVM Clojure one — `cpp/` interop evaluates
in it:

```clojure
(println :interop (cpp/int 42))
;; :interop 42
```

`lein` writes `raylib-examples/.nrepl-port`, which most editor tooling picks
up on its own (CIDER and Conjure are both known to work with jank's nREPL).

This matters because a jank compile costs 30–60s. The smoke-test loop above
is still what proves an example, but the REPL is much the faster place to
work out a formula or check what a raylib call returns before you commit to
a rebuild.

## Adding an example

Port from the **definitive C source** in
[`raysan5/raylib`](https://github.com/raysan5/raylib) (`examples/<category>/`),
not from any intermediate binding. Keep the physics and formulas faithful, and
note any deliberate simplification in the docstring.

One new example touches exactly four places, all in the same commit:

**[`docs/guide/porting-workflow.md § 3`](docs/guide/porting-workflow.md#3-register-in-all-four-places-same-commit)**

In short: a `project.clj` profile, a `bb.edn` task, a `bb/helpers.clj`
registry row (including its `:cat`), and the catalog table in
`raylib-examples/README.md`. Registration is part of the port, not a
follow-up.

Two conventions:

- **Every example sets `FLAG_WINDOW_HIGHDPI`** before `InitWindow`, so
  windows scale with the monitor's DPI while drawing stays at the C's logical
  800x450. `SetConfigFlags` ORs, so it stacks with MSAA and friends.
- **Comments must be ASCII.** A stray em-dash trips the jank lexer with
  `lex/invalid-unicode`. Use `-`.

## Writing jank that compiles

jank is native, so there is no Java interop — `Math/sin`, `rand-int`,
`format`, and `.indexOf` do not exist. Before writing anything non-trivial,
read [`docs/guide/native-value-lifetimes.md`](docs/guide/native-value-lifetimes.md).
It covers what the jank/C++ boundary actually enforces, which is
**convertibility, not scope**: trait-convertible types (integral types,
bools, C strings, `std::string`) cross freely, while a type without a
conversion trait — `Color`, `Vector2`, `Camera2D` — cannot cross a fn
boundary *implicitly*.

Most examples here thread a plain jank id and resolve the native value
inline at the call, which is the right call for a per-frame draw. When a
value genuinely needs to outlive the fn that made it, jank's opaque boxes
(`cpp/new` + `cpp/box` + `cpp/unbox`) carry it through the runtime; run
`bb opaque-boxes` for a worked example.

The rest of [`docs/guide/`](docs/guide/index.md) covers the C-interop toolbox,
int-vs-real coercion (`mod`/`quot` return reals; `cpp/float` wants a real),
and which raylib APIs are reachable today.

### Helper signatures

For a jank `defn`/`defn-` taking **more than three parameters**, prefer a
single map argument with `{:keys [...]}` destructuring over a long positional
list, and call it with a map literal:

```clojure
;; prefer
(defn ellipses-collide? [{:keys [c1x c1y rx1 ry1 c2x c2y rx2 ry2]}] ...)
(ellipses-collide? {:c1x 10 :c1y 20 :rx1 5 :ry1 5 :c2x 40 :c2y 20 :rx2 8 :ry2 8})

;; over
(defn ellipses-collide? [c1x c1y rx1 ry1 c2x c2y rx2 ry2] ...)   ; easy to transpose
```

Map destructuring compiles in jank — it's ordinary Clojure, not a native
feature. This is a soft preference, not a gate, and it has two exceptions:

- **Not for `cpp/` interop calls.** C and C++ functions are positional-only.
  jank resolves overloads by argument position and type at compile time and
  has no named-argument call syntax, so `(cpp/DrawRectanglePro rec origin rot
  color)` stays positional — there is no keyword form to reach for.
- **Not for well-known positional math signatures.** The Penner easing
  convention `[t b c d]` (time, begin, change, duration) reads better
  positionally to anyone fluent in it; a map would be noise.

## Demo GIFs

You don't need to record anything. Every GIF under `docs/demos/` is committed
and `docs/demos/README.md` is generated from `scripts/demo_manifest.edn`.

`bb record` drives a `screen-grab` capture CLI that is not publicly
released, so recording is maintainer-only — the task will tell you so rather
than failing obscurely.

If your example would benefit from a particular input sequence in its demo,
add an entry for it in `scripts/demo_manifest.edn` and mention it in your PR;
a maintainer will record it and commit the GIF.

Publishing the guide as a site is likewise maintainer-only (`bb docs-sync`,
which needs a sibling checkout and AWS credentials), so a docs change in your
PR goes live when a maintainer next syncs — you don't need to do anything.

## Licensing

This project is released under the zlib License — see [`LICENSE`](LICENSE).
That is the same license raylib itself uses. By contributing, you agree your
contribution is licensed under those terms.

Two files under `jank-raylib-sys/` are the exception and remain MPL 2.0
(`project.clj` and `jank-build.bb`, both derived from
[`lein-jank-playground`](https://github.com/kylc/lein-jank-playground)). If
your change touches either, it stays MPL — see
[`jank-raylib-sys/README.md`](jank-raylib-sys/README.md). Everything else,
including all the examples, is zlib.

If your example is a port of an upstream raylib example, **name the original
C file in its docstring** (e.g.
`Based on raylib/examples/shapes/shapes_bouncing_ball.c`) and in the README
table, so the attribution stays traceable — that is what zlib asks for in
return. [`NOTICE`](NOTICE) records the rest.
