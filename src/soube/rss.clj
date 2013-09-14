(ns soube.rss
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [clojure.xml]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [local :as timel] [coerce :as timec]]
						[soube.config :as config]))

(defn format-time [time]
  (timef/unparse (timef/with-locale (timef/formatters :rfc822) java.util.Locale/ENGLISH) time))

(defmacro tag [id attrs & content]
    `{:tag ~id :attrs ~attrs :content [~@content]})

(defn item [site {:keys [id title content time author]}]
    (let [link (str site "/post/" id ".html")]
          (tag :item nil
                        (tag :guid nil link)
                        (tag :title nil title)
                        (tag :dc:creator nil author)
                        (tag :description nil content)
                        (tag :link nil link)
                        (tag :pubDate nil (format-time time))
                        (tag :category nil "clojure"))))

(defn message [site title description author lastdate posts]
  (tag :rss {:version "2.0"
             :xmlns:dc "http://purl.org/dc/elements/1.1/"
             :xmlns:sy "http://purl.org/rss/1.0/modules/syndication/"}
       (update-in
             (tag :channel nil
                  (tag :title nil title)
                  (tag :description nil description)
                  (tag :link nil site)
                  (tag :lastBuildDate nil (format-time lastdate))
                  (tag :dc:creator nil author)
                  (tag :language nil "zh-CN")
                  (tag :sy:updatePeriod nil "hourly")
                  (tag :sy:updateFrequency nil "1"))
             [:content]
             into (map (partial item site) posts))))


(defn view-rss
  "rss"
  [req]
  (let [table-name (config/get-tablename (:server-name req))
        app (config/sites (:server-name req))
        rows (jdbc/query
               config/mysql-db
               (sql/select [:date :title :id :html :account] table-name (sql/order-by {:date :desc}) "limit 10"))]
    (with-out-str (clojure.xml/emit
                    (message
                      (str "http://" (:server-name req))
                      (:name app "")
                      (:description app "")
                      (:author app "")
                      (timec/from-sql-date (:date (first rows)))
                      (map
                        #(merge
                           (select-keys % [:title :id])
                           {:time (timec/from-sql-date (:date %)) :content (:html %) :author (:account %)})
                        rows))))))

