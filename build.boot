(set-env!
 :source-paths    #{"src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[adzerk/boot-cljs          "1.7.228-1"  :scope "test"]
                 [adzerk/boot-reload        "0.4.12"      :scope "test"]
                 [pandeiro/boot-http        "0.7.3"      :scope "test"]
                 [org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.93"]
                 [org.clojure/core.async "0.2.385"]
                 [datascript "0.15.2"]
                 [posh "0.5.3.3"]
                 [reagent "0.6.0-rc"]
                 [devcards "0.2.1-7"]
                 [bidi "2.0.9"]
                 [cljsjs/d3 "3.5.16-0"]
                 [cljsjs/firebase "3.3.0-0"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]])

(deftask build []
  (comp ;;(speak)
        (cljs)))

(deftask run []
  (comp (serve)
        (watch)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :whitespace
                       :source-map true})
  identity)

(deftask development []
  (task-options! cljs {:optimizations :none
                       :source-map true}
                 reload {:on-jsload 'voterx.main/render})
  identity)

(deftask dev
  "Simple alias to run application in development mode"
  []
  (comp (development)
        (run)))

(deftask public []
  (comp (production)
        (build)
        (sift :invert true :include #{#"js/devcards\.out" #"js/app\.out" #"\S+\.cljs\.edn"})
        (target :dir #{"public"})))
