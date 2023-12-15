(ns component.rw-api.api-test
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.stuartsierra.component :as component]
            [rw-api.components.pedestal-comp :refer [url-for]]
            [rw-api.core :as core]))

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

(deftest greeting-test
  (testing "greeting-test"
    (with-system [sut (core/rw-api-system {:server {:port (get-free-port)}})]
      (is (= {:status 200 :body "Hi youtube!"}
             (-> (sut->url sut (url-for :greet))
                 (client/get)
                 (select-keys [:body :status])))))))

(deftest get-todo-test
  (testing "get-todo-test"
    (let [todo-id-1 (random-uuid)
          todo-1 {:id todo-id-1
                  :name "Todo for test"
                  :items [{:id (random-uuid)
                           :name "Finish the test"}]}]
      (with-system [sut (core/rw-api-system {:server {:port (get-free-port)}})]
        (reset! (-> sut :in-memory-state-comp :state-atom) [todo-1])
        (is (= {:status 200 :body (pr-str todo-1)}
               (-> (sut->url sut (url-for :get-todo {:path-params {:todo-id todo-id-1}}))
                   (client/get)
                   (select-keys [:status :body]))))
        (testing "Empty body is return for random id"
          (is (= {:status 200 :body ""}
                 (-> (sut->url sut (url-for :get-todo {:path-params {:todo-id (random-uuid)}}))
                     (client/get)
                     (select-keys [:status :body])))))))))



(comment
  (greeting-test)
  (get-todo-test))
