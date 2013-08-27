(ns soube.handler
  (:use [compojure.core]
				;[ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
;            [clj-http.client :as client]
						[soube.install :as install]
						[soube.admin :as admin]
						[soube.page :as page]
						[soube.config :as config]))


;; Authentication handler
;; 验证的逻辑
(defn wrap-auth
	[handler]
	(fn [req]
		(if (and (.startsWith (:uri req) "/admin") (not (admin/authenticated req)))
          (admin/authenticate req)
          (handler req))))

(defroutes app-routes
  (GET "/" [] page/view-index)
  (GET "/admin" [] admin/view-index)
  (GET "/admin/dashboard" [] admin/view-index)
  (GET "/admin/sync.json" [] admin/markdown-sync)
  (GET "/admin/info" [] admin/account-info)
  (GET "/admin/install" [] admin/init-table)
  (GET "/admin/tools" [] admin/view-tools)
  (GET "/test" [] page/view-test)
;  (GET "/test_https" [] (test-https))
  (GET ["/post/:id.:type", :id  #"[0-9]+", :type #"(html|md)"] [] page/view-article)
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
