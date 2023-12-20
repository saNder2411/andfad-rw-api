(ns persistence.rw-api.simple-test
  (:require [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(deftest simple-persistence-test
  (let [db-container (doto (PostgreSQLContainer. "postgres:15.4")
                       (.withDatabaseName "rw-api-db")
                       (.withUsername "test")
                       (.withPassword "test"))]
    (try
      (.start db-container)

      (let [ds (jdbc/get-datasource {:jdbcUrl (.getJdbcUrl db-container)
                                     :username (.getUsername db-container)
                                     :password (.getPassword db-container)})]
        (is (= {:r 1} (first (jdbc/execute! ds ["select 1 as r;"])))))
      (finally
        (.stop db-container)))))

(comment
  (simple-persistence-test))
