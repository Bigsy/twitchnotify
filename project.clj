(defproject twitchnotify "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [binaryage/chromex "0.6.1"]
                 [binaryage/devtools "0.9.10"]
                 [figwheel "0.5.16"]
                 [figwheel-sidecar "0.5.16"]
                 [environ "1.1.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]
            [lein-shell "0.5.0"]
            [lein-environ "1.1.0"]
            [lein-cooper "1.2.2"]]

  :source-paths ["src/background"
                 "src/content_script"
                 "scripts"]

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/release/compiled"]

  ;:cljsbuild {:builds
  ;            {:background
  ;             {:source-paths ["src/background"]
  ;              :figwheel     true
  ;              :compiler     {:output-to     "resources/unpacked/compiled/background/main.js"
  ;                             :output-dir    "resources/unpacked/compiled/background"
  ;                             :asset-path    "compiled/background"
  ;                             :preloads      [devtools.preload figwheel.preload]
  ;                             :main          twitchnotify.background
  ;                             :optimizations :none
  ;                             :source-map    true}}}}


  :profiles {:unpacked
             {:cljsbuild {:builds
                          {:background
                           {:source-paths ["src/background"]
                            :figwheel     true
                            :compiler     {:output-to     "resources/unpacked/compiled/background/main.js"
                                           :output-dir    "resources/unpacked/compiled/background"
                                           :asset-path    "compiled/background"
                                           :preloads      [devtools.preload figwheel.preload]
                                           :main          twitchnotify.background
                                           :optimizations :none
                                           :source-map    true}}}}}

             :unpacked-content-script
             {:cljsbuild {:builds
                          {:content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to     "resources/unpacked/compiled/content-script/main.js"
                                           :output-dir    "resources/unpacked/compiled/content-script"
                                           :asset-path    "compiled/content-script"
                                           :main          twitchnotify.content-script
                                           ;:optimizations :whitespace                                                        ; content scripts cannot do eval / load script dynamically
                                           :optimizations :advanced                                                           ; let's use advanced build with pseudo-names for now, there seems to be a bug in deps ordering under :whitespace mode
                                           :pseudo-names  true
                                           :pretty-print  true}}}}}
             :checkouts
             ; DON'T FORGET TO UPDATE scripts/ensure-checkouts.sh
             {:cljsbuild {:builds
                          {:background {:source-paths ["checkouts/cljs-devtools/src/lib"
                                                       "checkouts/chromex/src/lib"
                                                       "checkouts/chromex/src/exts"]}}}}

             :checkouts-content-script
             ; DON'T FORGET TO UPDATE scripts/ensure-checkouts.sh
             {:cljsbuild {:builds
                          {:content-script {:source-paths ["checkouts/cljs-devtools/src/lib"
                                                           "checkouts/chromex/src/lib"
                                                           "checkouts/chromex/src/exts"]}}}}

             :figwheel
             {:figwheel {:server-port    3449
                         :server-logfile ".figwheel.log"
                         :repl           true}}

             :disable-figwheel-repl
             {:figwheel {:repl false}}

             :cooper
             {:cooper {"content-dev"     ["lein" "content-dev"]
                       "fig-dev-no-repl" ["lein" "fig-dev-no-repl"]
                       "browser"         ["scripts/launch-test-browser.sh"]}}

             :release
             {:env       {:chromex-elide-verbose-logging "true"}
              :cljsbuild {:builds
                          {:background
                           {:source-paths ["src/background"]
                            :compiler     {:output-to     "resources/release/compiled/background.js"
                                           :output-dir    "resources/release/compiled/background"
                                           :asset-path    "compiled/background"
                                           :main          twitchnotify.background
                                           :optimizations :advanced
                                           :elide-asserts true}}

                           :content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to     "resources/release/compiled/content-script.js"
                                           :output-dir    "resources/release/compiled/content-script"
                                           :asset-path    "compiled/content-script"
                                           :main          twitchnotify.content-script
                                           :optimizations :advanced
                                           :elide-asserts true}}}}}}

  :aliases {"dev-build"       ["with-profile" "+unpacked,+unpacked-content-script,+checkouts,+checkouts-content-script" "cljsbuild" "once"]
            "fig"             ["with-profile" "+unpacked,+figwheel" "figwheel" "background"]
            "content"         ["with-profile" "+unpacked-content-script" "cljsbuild" "auto" "content-script"]
            "fig-dev-no-repl" ["with-profile" "+unpacked,+figwheel,+disable-figwheel-repl,+checkouts" "figwheel" "background"]
            "content-dev"     ["with-profile" "+unpacked-content-script,+checkouts-content-script" "cljsbuild" "auto"]
            "devel"           ["with-profile" "+cooper" "do"                                                                  ; for mac only
                               ["shell" "scripts/ensure-checkouts.sh"]
                               ["cooper"]]
            "release"         ["with-profile" "+release" "do"
                                           ["clean"]
                                           ["cljsbuild" "once" "background" "content-script"]]
            "package"         ["shell" "scripts/package.sh"]})
