(ns unit.rw-api.test
  (:require [clojure.test :refer [deftest is testing]]
            [rw-api.components.pedestal-comp :refer [url-for]]))

(deftest url-for-test
  (testing "greet endpoint url"
    (is (= "/greet" (url-for :greet))))

  (testing "get todo by id endpoint url"
    (let [todo-id (random-uuid)]
      (is (= (str "/todo/" todo-id) (url-for :get-todo {:path-params {:todo-id todo-id}})))))


  (comment
    (url-for-test)))