(ns ru.nsu.zenin.util)

;; Returns f(f(..f(x)...)), where f applied to x n times
(defn applyn
  [f n x]
  (reduce 
    (fn [acc i] (i acc)) 
    x 
    (repeat n f)))

(defn appendable?
  [x li]
  (or 
    (empty? li)
    (not (zero? (compare (first li) x)))))

;; ((b a) (c b) (d c)), d -> ((d b a) (d c b))
(defn append
  [lists x]
  (map
    (fn [i] (cons x i))
    (filter (partial appendable? x) lists)))

(defn flatten
  [lists]
  (reduce
    (fn [acc i] (concat i acc))
    '()
    lists))

;; ((a) (b) (d)), (c d) -> ((c a) (d a) (c b) (d b))
(defn appendAll
  [symbols lists]
  (flatten (reduce 
    (fn [acc i] (cons (append lists i) acc))
    '()
    symbols)))

(defn flattenStr
  [lists]
  (reduce
    (fn [acc i] (str acc i))
    ""
    lists))

(defn makeWords
  [alph n]
  (map 
    flattenStr
    (applyn 
      (partial appendAll alph) 
      n
      '(()))))

