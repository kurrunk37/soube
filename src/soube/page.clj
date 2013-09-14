(ns soube.page
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [coerce :as timec]]
						[soube.config :as config])
  (:import [java.net URLEncoder]))

(defn get-siteid
  "取得站点id"
  [hostname]
  (if (contains? config/sites hostname)
      (:server-name req)
      "default"))

(defn get-site-conf
  "取得站点配置"
  [req]
  (config/sites (get-siteid (:server-name req))))

(defn render-page
  "渲染页面"
  [hostname template data]
  (let [dir (get-siteid hostname)]
    (clostache/render-resource
      (str dir "/templates/" template ".mustache")
      data
      (reduce
        (fn [accum pt]
          (assoc accum pt (slurp
                            (clojure.java.io/resource
                                    (str dir "/templates/" (name pt) ".mustache")))))
        {}
        [:header :footer]))))


(defn view-index
  "首页，文章列表"
  [req]
  (let [table-name (config/get-tablename (:server-name req))
        p (Integer/parseInt (get (:params req) :p "1"))
        limit 5
        l (try
            (jdbc/query
              config/mysql-db
              (sql/select [:date :title :id :html] table-name (sql/order-by {:date :desc}) (str "limit " (* limit (dec p)) "," limit)))
            (catch Exception e nil))]
    (if (not l)
      (clostache/render-resource (str "templates/doc/install.mustache") {})
      (render-page (:server-name req)
                   "index" {:site-name (:name (get-site-conf req))
                          :page-title (:name (get-site-conf req))
                          :list l
                          :p p
                          :next (if (= limit (count l)) (inc p) false) 
                          :prev (if (= p 1) false (dec p))
                          :tags (map
                                  #(into {} {:url (URLEncoder/encode %) :tag %})
                                  (take 30 (config/sort-tags (:server-name req))))}))))

(defn view-article
  "文章页"
  [req]
  ; (render-page "post" (first l))
  (let [table-name (config/get-tablename (:server-name req))
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
             :site-name (:name (get-site-conf req))
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
        hostname (:server-name req)
        table-name (config/get-tablename hostname)
        tag-posts ((deref (config/tag-map hostname)) tag)]
    (render-page (:server-name req)
                 "tag" {:site-name (:name (get-site-conf req))
                        :page-title tag
                        :list tag-posts
                        :tags (map
                                #(into {} {:url (URLEncoder/encode %) :tag %})
                                (take 30 (config/sort-tags hostname)))})))

(defn view-test 
  "test"
  [req]
  (str (System/getenv "VCAP_SERVICES"))
  #_(let [t
        (reduce #(merge-with concat %1 %2) (for [row (jdbc/query config/mysql-db (sql/select [:title :id :tags] "blogkurrunkcom_posts" ["tags is not NULL"]))]
          (reduce #(assoc %1 (first %2) [(nth %2 1)]) {} (for [tag (clojure.string/split (:tags row) #",")] [tag (select-keys row [:title :id])]))))]
    (pr-str t)))
