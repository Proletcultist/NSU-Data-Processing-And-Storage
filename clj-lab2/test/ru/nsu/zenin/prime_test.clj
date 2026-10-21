(ns ru.nsu.zenin.prime-test
    (:require [clojure.test :refer :all]
              [ru.nsu.zenin.prime :refer :all]))

(deftest naturals-test
    (testing "Checking few naturals from defined sequence on correctness"
        (is (= (nth naturals 0) 1))
        (is (= (nth naturals 1) 2))
        (is (= (nth naturals 10) 11))
        (is (= (nth naturals 199) 200))))

(deftest primes-test
    (testing "Checking few primes from defined sequence on correctness"
        (is (= (nth primes 0) 2))
        (is (= (nth primes 1) 3))
        (is (= (nth primes 2) 5))
        (is (= (nth primes 101) 557))))
