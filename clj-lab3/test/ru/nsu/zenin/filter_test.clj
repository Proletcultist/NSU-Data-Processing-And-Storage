(ns ru.nsu.zenin.filter-test
    (:require [clojure.test :refer :all]
              [ru.nsu.zenin.filter :refer :all]))

(deftest pfilter-test
    (testing "Checking for correct filtering of finite sequences"
        (is (= 
              (pfilter (fn [n] (zero? (rem n 2))) (range 1 128)) 
              (filter (fn [n] (zero? (rem n 2))) (range 1 128))))
        (is (= 
              (pfilter (fn [n] (not (zero? (rem n 2)))) (range 1 255)) 
              (filter (fn [n] (not (zero? (rem n 2)))) (range 1 255))))
        (is (= 
              (pfilter (fn [n] (zero? (rem n 2))) '()) 
              (filter (fn [n] (zero? (rem n 2))) '())))
        (is (= 
              (pfilter (fn [n] (zero? (rem n 2))) '(1)) 
              (filter (fn [n] (zero? (rem n 2))) '(1)))))
    (testing "Checking for correct filtering of infinite sequence"
        (is (= 
              (take 101 (pfilter (fn [n] (zero? (rem n 2))) (iterate inc 1))) 
              (take 101 (filter (fn [n] (zero? (rem n 2))) (iterate inc 1)))))))
