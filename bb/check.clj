(ns check
  "Offline gates for the example suite.

  Deliberately does NOT compile anything. jank publishes no current prebuilt
  binary and building it means building Clang, which is far beyond what a
  per-PR job should do, so a real compile gate is not available to us. What
  IS available is everything that goes wrong before the compiler is even
  reached, and in practice that is most of it: unbalanced parens, and an
  example that was added to some of its four registration sites but not all
  of them.

  Every check here runs in well under a second, so `bb check` stays cheap
  enough to run before every commit."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [helpers :as h]))

(def ^:private RESET "\033[0m")
(def ^:private RED "\033[0;31m")
(def ^:private GREEN "\033[0;32m")

(defn- src-path
  "raylib-examples/src/raylib_examples/foo_bar.jank for profile foo-bar."
  [profile]
  (str "raylib-examples/src/raylib_examples/" (str/replace profile "-" "_") ".jank"))

(defn- reader-errors
  "Read every .jank source to EOF with a strict top-level loop.

  Not `read-string`: that consumes only the first form, so a file whose
  imbalance is anywhere after the first defn passes it. Reading to EOF is
  what catches a trailing stray paren, which is the common shape of this
  mistake."
  []
  (->> (sort (map str (fs/glob "raylib-examples/src" "**/*.jank")))
       (keep (fn [f]
               (let [r (java.io.PushbackReader. (java.io.StringReader. (slurp f)))]
                 (try
                   (loop []
                     (when-not (= ::eof (read {:read-cond :allow :eof ::eof} r))
                       (recur)))
                   nil
                   (catch Exception e
                     (str (fs/file-name f) " - " (.getMessage e)))))))
       vec))

(defn- registration-errors
  "Every registry example must exist in all four registration sites.

  This is the check that earns its keep: the four touchpoints are documented
  but easy to half-do, and missing one fails in a different way each time -
  no `bb <name>` task, or an example absent from the catalog, or a profile
  lein cannot resolve."
  []
  ;; project.clj and bb.edn are PARSED, not grepped. A line-anchored regex
  ;; looked right and silently mis-reported the very first profile, because
  ;; it shares its line with `:profiles {`. Reading the actual data has no
  ;; such blind spot.
  (let [proj-opts (apply hash-map (drop 3 (read-string (slurp "raylib-examples/project.clj"))))
        profiles  (set (map name (keys (:profiles proj-opts))))
        tasks     (set (map name (keys (:tasks (clojure.edn/read-string (slurp "bb.edn"))))))
        cat       (slurp "docs/guide/example-catalog.md")]
    (->> h/examples
         (mapcat (fn [{:keys [profile]}]
                   (cond-> []
                     (not (fs/exists? (src-path profile)))
                     (conj (str profile " - no source at " (src-path profile)))

                     (not (contains? profiles profile))
                     (conj (str profile " - no :profiles entry in raylib-examples/project.clj"))

                     (not (contains? tasks profile))
                     (conj (str profile " - no `bb " profile "` task in bb.edn"))

                     (not (str/includes? cat (str "`" profile "`")))
                     (conj (str profile " - not listed in docs/guide/example-catalog.md")))))
         vec)))

(defn- edn-errors
  "The EDN the tooling reads at runtime, parsed here instead of at use time."
  []
  (->> ["scripts/demo_manifest.edn" "docs/demos/ledger.edn"]
       (keep (fn [f]
               (when (fs/exists? f)
                 (try (clojure.edn/read-string (slurp f)) nil
                      (catch Exception e (str f " - " (.getMessage e)))))))
       vec))

(defn- orphan-errors
  "A runnable .jank source with no registry row is invisible: no task, no
   catalog entry, and nothing runs it. Catches the reverse of
   registration-errors.

   Runnable means it defines -main. Library namespaces that examples require
   (rlights, say) legitimately have no registry row, and `-main` separates
   the two exactly: every one of the registered examples defines it and no
   library does."
  []
  (let [known (set (map (comp src-path :profile) h/examples))]
    (->> (sort (map str (fs/glob "raylib-examples/src" "**/*.jank")))
         (remove known)
         (filter (fn [f] (re-find #"\(defn -main" (slurp f))))
         (mapv (fn [f] (str f " - defines -main but has no bb/helpers.clj registry row"))))))

(defn- report [label errs]
  (if (seq errs)
    (do (println (str RED "✗ " label RESET))
        (doseq [e errs] (println (str "    " e)))
        false)
    (do (println (str GREEN "✓ " label RESET)) true)))

(defn run!
  "Run every offline gate. Exits non-zero if any fails."
  []
  (println "Checking the example suite (offline - no jank compile)\n")
  (let [results [(report (str "reader syntax - " (count (fs/glob "raylib-examples/src" "**/*.jank")) " .jank files parse to EOF")
                         (reader-errors))
                 (report (str "registration - " (count h/examples) " examples present in all four sites")
                         (registration-errors))
                 (report "no orphan sources" (orphan-errors))
                 (report "EDN data files parse" (edn-errors))]]
    (println)
    (if (every? true? results)
      (println (str GREEN "All checks passed." RESET))
      (do (println (str RED "Some checks failed." RESET))
          (System/exit 1)))))
