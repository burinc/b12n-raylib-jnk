(defproject org.clojars.kylc/jank-raylib-sys "6.0-SNAPSHOT"
  :license {:name "MPL 2.0"
            :url  "https://www.mozilla.org/en-US/MPL/2.0/"}
  :plugins [[org.jank-lang/lein-jank "2026.06-1"]]
  :middleware [leiningen.jank/middleware]
  :build-dependencies [[org.clojars.kylc/jank-build-cmake "0.1-SNAPSHOT"]]
  :verbatim-paths ["raylib" "include"])
