(ns persistence.rw-api.migrations-test
  (:require [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.test-helpers :as helpers]))

(def sut (gensym))

(deftest migrations-test
  (let [database-container (helpers/create-db-container)]
    (try
      (.start database-container)
      (helpers/with-system
        [sut (helpers/datasource-only-system
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
  (let [database-container (helpers/create-db-container)]
    (try
      (.start database-container)
      (helpers/with-system
        [sut (helpers/datasource-only-system
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

  (todo-table-test))
