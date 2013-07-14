(ns soube.admin
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
						[soube.jopbox :as dropbox]
						[soube.config :as config]))

(defn render-template [template-file params]
	(clostache/render 
		(slurp 
			(clojure.java.io/resource 
				(str "templates/" template-file ".mustache")))
		params))

(defn view-index [req]
  (render-template "admin" {:name "one admin"}))

(defn view-list [req]
  (let [files
        (dropbox/metadata
          config/consumer
          (:auth-token (:session req))
          :sandbox
          ((config/account-dict (:server-name req)) :dirname))]
    (render-template "admin" {:name files})))

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
