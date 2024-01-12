(ns persistence.rw-api.honeysql-test
  (:require [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [utils.test-helpers :as helpers]
            [honey.sql :as sql]))

(def sut (gensym))

(-> {:select [[[:count :t1.a] :t1a] :t2.*]
     :from [[:tabel-one :t1] [:table-two :t2]]
     :where [:and
             [:= :t1.id :t2.id]
             [:or
              [:= :a 1]
              [:<> :t2.b "something"]]]}
    (sql/format))

(deftest migrations-honeysql-query-test
  (let [database-container (helpers/create-db-container)]
    (try
      (.start database-container)
      (helpers/with-system
        [sut (helpers/datasource-only-system
              {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                         :username (.getUsername database-container)
                         :password (.getPassword database-container)}})]
        (let [{:keys [data-source]} sut
              select-query (sql/format {:select :*
                                        :from :schema-version})
              [schema-version :as schema-versions] (jdbc/execute!
                                                    (data-source)
                                                    select-query
                                                    {:builder-fn rs/as-unqualified-lower-maps})]

          (is (= ["SELECT * FROM schema_version"] select-query))
          (is (= 1 (count schema-versions)))
          (is (= {:description "add todo tables"
                  :script "V1__add_todo_tables.sql"
                  :success true}
                 (select-keys schema-version [:description :script :success])))))
      (finally
        (.stop database-container)))))

(deftest todo-table-honeysql-query-test
  (let [database-container (helpers/create-db-container)]
    (try
      (.start database-container)
      (helpers/with-system
        [sut (helpers/datasource-only-system
              {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                         :username (.getUsername database-container)
                         :password (.getPassword database-container)}})]
        (let [{:keys [data-source]} sut
              insert-query (-> {:insert-into [:todo]
                                :columns [:title]
                                :values [["My Todo List"]
                                         ["Other Todo List"]]
                                :returning :*}
                               (sql/format))

              insert-results (jdbc/execute!
                              (data-source)
                              insert-query
                              {:builder-fn rs/as-unqualified-lower-maps})

              select-results (jdbc/execute!
                              (data-source)
                              (sql/format {:select :* :from :todo})
                              {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= ["INSERT INTO todo (title) VALUES (?), (?) RETURNING *" "My Todo List" "Other Todo List"] insert-query))
          (is (= 2 (count insert-results) (count select-results)))
          (is (= #{"My Todo List" "Other Todo List"}
                 (->> insert-results (map :title) (into #{}))
                 (->> select-results (map :title) (into #{}))))))
      (finally
        (.stop database-container)))))


(comment
  (migrations-honeysql-query-test)

  (todo-table-honeysql-query-test))
