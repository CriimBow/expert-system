(load "util")

(defn exit-parser [nl]
  (println "Parser: invalid syntaxe line:" (inc nl))
  (System/exit 0))

; --- EXP
(defn make-parse-arity-2 [op next]
  (fn parse-fn
    ([exps]
     (if (in? exps op)
       (parse-fn '() exps)
       (next exps)))
    ([exps-befor exps-after]
     (let [frst (first exps-after)
           scnd (second exps-after)
           thrd (second (rest exps-after))]
       (cond
         (= thrd nil)  (parse-fn (reverse exps-befor))
         (= scnd op)   (parse-fn
                        (concat (reverse (conj exps-befor (list op frst thrd))) (rest (rest (rest exps-after)))))
         :else         (parse-fn (conj exps-befor frst) (rest exps-after)))))))

(def parse-equival (make-parse-arity-2 :equival identity))
(def parse-impl-right (make-parse-arity-2 :impl-right parse-equival))
(def parse-impl-left (make-parse-arity-2 :impl-left parse-impl-right))
(def parse-xor (make-parse-arity-2 :xor parse-impl-left))
(def parse-or (make-parse-arity-2 :or parse-xor))
(def parse-and (make-parse-arity-2 :and parse-or))

(defn parse-neg
  ([with-par]
   (if (in? with-par :neg)
     (parse-neg '() with-par)
     (parse-and with-par)))
  ([exp with-par]
   (let [frst (first with-par)
         rst  (rest with-par)]
     (cond
       (= frst nil)                                   (parse-neg (reverse exp))
       (and (= frst :neg) (not (= (first rst) :neg))) (parse-neg (conj exp (list :neg (first rst))) (rest rst))
       :else                                          (parse-neg (conj exp frst) rst)))))

(defn parse-exp
  ([nl tokens]
   (if (in? tokens :par-open)
     (parse-exp '() '() 0 nl tokens)
     (first (parse-neg tokens))))
  ([exp in-par nb-par-open nl tokens]
   (let [frst (first tokens)
         rst  (rest tokens)]
     (cond
       (= frst nil)                                (if (= nb-par-open 0)
                                                     (parse-exp nl (reverse exp))
                                                     (exit-parser nl))
       (and (= frst :par-open) (= nb-par-open 0))  (parse-exp exp in-par (inc nb-par-open) nl rst)
       (and (= frst :par-open) (> nb-par-open 0))  (parse-exp exp (conj in-par frst) (inc nb-par-open) nl rst)
       (and (= frst :par-close) (= nb-par-open 1)) (parse-exp (conj exp (list :par (parse-exp (reverse in-par)))) '() (dec nb-par-open) nl rst)
       (and (= frst :par-close) (> nb-par-open 1)) (parse-exp exp (conj in-par frst) (dec nb-par-open) nl rst)
       (> nb-par-open 0)                           (parse-exp exp (conj in-par frst) nb-par-open nl rst)
       :else                                       (parse-exp (conj exp frst) in-par nb-par-open nl rst)))))

(defn make-parse-simple [tok]
  (fn [nl tokens]
    (let [frst (first tokens)
          rst  (rest tokens)]
      (if (and (= frst tok) (not (some #(not (char? %)) rst)))
        tokens
        (exit-parser nl)))))

(def parse-queries (make-parse-simple :queries))
(def parse-facts (make-parse-simple :facts))

(defn tokens->exp [nl tokens]
  (cond
    (in? tokens :queries)       (parse-queries nl tokens)
    (in? tokens :facts)         (parse-facts nl tokens)
    :else                       (parse-exp nl tokens)))

; --- PARSER
(defstruct parser-struct :queries :facts :exps)

(defn parser [l-tokens]
  (println "--- TEST")
  (let [tmp (map-indexed tokens->exp l-tokens)]
    (doseq [l tmp]
      (println l)))
  (comment
    (let [exps   (map tokens->exp l-tokens)
          st-rev (reduce
                  (fn [st exp]
                    (cond
                      (char? exp)              (struct parser-struct (:queries st) (:facts st) (conj (:exps st) exp))
                      (= (first exp) :queries) (struct parser-struct (rest exp) (:facts st) (:exps st))
                      (= (first exp) :facts)   (struct parser-struct (:queries st) (rest exp) (:exps st))
                      :else                    (struct parser-struct (:queries st) (:facts st) (conj (:exps st) exp))))
                  (struct parser-struct '() '() '())
                  exps)]
      (struct parser-struct (:queries st-rev) (:facts st-rev) (reverse (:exps st-rev))))))
