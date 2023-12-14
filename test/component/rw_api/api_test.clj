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
  (testing "Simple component test"
    (with-system [sut (core/rw-api-system {:server {:port 8088}})]
      (is (= {:status 200 :body "Hi youtube!"} (-> (str "http://localhost:" 8088 "/greet")
                                                   (client/get)
                                                   (select-keys [:body :status])))))))

;; (greeting-test)