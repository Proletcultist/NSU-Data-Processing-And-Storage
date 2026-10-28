(ns ru.nsu.zenin.filter)

(defn pfilter
  [pred coll]
  (let [chunk-size 128,
        parallelism (.. Runtime getRuntime availableProcessors),
        parts (partition-all parallelism (partition-all chunk-size coll))]
    (flatten 
      (flatten 
        (map 
          (fn [chunks] 
            (->> chunks
                 (map (fn [chnk] (future (doall (filter pred chnk)))))
                 (doall)
                 (map deref))) 
          parts)))))


;; (1 2 3 4 ...)
;; ((1 2) (3 4) (...))
;; (((1 2) (3 4)) (...))
;; ((fut1 fut2) (..))
;; (((2) (4)) (..))
;; ((2 4) (...))
;; (2 4 ...)
