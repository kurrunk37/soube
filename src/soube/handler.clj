(ns soube.handler
  (:use [compojure.core]
				;[ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clostache.parser :as clostache]
;            [clj-http.client :as client]
						[soube.install :as install]
						[soube.admin :as admin]
						[soube.config :as config]))

(defn render-template [template-file params]
	(clostache/render 
		(slurp 
			(clojure.java.io/resource 
				(str "templates/" template-file ".mustache")))
		params))

(defn index [req]
  (render-template "index" {:name "is正全"}))

;(defn test-https []
;  (str (client/get "https://alioth.debian.org" {:insecure? true})))


(defn post [id]
	(str "hello" id))

;; Authentication handler
;; 验证的逻辑
(defn wrap-auth
	[handler]
	(fn [req]
		(if (and (.startsWith (:uri req) "/admin") (not (admin/authenticated req)))
          (admin/authenticate req)
          (handler req))))

(defroutes app-routes
  (GET "/" [] index)
  (GET "/admin" [] admin/view-index)
  (GET "/admin/list" [] admin/view-list)
  (GET "/install" [] install/do)
;  (GET "/test_https" [] (test-https))
  (GET ["/:id", :id  #"[0-9]+"] [id] (post id))
  (route/resources "/")
  (route/not-found "Not Found"))

;(def app
;  (handler/site app-routes))

(def app
	(-> app-routes
		wrap-auth
		wrap-session
		wrap-keyword-params
		wrap-params))
