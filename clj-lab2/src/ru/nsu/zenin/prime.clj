(ns ru.nsu.zenin.prime)

(def naturals
    (lazy-seq
        (cons 1 (map inc naturals))))

(defn sieve
    [candidates]
    (lazy-seq
        (let [[prime & restCandidates] candidates]
            (cons prime (sieve (remove (fn [n] (zero? (rem n prime))) restCandidates))))))

(def primes (sieve (rest naturals)))
