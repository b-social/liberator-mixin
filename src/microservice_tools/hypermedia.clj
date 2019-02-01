(ns microservice-tools.hypermedia
  (:require
    [bidi.bidi :refer [path-for]]
    [clojure.string :as str]))

(defn base-url [request]
  (str
    (-> request :scheme name)
    "://"
    (get-in request [:headers "host"])))

(defn absolute-url-for
  [request routes handler & args]
  (str
    (base-url request)
    (apply path-for routes handler args)))

(defn parameterised-url-for
  [request routes handler parameter-names & args]
  (str
    (apply absolute-url-for request routes handler args)
    (str "{?" (str/join "," parameter-names) "}")))
