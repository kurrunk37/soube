(ns soube.page
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [coerce :as timec]]
						[soube.config :as config]
						[soube.to :as to]))

(defn render-page
    [template data]
    (clostache/render-resource
      (str "templates/" template ".mustache")
      data
      (reduce
        (fn [accum pt]
          (assoc accum pt (slurp
                            (clojure.java.io/resource
                                    (str "templates/" (name pt) ".mustache")))))
        {}
        [:header :footer])))

(defn view-index
  "首页，文章列表"
  [req]
  (let [table-name (to/h2t (:server-name req))
        l (jdbc/query
            config/mysql-db
            (sql/select [:date :title :id] table-name (sql/order-by {:date :desc})))]
    (render-page "index" {:list l :tags (take 30 (config/sort-tags table-name))})))

(defn view-article
  "文章页"
  [req]
  ; (render-page "post" (first l))
  (let [table-name (to/h2t (:server-name req))
        id (:id (:params req))
        gettype (:type (:params req))
        l (jdbc/query
            config/mysql-db
            (sql/select [:markdown :html :title] table-name (sql/where {:id id})))
        thepost (first l)]
    (if thepost
      (cond
        (= gettype "html")
          (render-page "post" {:markdown (:html thepost) :page-title (:title thepost)})
        (= gettype "md")
          (:markdown thepost)
        :else
          (response (str "404")))
      (render-page "post" {:markdown "404"}))))

(defn view-test 
  "test"
  [req]
  (str config/sort-tags)
  #_(let [t
        (reduce #(merge-with concat %1 %2) (for [row (jdbc/query config/mysql-db (sql/select [:title :id :tags] "blogkurrunkcom_posts" ["tags is not NULL"]))]
          (reduce #(assoc %1 (first %2) [(nth %2 1)]) {} (for [tag (clojure.string/split (:tags row) #",")] [tag (select-keys row [:title :id])]))))]
    (pr-str t)))
