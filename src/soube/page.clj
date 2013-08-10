(ns soube.page
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						;[cheshire.core :as cheshire]
            [endophile.core :as markdown]
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

(defn view-index [req]
  (render-page "index" {:name "is正全"}))

(defn view-article [id]
  (render-page "index" {:name "is正全"}))
