# jank-raylib-sys

**This is no longer a `-sys` wrapper.** raylib comes from the official
[`org.jank-lang.commons/raylib-sys`](https://github.com/jank-lang/commons)
package on Clojars, declared in `raylib-examples/project.clj`.

What remains here is the vendored `raylib` checkout, kept for one reason:
**example assets**. 101 of the examples load shaders, models, textures, fonts
and audio from `raylib/examples/*/resources/`, roughly 70 MB that the
published jar does not carry. `bb` fetches the submodule on first use.

Nothing here is built, installed, or patched any more. The wrapper that used
to live here, its `project.clj`, `jank-build.bb`, the `jank_rlights.h`
helper header and a macOS OpenGL patch, is gone:

- the build is the official package's job now
- `jank_rlights.h` was replaced by `raylib-examples/src/raylib_examples/rlights.jank`,
  a plain jank namespace, once it became clear the C was only there to take
  the address of a value for `SetShaderValue`
- the macOS patch only ever affected raylib's `RL_OPENGL_43` branch, which
  this project never takes

The directory keeps its name only to avoid rewriting the asset paths in those
101 examples; renaming it is a reasonable follow-up.
