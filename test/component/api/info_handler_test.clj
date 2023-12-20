(ns component.api.info-handler-test
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            [rw-api.components.pedestal-comp :refer [url-for]]
            [rw-api.core :as core])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(def sut (gensym))

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

(deftest info-handler-test
  (testing "info-handler-test"
    (let [db-container (doto (PostgreSQLContainer. "postgres:15.4")
                         (.withDatabaseName "rw-api-db")
                         (.withUsername "test")
                         (.withPassword "test"))]
      (try
        (.start db-container)
        (with-system [sut (core/rw-api-system {:server {:port (get-free-port)}
                                               :htmx {:server {:port (get-free-port)}}
                                               :db-spec {:jdbcUrl (.getJdbcUrl db-container)
                                                         :username (.getUsername db-container)
                                                         :password (.getPassword db-container)}})]
          (is (= {:status 200 :body "Database server version: 15.4 (Debian 15.4-2.pgdg120+1)"}
                 (-> (sut->url sut (url-for :info))
                     (client/get {:accept :json})
                     (select-keys [:body :status])))))
        (finally
          (.stop db-container))))))


(comment
  (info-handler-test))