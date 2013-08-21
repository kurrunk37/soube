(ns soube.page
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [coerce :as timec]]
						[soube.config :as config]))

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
  (let [table-name (str ((config/account-dict (:server-name req)) :table-prefix) "posts")
        l (jdbc/query
            config/mysql-db
            (sql/select [:date :title :id] table-name (sql/order-by [:date])))]
    (render-page "index" {:list l})))

(defn view-article
  "文章页"
  [req]
  ; (render-page "post" (first l))
  (let [table-name (str ((config/account-dict (:server-name req)) :table-prefix) "posts")
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

