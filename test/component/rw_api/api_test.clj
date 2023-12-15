(ns component.rw-api.api-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.stuartsierra.component :as component]
            [rw-api.core :as core]
            [clj-http.client :as client]))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(def sut (gensym))

(deftest greeting-test
  (testing "greeting-test"
    (with-system [sut (core/rw-api-system {:server {:port 8088}})]
      (is (= {:status 200 :body "Hi youtube!"} (-> (str "http://localhost:" 8088 "/greet")
                                                   (client/get)
                                                   (select-keys [:body :status])))))))

(deftest get-todo-test
  (testing "get-todo-test"
    (let [todo-id-1 (random-uuid)
          todo-1 {:id todo-id-1
                  :name "Todo for test"
                  :items [{:id (random-uuid)
                           :name "Finish the test"}]}]
      (with-system [sut (core/rw-api-system {:server {:port 8088}})]
        (reset! (-> sut :in-memory-state-comp :state-atom) [todo-1])
        (is (= {:status 200 :body (pr-str todo-1)}
               (-> (str "http://localhost:" 8088 "/todo/" todo-id-1)
                   (client/get)
                   (select-keys [:status :body]))))
        (testing "Empty body is return for random id"
          (is (= {:status 200 :body ""}
                 (-> (str "http://localhost:" 8088 "/todo/" (random-uuid))
                     (client/get)
                     (select-keys [:status :body])))))))))


(comment
  (get-todo-test))