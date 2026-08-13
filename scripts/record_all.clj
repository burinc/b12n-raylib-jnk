#!/usr/bin/env bb
;; record_all.clj — batch-record a GIF for every example.
;;
;;   bb record                              ; record everything not up to date
;;   bb record --only bouncing-ball,starfield  ; subset (comma-separated ids)
;;   bb record --force                      ; ignore the ledger
;;   bb record --dry-run                    ; show the plan
;;
;; bb record (in bb.edn) wraps: bb scripts/record_all.clj
;;
;; Ported from b12n-rljlt/scripts/record_all.clj. jank-specific changes:
;;
;; - Every example ultimately runs as a process literally named `jank`
;;   (lein spawns java, which spawns the jank binary — confirmed live via
;;   `ps`) — so capture/wait-for-window target `--app jank`, not a PID.
;; - `--app jank` is ambiguous whenever more than one jank process is
;;   running (confirmed live: "error: multiple pids for `jank`" after a
;;   leftover process). kill-stray-jank! runs before AND after every
;;   recording — p/destroy-tree alone only reaches the lein/java parent
;;   this script directly spawns, not the jank grandchild that owns the
;;   window.
;; - The whole batch unlinks a stray Homebrew-installed raylib
;;   (`brew unlink raylib`) before recording starts and relinks it
;;   (`brew link raylib`) after, success or failure — 8 of the 208
;;   in-scope examples won't compile with it linked (see
;;   ~/.claude/projects/-Users-choomnuanb-dev-b12n-raylib-jnk/memory/
;;   jank-homebrew-raylib-shadowing.md for the mechanism).
;; - The registry comes from bb/helpers.clj's `examples`/`cat-order`
;;   (this project's existing single source of truth for bb info/bb
;;   examples), not a separate examples-registry.clj file.

(ns record-all
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [helpers :as h]
            [record-gif :as rec])
  (:import [java.security MessageDigest]
           [java.util.concurrent Executors TimeUnit]))

(def spec
  {:manifest {:coerce :string
              :default "scripts/demo_manifest.edn"}
   :out-dir  {:coerce :string
              :default "docs/demos"}
   :ledger   {:coerce :string
              :default "docs/demos/ledger.edn"}
   :only     {:coerce :string
              :default nil
              :desc "Comma-separated example ids"}
   :force    {:coerce :boolean
              :default false}
   :dry-run  {:coerce :boolean
              :default false}
   :pool     {:coerce :long
              :default 2
              :desc "Concurrent encoder threads"}
   :readme   {:coerce :string
              :default "docs/demos/README.md"}})

;; ---------------------------------------------------------------- helpers

(defn sha256 [path]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (fs/read-all-bytes path))
         (map #(format "%02x" %))
         (apply str))))

(defn have? [bin]
  (zero? (:exit (p/shell {:out nil
                          :err nil
                          :continue true} "which" bin))))

;; ---------------------------------------------------------------- process hygiene

(defn kill-stray-jank!
  "Kill any running `jank` process. `--app jank` targeting (used
   throughout this script) is ambiguous whenever more than one jank
   process exists, so this must run before AND after every recording.
   Matches by exact process name (`pkill -x`), not by install path — a
   jank installed somewhere other than `~/.local/bin` would otherwise
   dodge this and reintroduce the ambiguity."
  []
  (p/shell {:continue true :out nil :err nil} "pkill" "-9" "-x" "jank")
  (Thread/sleep 300))

;; ---------------------------------------------------------------- window

(defn wait-for-window
  "Poll the capture command until it produces a non-trivial PNG. A jank
   process can exist for several seconds before its window actually
   renders (first-ever compile of an example is slower than a cached
   rebuild) — checking process existence alone is not enough."
  [{:keys [capture timeout-ms]}]
  (let [probe (str (fs/create-temp-file {:suffix ".png"}))
        end   (+ (System/currentTimeMillis) timeout-ms)]
    (try
      (loop []
        (let [{:keys [exit]} (p/shell {:continue true
                                       :out nil
                                       :err nil}
                                      (rec/render capture {:file probe}))]
          (cond
            (and (zero? exit) (fs/exists? probe) (> (fs/size probe) 4096)) true
            (> (System/currentTimeMillis) end) false
            :else (do (Thread/sleep 150) (recur)))))
      (finally (fs/delete-if-exists probe)))))

