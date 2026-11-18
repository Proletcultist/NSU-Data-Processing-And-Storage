(ns ru.nsu.zenin.philosophers)

(defn philosopher
  [idx
   l-fork r-fork
   n-dine
   think-duration dine-duration]
  {:pre [(>= n-dine 0)
         (>= think-duration 0)
         (>= dine-duration 0)]}
  (if (pos? n-dine)
    (do
      (Thread/sleep think-duration)
      (let [tries (atom 0)]
        (dosync
          (swap! tries inc)
          (alter l-fork inc)
          (alter r-fork inc)
          (Thread/sleep dine-duration))
        (printf "Philosopher №%d ate in %d tries. %d more to eat\n" idx @tries (dec n-dine))
        (flush))
      (recur idx l-fork r-fork (dec n-dine) think-duration dine-duration))))
  

(defn run-philosophers
  [philos-num
   n-dine
   think-duration dine-duration]
  {:pre [(>= philos-num 2)
         (>= n-dine 0)
         (>= think-duration 0)
         (>= dine-duration 0)]}
  (let [forks (repeatedly 
                philos-num
                (fn [] (ref 0)))
        fork-pairs (partition 
                     2 1
                     (concat forks (list (first forks))))
        philos (->> fork-pairs
                    (map vector (iterate inc 0))
                    (map 
                      (fn 
                        [[idx [left right]]]
                        (new Thread 
                             (fn [] (philosopher idx left right n-dine think-duration dine-duration))))))]
    (->> philos
         (map (fn [p] (.start p)))
         (doall))
    (->> philos
         (map (fn [p] (.join p)))
         (doall))
    nil))
