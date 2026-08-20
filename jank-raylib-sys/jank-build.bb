;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this file,
;; You can obtain one at https://mozilla.org/MPL/2.0/.
;;
;; Derived from Kyle Cesare's lein-jank-playground (jank-raylib-sys), which is
;; MPL 2.0. The rest of this repository is under the zlib License; MPL 2.0 is a
;; file-level copyleft, so this file keeps its original terms. See ../NOTICE.

(require '[babashka.fs :as fs]
         '[babashka.process :as proc])

;; cmake-build is inlined from org.clojars.kylc/jank-build-cmake 0.1-SNAPSHOT
;; (jank/build/cmake.bb, Copyright (c) Kyle Cesare, MPL 2.0 - the same licence
;; this project carries; see ../NOTICE). It is vendored rather than pulled in
;; through :build-dependencies so that a fresh clone builds without fetching a
;; floating SNAPSHOT from a third party's Clojars account. Behaviour is
;; unchanged from the upstream fn, including the "install" target default.
(defn- cmake-build
  [{:keys [src-dir build-dir out-dir optimization-level static-build]}
   {:keys [defines target] :or {target "install"}}]
  (let [defaults {"CMAKE_BUILD_TYPE"     (if (pos? optimization-level) "Release" "Debug")
                  "CMAKE_INSTALL_PREFIX" out-dir
                  "BUILD_SHARED_LIBS"    (if static-build "OFF" "ON")}
        d-flags  (map (fn [[k v]] (str "-D" (name k) "=" v))
                      (merge defaults defines))]
    (proc/shell (concat ["cmake"] d-flags ["-B" build-dir src-dir]))
    (proc/shell ["cmake" "--build" build-dir "--target" target])))

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
  (cmake-build input {:defines {"BUILD_EXAMPLES" false
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
