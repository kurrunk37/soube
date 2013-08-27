(ns soube.config
	(:require [soube.jopbox :as dropbox]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]))

;dropbox
(def consumer (dropbox/make-consumer
                (System/getenv "DROPBOX_KEY")
                (System/getenv "DROPBOX_SECRET")))

;db连接
(def mysql-db {:subprotocol "mysql"
               :subname (System/getenv "DB_SUBNAME")
               :user (System/getenv "DB_USER")
               :password (System/getenv "DB_PASSWORD")})

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
                                    "blogkurrunkcom_posts"
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
