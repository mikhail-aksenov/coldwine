(defproject coldwine "0.1.0-SNAPSHOT"
  :description "Simple seismic navigation extractor"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [sigrun "0.3.5"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.slf4j/slf4j-simple "1.7.9"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [intervox/clj-progress "0.1.6"]
                 [medley "0.5.5"]]
  :main coldwine.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
