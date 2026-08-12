(require '[babashka.fs :as fs]
         '[jank.build.cmake :as cmake])

(let [src-dir (fs/path (:src-dir *input*) "raylib")
      out-dir (:out-dir *input*)
      input   (assoc *input* :src-dir src-dir)]
  ;; OPENGL_VERSION 4.3 = the same GL 3.3 feature set PLUS compute shaders
  ;; (rlLoadShaderProgramCompute, SSBOs, rlComputeShaderDispatch); the
  ;; GLSL-330 examples are unaffected.
  (cmake/build input {:defines {"BUILD_EXAMPLES" false
                                "BUILD_SHARED_LIBS" true
                                "OPENGL_VERSION" "4.3"}})

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
