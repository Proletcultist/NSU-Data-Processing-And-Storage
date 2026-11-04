(ns ru.nsu.zenin.factory)

(declare supply-msg)
(declare notify-msg)

(defn storage
  "Creates a new storage
   ware - a name of ware to store (string)
   notify-step - amount of stored items required for logger to react. 0 means to logging 
   consumers - factories to notify when the storage is updated
   returns a map that contains:
     :storage - an atom to store items that can be used by factories directly
     :ware - a stored ware name
     :worker - an agent to send supply-msg"
  [ware notify-step & consumers]
  (let [counter (atom 0 :validator #(>= % 0)),
        worker-state {:storage counter,
                    :ware ware,
                    :notify-step notify-step,
                    :consumers consumers}]
    {:storage counter,
     :ware ware,
     :worker (agent worker-state)}))

(defn factory
  "Creates a new factory
   amount - number of items produced per cycle
   duration - cycle duration in milliseconds
   target-storage - a storage to put products with supply-msg
   ware-amounts - a list of ware names and their amounts required for a single cycle
   returns a map that contains:
     :worker - an agent to send notify-msg"
  [amount duration target-storage & ware-amounts]
  (let [bill (apply hash-map ware-amounts),
        buffer (reduce-kv (fn [acc k _] (assoc acc k 0)) 
                          {} bill),
        ;;a state of factory agent:
        ;;  :amount - a number of items to produce per cycle
        ;;  :duration - a duration of cylce 
        ;;  :target-storage - a storage to place products (via supply-msg to its worker)
        ;;  :bill - a map with ware names as keys and their amounts of values
        ;;     shows how many wares must be consumed to perform one production cycle
        ;;  :buffer - a map with similar structure as for :bill that shows how many wares are already collected;
        ;;     it is the only mutable part. 
        worker-state {:amount amount,
                      :duration duration,
                      :target-storage target-storage,
                      :bill bill,
                      :buffer buffer
                      :production-line (agent nil)}]
    {:worker (agent worker-state)}))

(defn source
  "Creates a source that is a thread that produces 'amount' of wares per cycle to store in 'target-storage'
   and with given cycle 'duration' in milliseconds
   returns Thread that must be run explicitly"
  [amount duration target-storage]
  (new Thread 
       (fn []
         (Thread/sleep duration)
         (send (target-storage :worker) supply-msg amount)
         (recur))))

(defn supply-msg
  "A message that can be sent to a storage worker to notify that the given 'amount' of wares should be added.
   Adds the given 'amount' of ware to the storage and notifies all the registered factories about it 
   state - see code of 'storage' for structure"
  [state amount]
  (swap! (state :storage) #(+ % amount))      ;update counter, could not fail  
  (let [ware (state :ware),
        cnt @(state :storage),                
        notify-step (state :notify-step),
        consumers (state :consumers)]
    ;;logging part, notify-step == 0 means no logging
    (when (and (> notify-step 0)
               (> (int (/ cnt notify-step))
                  (int (/ (- cnt amount) notify-step))))
      (println (.format (new java.text.SimpleDateFormat "hh.mm.ss.SSS") (new java.util.Date)) 
              "|" ware "amount: " cnt))
    ;;factories notification part
    (when consumers
      (doseq [consumer (shuffle consumers)]
        (send (consumer :worker) notify-msg ware (state :storage) cnt))))
  state)                 ;worker itself is immutable, keeping configuration only

(defn try-swap!
  [a f]
  (let []
    (loop []
      (let [value @a,
            res (f value),
            maybe-validator (get-validator a),
            validator (if (nil? maybe-validator)
                        (fn [_] true)
                        maybe-validator)]
        (if-not (validator res)
          [:error value]
          (if (compare-and-set! a value res)
            [:ok res]
            (recur)))))))

(defn try-take-from-storage
  [state ware storage-atom amount]
  (let [bill (state :bill),
        buffer (state :buffer),
        required (get bill ware 0),
        have (get buffer ware 0),
        need (- required have),
        desired-amount (min need amount),
        took (loop [to-take desired-amount]
               (if (pos? to-take)
                 (let [[status res] (try-swap! storage-atom (fn [n] (- n to-take)))]
                   (case status
                     :ok to-take
                     :error (recur res)))
                 0))]
    (assoc state :buffer
      (assoc buffer ware
        (+ have took)))))

(defn ready-to-produce?
  [state]
  (let [bill (state :bill),
        buffer (state :buffer)]
    (every? 
      (fn [[ware required]]
        (>= (get buffer ware 0) required))
      bill)))

(defn try-produce
  [state]
  (if (ready-to-produce? state)
    (let [bill (state :bill),
          buffer (state :buffer),
          duration (state :duration),
          amount (state :amount),
          target-storage (state :target-storage)
          production-line (state :production-line)]
      (send-off production-line 
                (fn [_]
                  (Thread/sleep duration)
                  (send (target-storage :worker) supply-msg amount)
                  nil))
      (assoc state :buffer
             (reduce-kv 
               (fn [m ware have] (assoc m ware (- have (get bill ware 0)))) 
               {} 
               buffer)))
    state))

(def notify-msg
  "A message that can be sent to a factory worker to notify that the provided 'amount' of 'ware's are
   just put to the 'storage-atom'."
   ;; 'state' is for agent created in 'factory', see comments in its code for details
   ;;The implementation should:
   ;; - try to retrieve some items from the 'storage-atom' if necessary
   ;; - if the retrieval is not successful, do not forget to handle validation exception correctly 
   ;; - if the retrieval is successful, put wares into the internal ':buffer'
   ;; - when there are enough wares of all types according to :bill, a new cycle must be started with given duratin;
   ;;   after it finished all the wares must be removed from the internal ':buffer' and ':target-storage' must be notified  
   ;;   with 'supply-msg'
   ;; - return new agent state with possibly modified ':buffer' in any case!
  (comp try-produce try-take-from-storage))

;;;
(def safe-storage (storage "Safe" 1))
(def safe-factory (factory 1 3000 safe-storage "Metal" 3))
(def cuckoo-clock-storage (storage "Cuckoo-clock" 1))
(def cuckoo-clock-factory (factory 1 2000 cuckoo-clock-storage "Lumber" 5 "Gears" 10))
(def gears-storage (storage "Gears" 20 cuckoo-clock-factory))
(def gears-factory (factory 4 1000 gears-storage "Ore" 4))
(def metal-storage (storage "Metal" 5 safe-factory))
(def metal-factory (factory 1 1000 metal-storage "Ore" 10))
(def lumber-storage (storage "Lumber" 20 cuckoo-clock-factory))
(def lumber-mill (source 5 4000 lumber-storage))
(def ore-storage (storage "Ore" 10 metal-factory gears-factory))
(def ore-mine (source 2 1000 ore-storage))

;;;runs sources and the whole process as the result
(defn start []
  (.start ore-mine)
  (.start lumber-mill))

;;;stopes running process
;;;recomplile the code after it to reset all the process
(defn stop []
  (.stop ore-mine)
  (.stop lumber-mill))

;;;This could be used to aquire errors from workers
;;;(agent-error (gears-factory :worker))
;;;(agent-error (metal-storage :worker))
