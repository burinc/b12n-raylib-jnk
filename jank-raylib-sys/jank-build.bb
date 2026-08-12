(require '[babashka.fs :as fs]
         '[jank.build.cmake :as cmake])

(let [src-dir (fs/path (:src-dir *input*) "raylib")
      out-dir (:out-dir *input*)
      input   (assoc *input* :src-dir src-dir)]
  ;; OPENGL_VERSION 3.3 — the GLSL-330 feature set every example except
  ;; rlgl-compute needs. NOT 4.3: macOS's native GL backend caps at 4.1
  ;; (Apple never shipped a 4.3 profile; GLFW rejects the version request
  ;; unconditionally, regardless of the forward-compat hint patched in
  ;; jank-raylib-sys/patches/) — a global 4.3 build breaks window creation
  ;; for every example on macOS, not just the one that needs compute
  ;; shaders. rlgl-compute (the only example needing OpenGL 4.3 compute
  ;; shaders: rlLoadShaderProgramCompute, SSBOs, rlComputeShaderDispatch)
  ;; does not run against this build on any platform — see the "Known
  ;; limitations" section of the root README.
  (cmake/build input {:defines {"BUILD_EXAMPLES" false
                                "BUILD_SHARED_LIBS" true
                                "OPENGL_VERSION" "3.3"}})

  ;; CMake installs shared libs to lib64 on some Linux distros and lib on
  ;; macOS/others. Emit whichever directory actually exists.
  (let [lib-dir (first (filter fs/directory?
                               [(fs/path out-dir "lib64")
                                (fs/path out-dir "lib")]))]
    (println (str "jank-build::include-dir=" (fs/path out-dir "include")))
    ;; jank-helper headers shipped with the wrapper (jank_rlights.h, ...)
    (println (str "jank-build::include-dir=" (fs/path (:src-dir *input*) "include")))
    (println (str "jank-build::link-dir=" lib-dir))
    (println (str "jank-build::link-library=" "raylib"))))
