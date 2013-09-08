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
						[soube.to :as to])
  (:import [java.net URLEncoder]))

(defn render-page
    [hostname template data]
    (clostache/render-resource
      (str hostname "/templates/" template ".mustache")
      data
      (reduce
        (fn [accum pt]
          (assoc accum pt (slurp
                            (clojure.java.io/resource
                                    (str hostname "/templates/" (name pt) ".mustache")))))
        {}
        [:header :footer])))

(defn get-site
  [req]
  (config/sites
    (if (contains? config/sites (:server-name req))
      (:server-name req)
      "default")))

(defn view-index
  "首页，文章列表"
  [req]
  (let [table-name (to/h2t (:server-name req))
        p (Integer/parseInt (get (:params req) :p "1"))
        limit 5
        l (jdbc/query
            config/mysql-db
            (sql/select [:date :title :id :html] table-name (sql/order-by {:date :desc}) (str "limit " (* limit (dec p)) "," limit)))]
    (render-page (:server-name req)
                 "index" {:site-name (:name (get-site req))
                          :page-title (:name (get-site req))
                          :list l
                          :p p
                          :next (if (= limit (count l)) (inc p) false) 
                          :prev (if (= p 1) false (dec p))
                          :tags (map
                                          #(into {} {:url (URLEncoder/encode %) :tag %})
                                          (take 30 (config/sort-tags table-name)))})))

(defn view-article
  "文章页"
  [req]
  ; (render-page "post" (first l))
  (let [table-name (to/h2t (:server-name req))
        id (:id (:params req))
        gettype (:type (:params req))
        l (jdbc/query
            config/mysql-db
            (sql/select [:markdown :html :title :date :tags] table-name (sql/where {:id id})))
        thepost (first l)
        ]
    (if thepost
      (cond
        (= gettype "html")
          (render-page
            (:server-name req)
            "post"
            {:markdown (:html thepost)
             :site-name (:name (get-site req))
             :post-date (:date thepost)
             :tags (map #(into {}  {:url (URLEncoder/encode %) :tag %}) (clojure.string/split (:tags thepost) #","))
             :page-title (:title thepost)})
        (= gettype "md")
          (:markdown thepost)
        :else
          (response (str "404")))
      (render-page (:server-name req) "post" {:markdown "404"}))))

(defn view-tag
  "tag聚合页"
  [req]
  (let [tag (:tag (:params req))
        table-name (to/h2t (:server-name req))
        tag-posts ((config/tag-map table-name) tag)]
    (render-page (:server-name req)
                 "tag" {:site-name (:name (get-site req))
                        :page-title tag
                        :list tag-posts
                        :tags (map
                                #(into {} {:url (URLEncoder/encode %) :tag %})
                                (take 30 (config/sort-tags table-name)))})))

(defn view-test 
  "test"
  [req]
  (str (System/getenv "VCAP_SERVICES"))
  #_(let [t
        (reduce #(merge-with concat %1 %2) (for [row (jdbc/query config/mysql-db (sql/select [:title :id :tags] "blogkurrunkcom_posts" ["tags is not NULL"]))]
          (reduce #(assoc %1 (first %2) [(nth %2 1)]) {} (for [tag (clojure.string/split (:tags row) #",")] [tag (select-keys row [:title :id])]))))]
    (pr-str t)))
