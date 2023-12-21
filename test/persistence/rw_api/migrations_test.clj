(ns persistence.rw-api.migrations-test
  (:require [clojure.test :refer [deftest is]]
            [com.stuartsierra.component :as component]
            [rw-api.core :as core]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))


(def sut (gensym))

(defn datasource-only-system [config]
  (component/system-map
   :data-source (core/data-source-comp config)))

(defn create-db-contatiner []
  (doto (PostgreSQLContainer. "postgres:15.4")))

(deftest migrations-test
  (let [database-container (create-db-contatiner)]
    (try
      (.start database-container)
      (with-system
        [sut (datasource-only-system
              {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                         :username (.getUsername database-container)
                         :password (.getPassword database-container)}})]
        (let [{:keys [data-source]} sut
              [schema-version :as schema-versions]
              (jdbc/execute!
               (data-source)
               ["select * from schema_version"]
               {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= 1 (count schema-versions)))
          (is (= {:description "add todo tables"
                  :script "V1__add_todo_tables.sql"
                  :success true}
                 (select-keys schema-version [:description :script :success])))))
      (finally
        (.stop database-container)))))

(deftest todo-table-test
  (let [database-container (create-db-contatiner)]
    (try
      (.start database-container)
      (with-system
        [sut (datasource-only-system
              {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                         :username (.getUsername database-container)
                         :password (.getPassword database-container)}})]
        (let [{:keys [data-source]} sut
              insert-results (jdbc/execute!
                              (data-source)
                              ["
insert into todo (title)
values ('my todo list'),
       ('other todo list')
returning *
"]
                              {:builder-fn rs/as-unqualified-lower-maps})
              select-results (jdbc/execute!
                              (data-source)
                              ["
select * from todo"]
                              {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= 2
                 (count insert-results)
                 (count select-results)))
          (is (= #{"my todo list"
                   "other todo list"}
                 (->> insert-results (map :title) (into #{}))
                 (->> select-results (map :title) (into #{}))))))
      (finally
        (.stop database-container)))))

(comment
  (migrations-test)

  (todo-table-test)
  )
