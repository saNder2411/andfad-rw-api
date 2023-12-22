(ns component.api.api-test
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.test :refer [deftest is testing]]
            [rw-api.components.pedestal-comp :refer [url-for]]
            [rw-api.core :as core]
            [utils.test-helpers :as helpers]))

(def sut (gensym))

(deftest greeting-test
  (testing "greeting-test"
    (helpers/with-system [sut (core/rw-api-system {:server {:port (helpers/get-free-port)}})]
      (is (= {:status 200 :body "Hi youtube!"}
             (-> (helpers/sut->url sut (url-for :greet))
                 (client/get {:accept :json})
                 (select-keys [:body :status])))))))

(deftest content-negotiation-test
  (testing "only application/json is accepted"
    (helpers/with-system [sut (core/rw-api-system {:server {:port (helpers/get-free-port)}})]
      (is (= {:status 406 :body "Not Acceptable"}
             (-> (helpers/sut->url sut (url-for :greet))
                 (client/get {:accept :edn
                              :throw-exceptions false})
                 (select-keys [:body :status])))))))

(deftest get-todo-test
  (testing "get-todo-test"
    (let [todo-id-1 (str (random-uuid))
          todo-1 {:id todo-id-1
                  :title "Todo for test"
                  :items [{:id (str (random-uuid))
                           :title "Finish the test"
                           :status "created"}]}]

      (helpers/with-system [sut (core/rw-api-system {:server {:port (helpers/get-free-port)}})]
        (reset! (-> sut :in-memory-state-comp :state-atom) [todo-1])
        (is (= {:status 200 :body todo-1}
               (-> (helpers/sut->url sut (url-for :get-todo {:path-params {:todo-id todo-id-1}}))
                   (client/get {:accept :json
                                :as :json
                                :throw-exceptions false})
                   (select-keys [:status :body]))))

        (testing "Empty body is return for random id"
          (is (= {:status 404 :body ""}
                 (-> (helpers/sut->url sut (url-for :get-todo {:path-params {:todo-id (str (random-uuid))}}))
                     (client/get {:throw-exceptions false})
                     (select-keys [:status :body])))))))))


(deftest post-todo-test
  (testing "post-todo-test"
    (let [todo-id-1 (str (random-uuid))
          todo-1 {:id todo-id-1
                  :title "Todo for test"
                  :items [{:id (str (random-uuid))
                           :title "Finish the test"
                           :status "created"}]}]

      (helpers/with-system [sut (core/rw-api-system {:server {:port (helpers/get-free-port)}})]
        (testing "Store and retrieve todo by id"
          (is (= {:status 201 :body todo-1}
                 (-> (helpers/sut->url sut (url-for :post-todo))
                     (client/post {:accept :json
                                   :content-type :json
                                   :as :json
                                   :throw-exceptions false
                                   :body (json/encode todo-1)})
                     (select-keys [:status :body]))))

          (is (= {:status 200 :body todo-1}
                 (-> (helpers/sut->url sut (url-for :get-todo {:path-params {:todo-id todo-id-1}}))
                     (client/get {:accept :json
                                  :as :json
                                  :throw-exceptions false})
                     (select-keys [:status :body])))))

        (testing "Invalid Todo is rejected"
          (is (= {:status 500}
                 (-> (helpers/sut->url sut (url-for :post-todo))
                     (client/post {:accept :json
                                   :content-type :json
                                   :as :json
                                   :throw-exceptions false
                                   :body (json/encode {:id todo-id-1
                                                       :title "Todo for test"})})
                     (select-keys [:status])))))))))

(comment
  (greeting-test)
  (content-negotiation-test)
  (get-todo-test)
  (post-todo-test))