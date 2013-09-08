(ns soube.config
	(:require [soube.jopbox :as dropbox]
            [clojure.java.jdbc :as jdbc]
						[cheshire.core :as cheshire]
            [clojure.java.jdbc.sql :as sql]))

;dropbox
(def consumer (dropbox/make-consumer
                (or (System/getProperty "dropbox.key")
                    (System/getenv "DROPBOX_KEY"))
                (or (System/getProperty "dropbox.secret")
                    (System/getenv "DROPBOX_SECRET"))))

(defn get-cf-dbsec
  "取得Cloud Foundry数据库信息"
  []
  (let [vs (System/getenv "VCAP_SERVICES")
        vc ((first ((cheshire/parse-string vs) "mysql-5.1")) "credentials")]
    {:subprotocol "mysql"
     :subname (str "//" (vc "host") ":" (vc "port") "/" (vc "name"))
     :user (vc "user")
     :password (vc "password")}))

;db连接
(def mysql-db (if (System/getenv "VCAP_SERVICES")
                (get-cf-dbsec)
                {:subprotocol "mysql"
               :subname (or (System/getProperty "db.subname")
                            (System/getenv "DB_SUBNAME"))
               :user (or (System/getProperty "db.user")
                         (System/getenv "DB_USER"))
               :password (or (System/getProperty "db.password")
                             (System/getenv "DB_PASSWORD"))}))
;getenv 

(def tag-map
  "文章的tags"
  (let [rows (jdbc/query mysql-db ["show tables"])
        tables (filter #(re-matches #"^\w+_posts$" %)
                       (map #(first (vals %)) rows))]
    (apply merge
      (for [table-name tables]
        {table-name (reduce
                      #(merge-with concat %1 %2)
                      (for [row (jdbc/query
                                  mysql-db
                                  (sql/select
                                    [:title :id :tags]
                                    table-name
                                    ["tags is not NULL"]))]
                        (reduce
                          #(assoc %1 (first %2) [(nth %2 1)])
                          {}
                          (for
                            [tag (clojure.string/split (:tags row) #",")]
                            [tag (select-keys row [:title :id])]))))}))))

(def sort-tags
  "按文章数排序"
  (apply
    merge
    (for [table tag-map]
      {(first table) (take 100
                           (map #(first %)
                                (sort
                                  #(compare (count (last %2)) (count (last %1)))
                                  (last table))))})))

(def sites
  "站点配置"
  {"default" {:name "soube"
              :desciption "一个简单易用的博客引擎"}
   "blog.kurrunk.com" {:name "kurrunk"
                       :description "不停转圈的人"
                       :dropbox #{"77401815"}}
   "jiandao.ap01.aws.af.cm" {:name "kurrunk"
                             :description "不停转圈的人"
                             :dropbox #{"77401815"}}
   "soube.jelastic.servint.net" {:name "kurrunk"
                                 :description "不停转圈的人"
                                 :dropbox #{"77401815"}}})

