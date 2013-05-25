(defproject understanding-clojure-sql "0.1.0-SNAPSHOT"
  :url "http://github.com/mattdenner/understanding-sql-clojure"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lobos "1.0.0-beta1" :exclusions [org.clojure/java.jdbc]]
                 [korma               "0.3.0-RC5"]
                 [com.h2database/h2   "1.3.170"]
                 ]
  :profiles  {:dev  {:dependencies  [[midje "1.5.0"]]}})
