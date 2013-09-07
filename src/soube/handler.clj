(ns soube.handler
  (:use [compojure.core]
				[ring.util.response :only [redirect]]
				[ring.middleware session params keyword-params])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
;            [clj-http.client :as client]
						[soube.install :as install]
						[soube.admin :as admin]
						[soube.page :as page]
						[soube.rss :as rss]
						[soube.config :as config]))


(defn wrap-auth
  "验证的逻辑"
	[handler]
	(fn [req]
		(if (and (.startsWith (:uri req) "/admin")
             (not (admin/authenticated req)))
          (admin/authenticate req)
          (handler req))))

(defn wrap-hostname
  "判断域名是否有效"
	[handler]
	(fn [req]
    (if (contains? config/sites (:server-name req))
      (handler req)
      (redirect (str "http://" (key (first config/sites)) ":" (:server-port req))))))

(defroutes app-routes
  (GET "/" [] page/view-index)
  (GET "/feed" [] rss/view-rss)
  (GET ["/tag/:tag", :tag #"[^/?&]+"] [] page/view-tag)
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
    wrap-hostname
		wrap-auth
		wrap-session
		wrap-keyword-params
		wrap-params))
