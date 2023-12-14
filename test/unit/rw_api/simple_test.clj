(ns unit.rw-api.simple-test
  (:require [clojure.test :refer [deftest testing is]]))

(deftest unit-simple-passing-test
  (testing "Unit Test"
    (is (= 1 1))))
