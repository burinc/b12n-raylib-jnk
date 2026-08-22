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

It is boxing. An empty loop costs ~810 ns an iteration because `(inc i)` and
`(< i n)` allocate.

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

## Two traps that only bite native-slot loops

**An inner `loop` binding that shadows an outer native-int slot initialises
to 0**, not to the outer value. These differ by one identifier:

```clojure
(loop [j j bit ...] ...)     ; -> [512 512 512 512 512]
(loop [jj j bit ...] ...)    ; -> [512 256 768 128 640]  correct
```

The idiomatic Clojure form is the broken one. Rename the inner binding. The
boxed equivalent is correct, so this needs native slots. Silent: a wrong
permutation still produced a spectrum that looked like a spectrum
(`spectrum_visualizer.jank`).

**`recur` assigns loop slots in order, without temporaries.** An argument that
reads a slot written earlier in the same `recur` sees the new value:

```clojure
(recur (inc i) (f x) (g x))   ; if arg 2 writes x's slot, arg 3 reads the NEW x
```

Break the alias by binding first, or by forcing a copy with `(int x)`.
`unicode_ranges.jank` carries the fix.

## Related

`cpp/aget` yields a **reference**, not a copy. Binding `tr` to `re[i]` and then
writing `re[i]` changes `tr` underneath. Force a copy with a `cpp/cast` where
the C would have copied a struct.

Native pointers cannot be captured by a closure: `dotimes` and `doseq` over one
fail where `loop`/`recur` works. Same rule as
[`native-value-lifetimes.md`](native-value-lifetimes.md).
