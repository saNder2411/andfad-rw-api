(ns component.api.todo-api-test
  (:require [clj-http.client :as client]
            [clojure.test :refer [deftest is testing]]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [rw-api.components.pedestal-comp :refer [url-for]]
            [rw-api.core :as core]
            [utils.test-helpers :as helpers])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(def sut (gensym))

(deftest db-get-todo-test
  (testing "get-todo-test"
    (let [db-container (PostgreSQLContainer. "postgres:15.4")]
      (try
        (.start db-container)

        (helpers/with-system
          [sut (core/rw-api-system {:server {:port (helpers/get-free-port)}
                                    :htmx {:server {:port (helpers/get-free-port)}}
                                    :db-spec {:jdbcUrl (.getJdbcUrl db-container)
                                              :username (.getUsername db-container)
                                              :password (.getPassword db-container)}})]

          (let [{:keys [data-source]} sut
                insert-query (-> {:insert-into [:todo]
                                  :columns [:title]
                                  :values [["Todo for test"]]
                                  :returning :*}
                                 (sql/format))

                {:keys [todo-id title]} (jdbc/execute-one!
                                         (data-source)
                                         insert-query
                                         {:builder-fn rs/as-unqualified-kebab-maps})
                {:keys [status body]} (-> (helpers/sut->url
                                           sut (url-for :db-get-todo {:path-params {:todo-id todo-id}}))
                                          (client/get {:accept :json
                                                       :as :json
                                                       :throw-exceptions false}))]

            (is (= 200 status))
            (is (some? (:created-at body)))
            (is (= {:todo-id (str todo-id) :title title} (select-keys body [:todo-id :title])))

            (testing "Empty body is return for random id"
              (is (= {:status 404 :body ""}
                     (-> (helpers/sut->url sut (url-for :db-get-todo
                                                        {:path-params {:todo-id (str (random-uuid))}}))
                         (client/get {:throw-exceptions false})
                         (select-keys [:status :body])))))))
        (finally
          (.stop db-container))))))

(comment
  (db-get-todo-test))