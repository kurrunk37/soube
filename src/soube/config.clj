(ns soube.config
	(:require [soube.jopbox :as dropbox]))

;dropbox
(def consumer (dropbox/make-consumer "m0ewy8m17xyyxcy" "2wr21zwzilwwsuw"))

;db连接
(def mysql-db {:subprotocol "mysql"
               :subname "//127.0.0.1:3306/soube"
               :user "root"
               :password "xmlxml"})

;帐户
(def account-dict {
                   "localhost" {:table-prefix "localhost_" :dirname "localhost"} })
