(ns soube.rss
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [clojure.xml]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [local :as timel] [coerce :as timec]]
						[soube.config :as config]
						[soube.to :as to])
  (:import [java.net URLEncoder]))

(defn format-time [time]
  (timef/unparse (timef/with-locale (timef/formatters :rfc822) java.util.Locale/ENGLISH) time))

(defmacro tag [id attrs & content]
    `{:tag ~id :attrs ~attrs :content [~@content]})

(defn item [site author {:keys [id title content time]}]
    (let [link (str site "/post/" id ".html")]
          (tag :item nil
                        (tag :guid nil link)
                        (tag :title nil title)
                        (tag :dc:creator nil author)
                        (tag :description nil content)
                        (tag :link nil link)
                        (tag :pubDate nil (format-time time))
                        (tag :category nil "clojure"))))

(defn message [site title author posts]
    (let [date (format-time (timel/local-now))]
      (tag :rss {:version "2.0"
                 :xmlns:dc "http://purl.org/dc/elements/1.1/"
                 :xmlns:sy "http://purl.org/rss/1.0/modules/syndication/"}
           (update-in
             (tag :channel nil
                  (tag :title nil "sitename")
                  (tag :description nil title)
                  (tag :link nil site)
                  (tag :lastBuildDate nil date)
                  (tag :dc:creator nil author)
                  (tag :language nil "en-US")
                  (tag :sy:updatePeriod nil "hourly")
                  (tag :sy:updateFrequency nil "1"))
             [:content]
             into (map (partial item site author) posts)))))


(defn view-rss
  "rss"
  [req]
  (let [table-name (to/h2t (:server-name req))
        rows (jdbc/query
               config/mysql-db
               (sql/select [:date :title :id :html] table-name (sql/order-by {:date :desc}) "limit 10"))]
    (with-out-str (clojure.xml/emit (message
                          "http://blog.kurrunk.com"
                          "kurrunk"
                          "nil"
                          (map
                            #(merge
                                 (select-keys % [:title :id])
                                 {:time (timec/from-sql-date (:date %)) :content (:html %)})
                            rows))))))