;; ---------------------------------------------------------------- input
;; Not exercised this pass (every :input is nil — demo_manifest.edn ships
;; with an empty :overrides map), but kept ready for a future pass.

(defn synth-key!
  [k]
  (p/shell {:continue true :out nil :err nil} "cgevent" "key" "--app" "jank" k))

(defn synth-click!
  [x y]
  (p/shell {:continue true :out nil :err nil}
           "cgevent" "click" "--app" "jank" "--window" (str x) (str y)))

(defn play-input!
  [timeline]
  (future
    (let [t0 (System/currentTimeMillis)]
      (doseq [[at action & args] (sort-by first timeline)]
        (let [wait (- (+ t0 (long (* 1000 at))) (System/currentTimeMillis))]
          (when (pos? wait) (Thread/sleep wait))
          (case action
            :key   (synth-key! (first args))
            :click (let [[x y] args] (synth-click! x y))
            nil))))))

;; ---------------------------------------------------------------- one example

(defn record-one!
  [{:keys [id run src capture duration fps width warmup input manual out-dir]}]
  (let [gif (str (fs/file out-dir (str id ".gif")))]
    (fs/create-dirs out-dir)
    (println (format "\n▶ %s" id))
    (kill-stray-jank!)
    (let [proc (p/process {:dir "raylib-examples"
                           :out :string
                           :err :string
                           :shutdown p/destroy-tree} run)]
      (try
        (if-not (wait-for-window {:capture capture
                                  :timeout-ms (long (* 1000 (or warmup 10)))})
          (do (println "  ✗ window never appeared")
              {:id id
               :status :no-window
               :stderr (when-let [s (some-> (deref (:err proc) 2000 nil) str/join)]
                         (subs s 0 (min 400 (count s))))})
          (let [frames (str (fs/create-temp-dir {:prefix (str "frames-" id "-")}))]
            (Thread/sleep 250)
            (let [input-fut (when (seq input) (play-input! input))]
              (when manual
                (println (format "  ⌨  INTERACT NOW — recording %.0fs" (double duration))))
              (let [captured (rec/capture-frames {:capture capture
                                                  :duration duration
                                                  :fps fps
                                                  :frames-dir frames})]
                (when input-fut (future-cancel input-fut))
                (if (< (count captured) 2)
                  (do (fs/delete-tree frames)
                      {:id id
                       :status :too-few-frames})
                  {:id id
                   :status :captured
                   :gif gif
                   :frames frames
                   :captured captured
                   :fps fps
                   :width width
                   :duration duration
                   :frame-count (count captured)
                   :frames-expected (long (* duration fps))
                   :sha (sha256 src)})))))
        (finally
          (p/destroy-tree proc)
          (deref proc 3000 nil)
          (kill-stray-jank!))))))

(defn encode! [{:keys [gif frames captured fps width] :as r}]
  (let [ctx {:frames captured
             :frames-dir frames
             :out gif
             :fps fps
             :width width}]
    (try
      (if (have? "gifski") (rec/encode-gifski ctx) (rec/encode-ffmpeg ctx))
      (rec/optimize! gif)
      (fs/delete-tree frames)
      (println (format "  ✓ %s (%.1f MB)" gif (/ (fs/size gif) 1048576.0)))
      (assoc r :status :done :bytes (fs/size gif))
      (catch Exception e
        (println "  ✗ encode failed:" (ex-message e))
        (fs/delete-tree frames)
        (assoc r :status :encode-failed)))))

;; ---------------------------------------------------------------- manifest

