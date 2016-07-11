(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :dependencies
  [[adzerk/boot-cljs "1.7.228-1" :scope "test"]
   [adzerk/boot-reload "0.4.11" :scope "test"]
   [pandeiro/boot-http "0.7.3" :scope "test"]
   [org.clojure/clojure "1.9.0-alpha8"]
   [org.clojure/clojurescript "1.9.93"]
   [org.clojure/core.async "0.2.385"]
   [datascript "0.15.0"]
   [posh "0.5.1"]
   [reagent "0.6.0-rc"]
   [cljsjs/d3 "3.5.16-0"]]
  :source-paths
  ["src/cljs" "resources"])