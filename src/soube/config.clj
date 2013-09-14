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
        vc ((first (vals (cheshire/parse-string vs))) "credentials")]
    {:subprotocol "mysql"
     :subname (str "//" (vc "host") ":" (vc "port") "/" (vc "name") "?characterEncoding=UTF-8")
     :user (vc "user")
     :password (vc "password")}))


(def allow-dropbox-set
  "dropbox uid 白名单"
  (apply
    hash-set
    (clojure.string/split
      (or
        (System/getenv "DROPBOX_UID")
        (System/getProperty "dropbox.uid")
        "77401815")
      #"[^\d]+")))

(def sites
  "站点配置,可以挂多个blog"
  {"default" {:name (or (System/getenv "SITE_NAME")
                        (System/getProperty "site.name")
                        "soube")
              :desciption (or (System/getenv "SITE_DESCIPTION")
                              (System/getProperty "site.desciption")
                              "一个简单易用的博客引擎")}
   "blog.kurrunk.com" {:name "kurrunk"
                       :description "不停转圈的人"}})

;db连接
(def mysql-db
  (if (System/getenv "VCAP_SERVICES")
    (get-cf-dbsec)
    {:subprotocol "mysql"
     :subname (or (System/getProperty "db.subname")
                  (System/getenv "DB_SUBNAME"))
     :user (or (System/getProperty "db.user")
               (System/getenv "DB_USER"))
     :password (or (System/getProperty "db.password")
                   (System/getenv "DB_PASSWORD"))}))
(defn get-tablename
  [hostname]
  (str
    (if (contains? sites hostname)
      (str
        (clojure.string/join
          ""
          (re-seq #"\w+" hostname)))
      "default")
    "_posts"))

(def tag-map
  "文章的tags"
  (zipmap (keys sites) (for [site (keys sites)] (atom {}))))

(defn push-tag 
  "新的文章"
  [site-name id title tags-str]
  (if (contains? tag-map site-name)
    (doseq [tag (clojure.string/split tags-str #",")]
      (if (not (= tag ""))
        (if (contains? (deref (tag-map site-name)) tag)
          (swap! (tag-map site-name) update-in [tag] conj {:id id :title title})
          (swap! (tag-map site-name) assoc tag [{:id id :title title}]))))))

; 更新现有文章的tag
(doseq [site-name (keys sites)]
  (try
    (doseq [row (jdbc/query mysql-db (sql/select [:title :id :tags] (get-tablename site-name) ["tags is not NULL"]))]
      (push-tag site-name (:id row) (:title row) (:tags row)))
    (catch Exception e (println "更新tag失败" site-name (.getMessage e)))))

(def sort-tags
  "按文章数排序"
  (apply
    merge
    (for [[site-name site-tags] tag-map]
      {site-name (take 100
                       (for [tag (sort #(compare (count (last %2)) (count (last %1)))
                                         (deref site-tags))]
                         (first tag)))})))


