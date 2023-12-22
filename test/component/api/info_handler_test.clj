(ns component.api.info-handler-test
  (:require [clj-http.client :as client]
            [clojure.test :refer [deftest is testing]]
            [rw-api.components.pedestal-comp :refer [url-for]]
            [rw-api.core :as core]
            [utils.test-helpers :as helpers])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(def sut (gensym))

(deftest info-handler-test
  (testing "info-handler-test"
    (let [db-container (doto (PostgreSQLContainer. "postgres:15.4")
                         (.withDatabaseName "rwa-test")
                         (.withUsername "test")
                         (.withPassword "test"))]
      (try
        (.start db-container)
        (helpers/with-system
          [sut (core/rw-api-system {:server {:port (helpers/get-free-port)}
                                    :htmx {:server {:port (helpers/get-free-port)}}
                                    :db-spec {:jdbcUrl (.getJdbcUrl db-container)
                                              :username (.getUsername db-container)
                                              :password (.getPassword db-container)}})]
          (is (= {:status 200 :body "Database server version: 15.4 (Debian 15.4-2.pgdg120+1)"}
                 (-> (helpers/sut->url sut (url-for :info))
                     (client/get {:accept :json})
                     (select-keys [:body :status])))))
        (finally
          (.stop db-container))))))


(comment
  (info-handler-test))