(defn load-manifest
  "Build the full per-example spec list from bb/helpers.clj's `examples`
   registry (the single source of truth also used by bb.edn/bb info)
   merged with demo_manifest.edn's :defaults and any per-id :overrides.
   Excludes rlgl-compute (out of scope — see the plan's Global
   Constraints)."
  [manifest-path]
  (let [{:keys [defaults overrides]} (edn/read-string (slurp manifest-path))]
    (for [{:keys [profile cat desc]} h/examples
          :when (not= profile "rlgl-compute")]
      (merge defaults
             {:id profile
              :group (name cat)
              :desc desc
              :src (str (fs/file "raylib-examples" "src" "raylib_examples"
                                 (str (str/replace profile #"-" "_") ".jank")))
              :run (str "lein with-profile +" profile " run --disable-sandbox")}
             (get overrides profile)))))

(defn up-to-date? [ledger {:keys [id src out-dir duration fps width]}]
  (let [prev (get ledger id)
        gif  (fs/file out-dir (str id ".gif"))]
    (and prev (fs/exists? gif)
         (= (:sha prev) (sha256 src))
         (= (:settings prev) [duration fps width]))))

(defn write-readme!
  "Grouped catalog (shapes/core/text/textures/shaders/models/audio,
   mirroring bb/helpers.clj's cat-order), one heading + GIF per example
   present in `ledger` — the full cumulative set of everything ever
   successfully recorded, not just this run's newly-recorded subset."
  [path ledger out-dir]
  (fs/create-dirs (fs/parent path))
  (let [done-ids (set (keys ledger))
        group-of (fn [id] (some #(when (= id (:profile %)) (name (:cat %))) h/examples))]
    (spit path
          (str "# Examples\n\n"
               (str/join "\n"
                         (for [[cat title] h/cat-order
                               :let [group (name cat)
                                     ids (filter #(= group (group-of %)) done-ids)]
                               :when (seq ids)]
                           (str (format "## %s\n\n" title)
                                (str/join "\n"
                                          (for [id (sort ids)]
                                            (format "### %s\n\n![%s](%s)\n" id id
                                                    (str (fs/file-name (fs/file out-dir (str id ".gif"))))))))))
               "\n")))
  (println "\nWrote" path))

;; ---------------------------------------------------------------- keep display awake

(defn- start-caffeinate!
  "Hold display/idle/disk/system-sleep assertions for the duration of the
   recording run. An hour-plus unattended batch (Task 4: 208 examples) risks
   the display's own auto-sleep timer firing mid-run — every recording after
   that point fails identically to a locked/asleep screen at startup: GLFW
   can't determine a monitor, raylib's InitWindow segfaults, and
   wait-for-window times out with \"window never appeared\" for every
   remaining example (confirmed live during the 3-example smoke test — the
   screen had auto-locked/slept before the run started, and every one of the
   3 examples failed identically until the display was woken).
   NOTE: this does NOT wake an already-asleep/locked display at startup —
   only holds it awake going forward for as long as this process runs. The
   operator must ensure the screen is unlocked/awake before starting a run."
  []
  (p/process {:out nil :err nil :shutdown p/destroy-tree} "caffeinate" "-d" "-i" "-m" "-s"))

(defmacro with-display-awake
  "Prevent the display/system from sleeping for the duration of `body`,
   releasing the assertion no matter how `body` exits."
  [& body]
  `(let [caffeinate-proc# (start-caffeinate!)]
     (try
       ~@body
       (finally (p/destroy-tree caffeinate-proc#)))))

;; ---------------------------------------------------------------- brew unlink/relink

(defn homebrew-raylib-linked?
  "Checks both Apple Silicon (/opt/homebrew) and Intel (/usr/local)
   Homebrew prefixes — bb/helpers.clj probes both the same way for
   `lein`, and a prefix-specific check here would silently skip the
   unlink (and let the 8 shadowing-affected examples fail to compile)
   on an Intel Mac."
  []
  (or (fs/exists? "/opt/homebrew/include/raylib.h")
      (fs/exists? "/usr/local/include/raylib.h")))

(defmacro with-raylib-unlinked
  "Unlink a stray Homebrew raylib for the duration of `body`, relinking it
   afterward no matter how `body` exits — success, exception, OR the
   process being killed externally (Ctrl-C, `kill`). A plain `finally`
   only covers the first two: a terminating signal does not unwind the
   stack, so a `finally` block silently never runs on `kill`/SIGINT,
   leaving Homebrew raylib unlinked indefinitely (breaks the user's
   normal, non-recording dev workflow with no warning). Registers a JVM
   shutdown hook for the signal-termination path; `brew link` is
   idempotent, so the hook and the `finally` both firing on a normal
   exit is harmless."
  [& body]
  `(let [was-linked?# (homebrew-raylib-linked?)
         relink!# (fn []
                    (when was-linked?#
                      (println "ℹ️  Relinking Homebrew raylib…")
                      ;; Plain ProcessBuilder, NOT babashka.process — p/shell's
                      ;; process* unconditionally tries to register its own
                      ;; JVM shutdown hook per spawned process, which throws
                      ;; `IllegalStateException: Shutdown in progress` when
                      ;; called from a shutdown hook that's already running
                      ;; (confirmed live: relink still happened because the
                      ;; subprocess launches before the failed hook
                      ;; registration, but it prints an ugly stack trace and
                      ;; relies on that ordering by accident, not by design).
                      ;; ProcessBuilder registers no hook, so it works
                      ;; cleanly from both the `finally` and the hook path.
                      (-> (ProcessBuilder. ["brew" "link" "raylib"])
                          (.inheritIO)
                          .start
                          .waitFor)))
         hook# (Thread. ^Runnable relink!#)]
     (when was-linked?#
       (println "ℹ️  Unlinking Homebrew raylib for the duration of this recording run…")
       (p/shell {:continue true} "brew" "unlink" "raylib"))
     (.addShutdownHook (Runtime/getRuntime) hook#)
     (try
       ~@body
       (finally
         (.removeShutdownHook (Runtime/getRuntime) hook#)
         (relink!#)))))

;; ---------------------------------------------------------------- main

(defn -main [& args]
  (let [{:keys [manifest out-dir ledger only force dry-run pool readme]}
        (cli/parse-opts args {:spec spec})
        ledger-data (if (fs/exists? ledger) (edn/read-string (slurp ledger)) {})
        wanted      (when only (set (str/split only #",")))
        examples    (cond->> (map #(assoc % :out-dir out-dir) (load-manifest manifest))
                      wanted (filter (comp wanted :id)))
        todo        (if force examples (remove #(up-to-date? ledger-data %) examples))]
    (println (format "%d examples, %d to record, %d up to date."
                     (count examples) (count todo) (- (count examples) (count todo))))
    (when dry-run
      (doseq [e todo] (println "  -" (:id e) (if (:manual e) "[manual]" "[scripted]")))
      (System/exit 0))
    (with-display-awake
      (with-raylib-unlinked
        (let [ex-pool (Executors/newFixedThreadPool pool)
              pending (atom [])]
          (doseq [ex todo]
            (let [r (try (record-one! ex)
                         (catch Exception e
                           (println "  ✗ error:" (ex-message e))
                           {:id (:id ex)
                            :status :error}))]
              (swap! pending conj
                     (.submit ex-pool
                              (reify java.util.concurrent.Callable
                                (call [_] (if (= :captured (:status r)) (encode! r) r)))))))
          (.shutdown ex-pool)
          (.awaitTermination ex-pool 30 TimeUnit/MINUTES)
          (let [results (mapv #(.get %) @pending)
                updated (reduce (fn [m {:keys [id status sha duration fps width
                                               frame-count frames-expected] :as r}]
                                  (if (= :done status)
                                    (assoc m id {:sha sha
                                                 :settings [duration fps width]
                                                 :bytes (:bytes r)
                                                 ;; [captured expected] — NOT a pass/fail
                                                 ;; signal by itself: many examples are
                                                 ;; idle/static without scripted input (out
                                                 ;; of scope this pass, see demo_manifest.edn),
                                                 ;; so gifski/gifsicle correctly collapse
                                                 ;; genuinely-identical consecutive frames
                                                 ;; into fewer stored frames. A low count
                                                 ;; here is expected for those; it's only
                                                 ;; worth investigating for examples known to
                                                 ;; animate continuously without input.
                                                 :frames [frame-count frames-expected]
                                                 :at (str (java.time.Instant/now))})
                                    m))
                                ledger-data results)]
            (fs/create-dirs (fs/parent ledger))
            (spit ledger (pr-str updated))
            (write-readme! readme updated out-dir)
            (let [failed (remove #(= :done (:status %)) results)]
              (when (seq failed)
                (println "\nFailed:")
                (doseq [f failed] (println "  " (:id f) (:status f)))))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
