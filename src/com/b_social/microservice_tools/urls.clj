(ns com.b-social.microservice-tools.urls
  (:require
    [bidi.bidi :refer [path-for]]
    [clojure.string :as str]))

(defn base-url [request]
  (let [scheme (-> request :scheme name)
        host (get-in request [:headers "host"])]
    (format "%s://%s" scheme host)))

(defn absolute-url-for
  [request routes handler & args]
  (str
    (base-url request)
    (apply path-for routes handler args)))

(defn parameterised-url-for
  [request routes handler parameter-names & args]
  (let [parameter-names (map name parameter-names)]
    (str
      (apply absolute-url-for request routes handler args)
      (str "{?" (str/join "," parameter-names) "}"))))