# Patches applied to the vendored `raylib` submodule

`bb/helpers.clj`'s `ensure-submodules!` applies exactly one patch from this
directory — `macos-opengl43-forward-compat.patch` — by its hardcoded
filename; it does not glob every `.patch` file here. Whether the patch's
changes are already present in the working tree is checked with
`git apply --reverse --check` on every `bb` invocation, independent of the
submodule-fetch check, so the patch lands correctly no matter how
`raylib`'s working tree was populated (the README's
`git clone --recurse-submodules`, a manual
`git submodule update --init --recursive`, or `bb`'s own first fetch) and
the check is a safe no-op once the patch is already applied.

## `macos-opengl43-forward-compat.patch`

Fixes a pre-existing raylib bug (confirmed present in `raysan5/raylib` at
the pinned commit `dbc56a87da87d973a9c5baa4e7438a9d20121d28`, not introduced
by this repo): `src/platforms/rcore_desktop_glfw.c`'s `RL_OPENGL_43` branch
is missing the `#if defined(__APPLE__)` GLFW forward-compat guard that the
neighboring `RL_OPENGL_33` branch already has.

**Currently inert under this repo's default build.** `jank-build.bb` builds
`jank-raylib-sys` at `OPENGL_VERSION "3.3"` by default (see the root
README's "Known limitations" section), so `rlGetVersion()` never returns
`RL_OPENGL_43` and the patched branch, while compiled into the binary, is
never taken. The patch is kept as insurance for the documented manual
`OPENGL_VERSION "4.3"` override path, where it fixes a real, independently
confirmed bug: without it, the `RL_OPENGL_43` branch always requests a
non-forward-compatible context, which macOS never grants. **This alone
does not get the 4.3 override working on macOS, even with the patch
applied** — GLFW's context-creation code performs a separate,
unconditional post-creation check (actual version obtained vs. version
requested) that fails regardless of the forward-compat hint, because
Apple's GL backend cannot grant an actual 4.3 context at all (it caps out
at 4.1 unconditionally). Both facts were confirmed by reading GLFW's own
vendored source and by live rebuild-and-run tests: applying only this
patch under `OPENGL_VERSION "4.3"` still fails with the exact same
`Requested OpenGL version 4.3, got version 4.1` error the patch was meant
to fix. The patch is still worth keeping — it's a genuine, correctly
targeted fix mirroring the working `RL_OPENGL_33` branch's own pattern —
but by itself it does not make the 4.3 override path usable on macOS. That
ceiling is separate and unfixable by any patch to this file.

This is a patch file rather than a submodule commit because the submodule
tracks upstream `raysan5/raylib` directly — we don't own that history and
aren't forking it for one guard clause. If upstream fixes this, the patch
will fail to apply (`git apply --check` will say so) and can be dropped.
