# Patches applied to the vendored `raylib` submodule

`bb/helpers.clj`'s `ensure-submodules!` applies every `.patch` file in this
directory automatically, once, right after the first
`git submodule update --init --recursive` (guarded by the same
`CMakeLists.txt`-exists check that guards the submodule fetch itself — a
fresh clone gets patched once; subsequent `bb` invocations are no-ops here).

## `macos-opengl43-forward-compat.patch`

Fixes a pre-existing raylib bug (confirmed present in `raysan5/raylib` at
the pinned commit `dbc56a87da87d973a9c5baa4e7438a9d20121d28`, not introduced
by this repo): `src/platforms/rcore_desktop_glfw.c`'s `RL_OPENGL_43` branch
is missing the `#if defined(__APPLE__)` GLFW forward-compat guard that the
neighboring `RL_OPENGL_33` branch already has. Since `jank-build.bb`
hardcodes `OPENGL_VERSION "4.3"` for every example (needed by
`rlgl-compute`), this breaks window creation on macOS for every example,
not just that one — GLFW reports
`Requested OpenGL version 4.3, got version 4.1` and raylib fails to
initialize its platform.

The fix mirrors the existing `RL_OPENGL_33` branch's own pattern exactly.

This is a patch file rather than a submodule commit because the submodule
tracks upstream `raysan5/raylib` directly — we don't own that history and
aren't forking it for one guard clause. If upstream fixes this, the patch
will fail to apply (`git apply --check` will say so) and can be dropped.
