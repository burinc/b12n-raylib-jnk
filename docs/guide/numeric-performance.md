# Numeric performance: boxing, not interop

> The `cpp/` operators themselves are in the jank book:
> [the cpp namespace](https://book.jank-lang.org/cpp-interop/cpp-ns.html).
> This page is one measured result and what follows from it.

## The result

A 1024-point FFT, same algorithm, three ways:

| | ms per call |
|---|---|
| C, covering strictly more work (window + FFT + dB pass) | 0.0208 |
| jank, ordinary arithmetic | 72.54 |
| jank, `cpp/` operators on `cpp/int` / `cpp/double` | **1.03** |

**70x**, changing nothing but whether the numbers are boxed.

Decomposed over 12000 iterations:

| | ns per iteration |
|---|---|
| empty `loop`/`recur`, bound is a `def` var | 809.6 |
| empty `loop`/`recur`, bound is a local | 809.1 |
| empty `loop`/`recur`, native int counter | **64.9** |
| one float array touch via `cpp/aget` / `cpp/=` | **21.8** |
| one `cpp/cos` | 148 |

Two intuitions this kills:

- **It is not var lookup.** 809.6 against 809.1 is noise.
- **It is not C++ interop.** A native array touch is the cheapest thing
  measured, an order of magnitude below one boxed arithmetic operation.
  Interop is the fast path, not the slow one.

It is boxing. An empty loop costs a fixed amount an iteration because
`(inc i)` and `(< i n)` allocate.

> **Re-measurement note (2026-08-25).** The ~810 ns figure in the table above
> did not reproduce at the same `-O2` on either jank binary on this box - the
> 2026-07-15 jank-0.1-alpha build or the Homebrew jank 0.1 release. An empty
> boxed `loop`/`recur` whose bound is a `def` var measures **37 ns** an
> iteration over 100 x 12000 iterations on both, and **51 ns cold / 44 ns
> warm** for a single 12000-iteration run - tens of ns, not hundreds. Our
> own re-run of this probe on a later jank build, recorded in
> [issue #4](https://github.com/burinc/jank/issues/4), reported 14.5 ns for
> the same row - the same order of magnitude. The original harness could not
> be reconstructed, so the table is left as recorded rather than silently
> rewritten, but **treat the boxed floor as tens of ns an iteration**. The
> `cpp/aget` and `cpp/cos` rows were not re-measured. The *shape* of the
> finding - boxing dominates, interop is the fast path - is unchanged; only
> the magnitude is smaller, which makes the "when it matters" thresholds
> below conservative rather than wrong.

## Unboxed loop locals: the init is not enough

jank does support unboxed `loop` locals, and they are unboxed by
**initialising them unboxed**: `(loop [i (cpp/int 0)] ...)`.

The trap is that this buys nothing on its own. Unboxedness does not propagate
through ordinary Clojure operators - `(inc i)` and `(< i n)` box an unboxed
local straight back up, so the loop costs what a fully boxed loop costs. Three
1,000,000-iteration accumulator loops with the bound read from a `def` var, so
the optimiser cannot fold them:

| loop | alpha | 0.1 |
|---|---|---|
| `[i 0 acc 0]`, `inc` / `<` / `+` | 41.9 | 33.2 |
| `[i (cpp/int 0) acc (cpp/int 0)]`, still `inc` / `<` / `+` | 42.4 | 31.9 |
| `[i (cpp/int 0) acc (cpp/int 0)]`, `cpp/+` / `cpp/<` throughout | eliminated | eliminated |

ns per iteration, on the 2026-07-15 alpha and the Homebrew 0.1 release.

The middle row is the point: an unboxed init with boxed operators is a
rounding error away from the fully boxed loop - on 0.1 it even measures
marginally faster, which is the same statement about noise. The third row
cannot be quoted as a per-iteration cost at all: at `-O2` a genuinely
unboxed loop is closed-formed away by the optimiser, which is itself the
evidence that nothing in it allocates.

**So the rule is not "unbox the init", it is "use `cpp/` operators
throughout".** The recipe below already does this; the init is necessary but
not sufficient.

## When it matters

Multiply your iteration count by ~810 ns. That is the floor before the loop
body does anything.

| iterations/frame | boxed floor | verdict |
|---|---|---|
| ~2500 | ~2 ms | fine (`camera_3d_fps`'s checkerboard) |
| ~90000 | ~73 ms | rewrite (`screen_buffer`) |
| 10M | ~8 s | not a jank problem (`point_rendering` stays C) |

Below a few thousand iterations a frame, write ordinary jank. Above it,
measure before assuming either way.

## The recipe

Native ints and doubles for indices and accumulators, `cpp/` operators
throughout, arrays via `cpp/MemAlloc` + `cpp/unsafe-cast`:

```clojure
(let [p (cpp/unsafe-cast (:* cpp/float) (cpp/MemAlloc (cpp/cast cpp/uint (* n 4))))]
  (loop [i (cpp/int 0)]
    (when (cpp/< i (cpp/int n))
      (cpp/= (cpp/aget p i) (cpp/cast cpp/float (cpp/* (cpp/cast cpp/double (cpp/aget p i))
                                                       (cpp/double 0.5))))
      (recur (cpp/+ i (cpp/int 1))))))
```

`MemAlloc` does not zero, unlike `RL_CALLOC`. Zero it yourself if the
algorithm assumes it (`screen_buffer.jank`, `spectrum_visualizer.jank`).

Worked examples: `screen_buffer.jank` (fire simulation, 90k cells twice a
frame), `spectrum_visualizer.jank` (1024-point FFT a frame).

## Two loop traps

**An inner `loop` binding that shadows an outer `loop` binding may silently
initialise to zero/empty instead of the outer value.** These differ by one
identifier:

```clojure
(loop [j j bit ...] ...)     ; -> [512 512 512 512 512]
(loop [jj j bit ...] ...)    ; -> [512 256 768 128 640]  correct
```

The idiomatic Clojure form is the broken one. Rename the inner binding.

Seen twice: a native `cpp/int` counter in `spectrum_visualizer.jank`, and a
**boxed jank vector** accumulator in `decals.jank`, where an inner loop
counted 3666 elements while the outer received 0. So it is not about native
types. In both cases the inner loop **recurs**; an inner loop that returns
without recurring has behaved correctly. Which of those is load-bearing is
not yet established.

Silent either way: a wrong permutation still produced a spectrum that looked
like a spectrum.

**`recur` assigns loop slots in order, without temporaries.** An argument that
reads a slot written earlier in the same `recur` sees the new value:

```clojure
(recur (inc i) (f x) (g x))   ; if arg 2 writes x's slot, arg 3 reads the NEW x
```

Break the alias by binding first, or by forcing a copy with `(int x)`.
`unicode_ranges.jank` carries the fix.

## Related

`cpp/aget` yields a **reference**, not a copy: it compiles to the C++ subscript
operator and the result is bound with `auto &&`, which on a `double *` deduces
`double &`. Binding `tr` to `re[i]` and then writing `re[i]` changes `tr`
underneath, so the textbook three-line swap silently degenerates into a copy
(`a[0]=20 a[1]=20` where you wanted `20 / 10`). This is not a struct-only
concern; the case that cost the most here was a plain `double *` swap in an FFT
bit reversal. Force the copy at the binding: both
`(cpp/cast cpp/double (cpp/aget a i))` and `(cpp/double (cpp/aget a i))` work.

The book states the same rule for member access, "Whenever a member is
accessed, you will get a reference to it, not a copy"
(`cpp-interop/native-values.md`), but says nothing about `aget`, which is where
it is easiest to miss.

A native value with no trait conversion cannot enter a jank collection, so a
`doseq` over a seq of them fails with `There is no implicit conversion from
'<T>' to 'jank::runtime::object_ref'`. Same rule as
[`native-value-lifetimes.md`](native-value-lifetimes.md).

This page previously said "native pointers cannot be captured by a closure, so
`dotimes`/`doseq` over one fail where `loop`/`recur` works". That was wrong on
both counts and was corrected on 2026-08-23: a `dotimes` indexing through a
native pointer works, and so does a real closure
(`(mapv (fn [i] (cpp/aget p (cpp/int i))) ...)`). The constraint is the
collection boundary, not closure capture.
