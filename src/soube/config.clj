(ns soube.config
	(:require [soube.jopbox :as dropbox]
            [clojure.java.jdbc :as jdbc]
						[cheshire.core :as cheshire]
            [clj-time  [coerce :as timecoerce] [local :as timelocal]]
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
     :subname (str "//" (vc "host") ":" (vc "port") "/" (vc "name") "?characterEncoding=UTF-8")
     :user (vc "user")
     :password (vc "password")}))

(def allow-dropbox-map
  "dropbox uid 白名单"
  (let [uid-list (clojure.string/split
                   (or (System/getenv "DROPBOX_UID") (System/getProperty "dropbox.uid") "77401815:zhengquan")
                   #",")]
    (apply merge
      (map
        (fn [uid-str]
          (if-let [uid-fields (clojure.string/split uid-str #":" 2)]
            {(get uid-fields 0) (get uid-fields 1)}))
        uid-list))))

(def sites
  "站点配置,可以挂多个blog"
  {"default" {:name (or (System/getenv "SITE_NAME")
                        (System/getProperty "site.name")
                        "soube")
              :desciption (or (System/getenv "SITE_DESCIPTION")
                              (System/getProperty "site.desciption")
                              "一个简单易用的博客引擎")}
   "hu.yudao.org" {:name "胡遇到"
                   :description "di di dididi"}
   "hu.yujian.name" {:name "胡遇见"
                     :description "di di dididi"}
   "blog.kurrunk.com" {:name "kurrunk"
                       :description "不停转圈的人"}})

;db连接
(def db-spec
  (cond
    (System/getenv "VCAP_SERVICES")
      (get-cf-dbsec)
    (System/getenv "DATABASE_URL")
      (System/getenv "DATABASE_URL")
    (System/getProperty "database.url")
      (System/getProperty "database.url")
      ;(get-url-dbsec)
    :else
      {:subprotocol (or (System/getenv "DB_SUBPROTOCOL")
                        (System/getProperty "db.subprotocol")
                        "mysql")
       :subname (or (System/getProperty "db.subname")
                    (System/getenv "DB_SUBNAME"))
       :user (or (System/getProperty "db.user")
                 (System/getenv "DB_USER"))
       :password (or (System/getProperty "db.password")
                     (System/getenv "DB_PASSWORD"))}))

(def db-protocol
  "取得db的类型"
  (if (string? db-spec)
    (get (clojure.string/split db-spec #":") 0 "mysql")
    (get db-spec :subprotocol "mysql")))

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
  [site-id id title date tags-str]
  (if (contains? tag-map site-id)
    (doseq [tag (apply hash-set (filter #(not (= % "")) (map #(clojure.string/trim %) (clojure.string/split tags-str #","))))]
      (if (contains? (deref (tag-map site-id)) tag)
        (swap! (tag-map site-id) update-in [tag] conj {:id id :title title :date date})
        (swap! (tag-map site-id) assoc tag [{:id id :title title :date date}])))))

(def sort-tags
  "按文章数排序"
  (atom (zipmap (keys sites) (for [site-id (keys sites)] []))))

(defn update-sort-tag
  "更新排序后的tag"
  [site-id]
  (if (contains? (deref sort-tags) site-id)
    (let [weight (fn [post]
                   (- 60
                      (/ (- (timecoerce/to-long (timelocal/local-now))
                            (timecoerce/to-long (:date post)))
                         2678400000)))]
      (swap! sort-tags assoc site-id
             (take 50
                   (keys
                     (sort
                       #(compare (apply + (map weight (last %2)))
                                 (apply + (map weight (last %1))))
                       (deref (tag-map site-id)))))))))

; 更新现有文章的tag
(doseq [site-id (keys tag-map)]
  (try
    (do
      (doseq [row (jdbc/query db-spec (sql/select [:title :id :tags :date] (get-tablename site-id) ["tags is not NULL"]))]
        (push-tag site-id (:id row) (:title row) (:date row) (:tags row)))
      (update-sort-tag site-id))
    (catch Exception e (println "更新tag失败" site-id (.getMessage e)))))

(defn get-siteid
  "取得站点id"
  [hostname]
  (if (contains? sites hostname)
    hostname
    "default"))

(defn get-site-conf
  "取得站点配置"
  [req]
  (sites (get-siteid (:server-name req))))


#_(apply
    merge
    (for [[site-name site-tags] tag-map]
      {site-name (take 100
                       (for [tag (sort #(compare (count (last %2)) (count (last %1)))
                                         (deref site-tags))]
                         (first tag)))}))


