(ns soube.config
	(:require [soube.jopbox :as dropbox]))

;dropbox
(def consumer (dropbox/make-consumer
                (System/getenv "DROPBOX_KEY")
                (System/getenv "DROPBOX_SECRET")))

;db连接
(def mysql-db {:subprotocol "mysql"
               :subname (System/getenv "DB_SUBNAME")
               :user (System/getenv "DB_USER")
               :password (System/getenv "DB_PASSWORD")})

;帐户
(def account-dict {
                   "localhost" {:table-prefix "localhost_"
                                :dirname "localhost"} })
