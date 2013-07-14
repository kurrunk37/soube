(ns soube.install
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
						[soube.config :as config]))

(defn render-template [template-file params]
	(clostache/render 
		(slurp 
			(clojure.java.io/resource 
				(str "templates/" template-file ".mustache")))
		params))

(defn init-table [hostname]
  (jdbc/with-connection config/mysql-db
    (jdbc/create-table
      (str "if not exists " ((config/account-dict hostname) :table-prefix) "posts")
      [:id :integer "UNSIGNED" "PRIMARY KEY" "AUTO_INCREMENT"]
      [:date "datetime" "not null"]
      [:title "tinytext" "not null"]
      [:src "enum('dropbox','write')" "not null" "default 'write'"]
      [:account "tinytext"]
      [:markdown "mediumtext"]
      [:html "mediumtext"]
      ))
  "done")

(defn do [req]
  (init-table (:server-name req)))

