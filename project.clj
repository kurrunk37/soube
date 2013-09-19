(defproject soube "0.1.0-SNAPSHOT"
  :description "这是一个个人blog引擎"
  :url "https://github.com/huzhengquan/soube"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
								 [de.ubercode.clostache/clostache "1.3.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [clj-oauth "1.4.0"]
                 [cheshire "5.2.0"]
                 [endophile "0.1.0"]
                 [markdown-clj "0.9.29"]
                 [clj-time "0.5.1"]
;                 [com.cemerick/url "0.1.0"]
;                 [hiccup "1.0.4"]
;                 [clj-uri "0.1.0"]
;								 [clj-dropbox "1.2.1"]
;                 [korma "0.3.0-RC5"]
;                 [enlive "1.1.1"]
;                 [sandbar/sandbar "0.4.0-SNAPSHOT"]
								 ;[clj-oauth "1.4.0"]
;								 [clj-oauth "1.3.1-SNAPSHOT"]
;								 [clj-http "0.7.2"]
;								 [org.clojars.tavisrudd/clj-apache-http "2.3.2-SNAPSHOT"]

								 ]
  :plugins [[lein-ring "0.8.3"]]
  :ring {:handler soube.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
