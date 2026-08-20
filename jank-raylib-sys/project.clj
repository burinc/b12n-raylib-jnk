;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this file,
;; You can obtain one at https://mozilla.org/MPL/2.0/.
;;
;; Derived from Kyle Cesare's lein-jank-playground (jank-raylib-sys), which is
;; MPL 2.0. The rest of this repository is under the zlib License; MPL 2.0 is a
;; file-level copyleft, so this file keeps its original terms. See ../NOTICE.

(defproject net.b12n/jank-raylib-sys "6.0-SNAPSHOT"
  :license {:name "MPL 2.0"
            :url  "https://www.mozilla.org/en-US/MPL/2.0/"}
  :plugins [[org.jank-lang/lein-jank "2026.06-1"]]
  :middleware [leiningen.jank/middleware]
  :verbatim-paths ["raylib" "include"])
