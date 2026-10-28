(ns ru.nsu.zenin.filter-perf
  (:require [ru.nsu.zenin.filter :refer :all]))

(defn heavy-pred
  [n]
  (Thread/sleep 10)
  (zero? (rem n 2)))

(defn filter-perf
  []
  (time (reduce + (filter heavy-pred (range 1 1024)))))

(defn pfilter-perf
  []
  (time (reduce + (pfilter heavy-pred (range 1 1024)))))
