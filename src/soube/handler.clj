(ns soube.handler
  (:use [compojure.core]
				[ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
						[jopbox.client :as dropbox]
            [clostache.parser :as clostache]
						[soube.config :as config]))

(defn render-template [template-file params]
	(clostache/render 
		(slurp 
			(clojure.java.io/resource 
				(str "templates/" template-file ".mustache")))
		params))

(defn index []
  (render-template "index" {:name "is正全"}))

(defn admin-index []
  (render-template "admin" {:name "one admin"}))

(defn post [id]
	(str "hello" id))

(defn authenticated
  [{session :session}]
  (boolean (:auth-token session)))

(defn authenticate
	[req]
  (let [{session :session}     req
        {port    :server-port} req
        {hostname  :server-name} req
        {path :uri} req
        {scheme  :scheme}      req
        {params  :params}      req
        {token :oauth_token uid :uid error :oauth_problem} params
        ;{token :oauth_token verifier :oauth_verifier error :oauth_problem} params
        url (str (name scheme) "://" hostname (if (= 80 port) "" (str ":" port)) "/admin")]
    (cond
      error
        (response (str "Authentication error " error))
      (and token uid)
        (do
          (-> (redirect url)
            (assoc :session (assoc session :auth-token token))))
      :else
				(let [request-token (dropbox/fetch-request-token config/consumer)
					dropboxurl (dropbox/authorization-url config/consumer request-token)]
          (redirect (str dropboxurl "&oauth_callback=" url))))))

;; Authentication handler
;; 验证的逻辑
(defn wrap-auth
	[handler]
	(fn [req]
		(if (and (.startsWith (:uri req) "/admin") (not (authenticated req)))
          (authenticate req)
          (handler req))))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/admin" [] (admin-index))
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
