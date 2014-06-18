(defproject soube "0.1.2"
  :description "这是一个个人blog引擎"
  :url "https://github.com/huzhengquan/soube"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [mysql/mysql-connector-java "5.1.25"]
                 ;[postgresql/postgresql "8.4-702.jdbc4"]
                 [clj-oauth "1.5.1"]
                 ;[cheshire "5.3.1"]
                 [org.clojure/data.json "0.2.5"]
                 ;[endophile "0.1.2"]
                 [org.pegdown/pegdown "1.4.2"]
                 [clj-time "0.7.0"]
								 ]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler soube.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
