(ns utils.test-helpers
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [rw-api.core :as core])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(defn sut->url [sut path]
  (str/join ["http://localhost:" (-> sut :pedestal-comp :config :server :port) path]))


(defn get-free-port []
  (with-open [socket (java.net.ServerSocket. 0)]
    (.getLocalPort socket)))

(defn datasource-only-system [config]
  (component/system-map
   :data-source (core/data-source-comp config)))

(defn create-db-contatiner []
  (doto (PostgreSQLContainer. "postgres:15.4")))