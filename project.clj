(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :comment
  "Generated from build.boot for Cursive"
  :dependencies
  [[adzerk/boot-cljs "1.7.228-2" :scope "test"]
   [adzerk/boot-reload "0.4.13" :scope "test"]
   [pandeiro/boot-http "0.7.3" :scope "test"]
   [org.clojure/clojure "1.9.0-alpha14"]
   [org.clojure/clojurescript "1.9.293"]
   [org.clojure/core.async "0.2.395"]
   [datascript "0.15.4"]
   [posh "0.5.4"]
   [reagent "0.6.0"]
   [devcards "0.2.2"]
   [cljsjs/d3 "4.2.4-0"]
   [cljsjs/firebase "3.3.0-0"]]
  :repositories
  [["clojars" {:url "https://clojars.org/repo/"}]
   ["maven-central" {:url "https://repo1.maven.org/maven2"}]]
  :source-paths
  ["src/cljs" "resources"])