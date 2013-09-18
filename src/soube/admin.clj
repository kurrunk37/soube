(ns soube.admin
  (:use [ring.util.response :only [redirect response]]
        [markdown.core :only [md-to-html-string]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						[cheshire.core :as cheshire]
            [endophile.core :as markdown]
            [clj-time [format :as timef] [coerce :as timec]]
						[soube.to :as to]
						[soube.config :as config]))

(defn render-page [template data]
    (clostache/render-resource
      (str "templates/admin/" template ".mustache")
      data
      (reduce
        (fn [accum pt]
          (assoc accum pt (slurp
                            (clojure.java.io/resource
                                    (str "templates/admin/" (name pt) ".mustache")))))
        {}
        [:header :footer])))

(defn view-index [req]
  (render-page "dashboard"
               {:name (str (:session req))
                :site-name (:name (config/get-site-conf req))}))

(defn view-tools [req]
  (render-page "tools"
               {:name (str (:session req))
                :site-name (:name (config/get-site-conf req))}))

(defn account-info [req]
  (let [info (dropbox/account-info config/consumer (:access-token (:session req)))]
    (render-page "admin" {:name info})))

; 取出现有数据
(defn get-db-dict
  "从数据库中把元数据取出,返回以:path为key的map"
  [src uid hostname]
  (println (str src uid hostname))
  (let [table-name (config/get-tablename hostname)
        l (jdbc/query
            config/mysql-db
            (sql/select
              [:id :path :revision :modified]
              table-name
              (sql/where {:src src :account uid})))]
    (zipmap (map #(:path %) l) l)))
      ;(group-by :path l))))

(defn parse-markdown
  "解析dropbox的文件，分离元数据和正文"
  [file-content]
  (let [lines (clojure.string/split-lines file-content)
        meta-lines (take-while #(re-matches #"^[\w]+:(.+)$" %) lines)
        md-string (clojure.string/join "\n" (drop (count meta-lines) lines))
        file-clj (markdown/to-clj (markdown/mp md-string))
        meta-map (into {}
                       (for
                         [[_ k v] (map #(re-matches #"^([\w]+):[\s]*(.+)$" %) meta-lines)]
                         [(keyword k) v]))]
    {:meta meta-map :md-clj file-clj :md-string md-string}))

;clj-time用法
;(parse (with-locale (formatters :rfc822) Locale/ENGLISH) "Wed, 4 Jul 2001 12:08:56 -0700" )
(defn update-db
  "新的md"
  [action hostname uid md file-content]
  (println action)
  (let [table-name (config/get-tablename hostname)
        {file-clj :md-clj  metadata :meta md-string :md-string} (parse-markdown file-content)
        ;title (or (:title metadata)
                  ;(clojure.string/join " " (:content (some #(if (= (:tag %) :h1) %) file-clj))))
        modified (timef/unparse
                   (timef/formatter "yyyy-MM-dd HH:mm:ss")
                   (timef/parse
                     (timef/with-locale (timef/formatters :rfc822) java.util.Locale/ENGLISH)
                     (:modified md)))
        date (or (:date metadata) modified)
        todb (merge
               (select-keys md [:path :revision])
               (select-keys metadata [:id :title :categories])
               {:src "dropbox",
                :account uid,
                :date date,
                :markdown file-content,
                :modified modified,
                :tags (if (:tags metadata)
                        (clojure.string/join
                          ","
                          (filter
                            #(not (= % ""))
                            (map #(clojure.string/trim %)
                                 (clojure.string/split (:tags metadata) #","))))
                        nil)
                :html (md-to-html-string md-string)})]
    (println (:title metadata))
    (cond
      (= (:published metadata) "False")
        (println "this is Undisclosed")
      (= action :insert)
        (try
          (do 
            (jdbc/insert! config/mysql-db table-name todb)
            (if (contains? metadata :tags)
              (if-let [rows (jdbc/query config/mysql-db
                                        (sql/select [:id :title :tags]
                                                    table-name
                                                    (sql/where {:src "dropbox" :account uid :path (:path md)})))]
                (if-let [row (first rows)]
                  (config/push-tag (config/get-siteid hostname) (:id row) (:title row) (:tags row))))))
          (catch Exception e (println "caught exception: " (.getMessage e))))
      (= action :update)
        (try
          (jdbc/update!
            config/mysql-db
            table-name
            (select-keys todb [:markdown :title :modified :date :revision :html])
            (sql/where {:account uid :path (:path md)}))
          (catch Exception e (println (str "caught exception: " (.getMessage e))))))))

; 文件同步
(defn markdown-sync [req]
  "同步dropbox和数据库里的文章"
  (println (:session req))
  (let [hostname (:server-name req)
        access-token (:access-token (:session req))
        dir-matadata (dropbox/metadata
                       config/consumer
                       access-token
                       "sandbox"
                       hostname)
        md-list (seq (filter #(and
                                (= (:mime_type %) "application/octet-stream")
                                (= (:is_dir %) false))
                             (:contents dir-matadata)))
        db-map (get-db-dict "dropbox" (:uid (:access-token (:session req))) hostname)
        update-md-list (filter (fn [md] (let [db-md (db-map (:path md))]
                                          (not (and db-md (= (:revision db-md) (:revision md))))))
                               md-list)]
    (doseq [md update-md-list]
      (let [file-content (dropbox/get-file
                           config/consumer
                           access-token
                           "sandbox" ;(:root md)
                           (:path md))
            db-md (db-map (:path md))]
        (cond
          (not db-md)
            (update-db :insert hostname (:uid access-token) md file-content)
          (not (= (:revision db-md) (:revision md)))
            (update-db :update hostname (:uid access-token) md file-content))))
    (if (empty? update-md-list)
      "{\"msg\":\"没有更新\"}"
      (do (config/update-sort-tag (config/get-siteid hostname))
        (cheshire/generate-string update-md-list)))))

(defn init-table [req]
  "初始化table"
  (try
    (do
      (jdbc/with-connection config/mysql-db
        (jdbc/create-table
          (str "if not exists " (config/get-tablename (:server-name req)))
          [:id :integer "UNSIGNED" "PRIMARY KEY" "AUTO_INCREMENT"]
          [:path "varchar(64)" "not null"]
          [:revision :smallint "UNSIGNED" "not null" "default 0"]
          [:date "datetime" "not null"]
          [:modified "datetime" "not null"]
          [:title "tinytext" "not null"]
          [:src "enum('dropbox','write')" "not null" "default 'write'"]
          [:account "tinytext"]
          [:tags "tinytext"]
          [:categories "varchar(64)"]
          [:markdown "mediumtext"]
          [:html "mediumtext"]
          ))
      (str "{\"status\": 1, \"msg\": \"初始化完成\"}"))
    (catch Exception e (str "{\"error\": \"caught exception: " (.getMessage e) "\"}"))))

; 身份判断
(defn authenticated
  [{session :session}]
  (boolean (:access-token session)))

; 去dropbox验证
(defn authenticate
	[req]
  (let [{session :session}     req
        {port    :server-port} req
        {hostname  :server-name} req
        {path :uri} req
        {scheme  :scheme}      req
        {params  :params}      req
        {oauth-token :oauth_token uid :uid error :oauth_problem} params
        ;{token :oauth_token verifier :oauth_verifier error :oauth_problem} params
        url (str (name scheme) "://" hostname (if (= 80 port) "" (str ":" port)) "/admin")]
    (cond
      error
        (response (str "Authentication error " error))
      (and oauth-token uid)
        (if (contains? config/allow-dropbox-map uid)
          (-> (redirect url)
            (assoc :session
                   (assoc session
                          :access-token (dropbox/fetch-access-token-response config/consumer (:request-token session)))))
          (redirect "/403.html"))
      :else
				(let [request-token (dropbox/fetch-request-token config/consumer)
              dropboxurl (dropbox/authorization-url config/consumer request-token)]
         (->
          (redirect (str dropboxurl "&oauth_callback=" url))
          (assoc :session (assoc session :request-token request-token)))))))
