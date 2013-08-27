(ns soube.to)

;dropbox
(defn h2t
  "主机名转换成table名"
  [hostname]
  (str (clojure.string/join "" (re-seq #"\w+" hostname)) "_posts"))

