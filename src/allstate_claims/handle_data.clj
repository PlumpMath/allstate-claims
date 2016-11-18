(ns allstate-claims.handle-data
  (:require [clojure-csv.core :as csv]
            [clojure.core.reducers :as r]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [huri.core :as h]
            [semantic-csv.core :as sc]
            [clojure.edn :as edn]
            [taoensso.nippy :as nippy]
            [taoensso.nippy.compression :as ncomp])
  (:import [java.io DataOutputStream DataInputStream]))

;; Utils
(defn- keyword-starts-with? [keyword prefix]
  (str/starts-with? (name keyword) prefix))

(def cat-keys
  [:cat91 :cat44 :cat22 :cat105 :cat54 :cat67 :cat35 :cat52 :cat73 :cat69
   :cat37 :cat102 :cat31 :cat26 :cat87 :cat113 :cat82 :cat101 :cat85
   :cat17 :cat89 :cat18 :cat78 :cat13 :cat111 :cat30 :cat110 :cat107
   :cat108 :cat79 :cat58 :cat81 :cat3 :cat16 :cat10 :cat15 :cat86 :cat38
   :cat68 :cat41 :cat21 :cat104 :cat2 :cat115 :cat29 :cat8 :cat39 :cat59
   :cat14 :cat100 :cat61 :cat25 :cat84 :cat45 :cat72 :cat23 :cat36 :cat114
   :cat47 :cat20 :cat97 :cat1 :cat24 :cat95 :cat90 :cat27 :cat12 :cat96
   :cat60 :cat88 :cat56 :cat34 :cat46 :cat55 :cat43 :cat93 :cat40 :cat70
   :cat53 :cat63 :cat19 :cat112 :cat80 :cat103 :cat33 :cat66 :cat51
   :cat4 :cat5 :cat92 :cat57 :cat7 :cat62 :cat48 :cat75 :cat77 :cat64
   :cat83 :cat74 :cat9 :cat109 :cat49 :cat98 :cat42 :cat116 :cat11 :cat94
   :cat65 :cat106 :cat32 :cat76 :cat6 :cat50 :cat71 :cat99 :cat28])

(def cont-keys
  [:cont1 :cont2 :cont9 :cont5 :cont13 :cont4 :cont10 :cont7 :cont12
   :cont14 :cont6 :cont3 :cont8 :cont11])

;; IO
(defn load-csv [filename]
  (->> (slurp filename)
       csv/parse-csv
       sc/mappify
       (sc/cast-with sc/->double {:only cont-keys})
       (sc/cast-with sc/->int {:only [:id]})
       (sc/cast-with keyword {:only cat-keys})))

(defn write-file [filename data]
  (with-open [w (io/output-stream filename)]
    (nippy/freeze-to-out! (DataOutputStream. w) data)))

(defn read-file [filename]
  (with-open [r (io/input-stream filename)]
    (nippy/thaw-from-in! (DataInputStream. r))))

(if-not (.exists (io/file "resources/train.dat"))
  (let [train-csv (sc/cast-with sc/->double {:only [:loss]}
                                (load-csv "resources/train.csv"))
        test-csv (load-csv "resources/test.csv")]
    (do
      (println "Writing train.dat...")
      (time (write-file "resources/train.dat" train-csv))
      (println "Writing test.dat...")
      (time (write-file "resources/test.dat" test-csv)))))

(do
  (println "Reading train.dat...")
  (time (defonce train (read-file "resources/train.dat")))
  (println "Reading test.dat...")
  (time (defonce test (read-file "resources/test.dat"))))

;; Dummifying
(defn- append-to-key [k s]
  (keyword (str (name k) s)))

(defn dummify
  "Splits out a given key for a categorical feature (1, 2...k) into k dummy keys
  of (0, 1)."
  [ms k]
  (let [possible-values (sort (set (h/col k ms)))
        dummies (map (partial append-to-key k)
                     possible-values)]
    (for [line ms]
      (conj (reduce #(assoc %1 %2 0)
                    (dissoc line k)
                    dummies)
            [(append-to-key k (k line)) 1]))))

(defn dummify-all [ms ks]
  (reduce dummify ms ks))

(defn dummy-vals
  "Find the possible values of a given key in a dataframe"
  [k df]
  (into #{} (r/map k df)))

(defn pdummy-vals
  "Find the possible values of a given key in a dataframe"
  [k df]
  (into #{} (concat (pmap k (partition-all 512 df)))))

#_(time (dummy-vals :cat1 test-csv))
#_(time (pdummy-vals :cat1 test-csv))

(defn dummies
  "Find the the possible values for given keys in a dataframe, dumps them into a
  map: k->[v]"
  [df ks]
  (into {} (r/map
            (fn [k] [k (dummy-vals k df)])
            ks)))

#_(time (dummies tmp categories))
#_(time (defonce dummy-map (dummies train-csv categories)))

(defn cast-for-lgbm
  "Finds the categorical variables, dummifies all of them."
  [ms]
  (let [cat-ks (filter #(str/starts-with? (name %) "cat") (h/cols ms))]
    (dummify-all ms cat-ks)))

#_(def train-data (cast-for-lgbm train-csv))
#_(def test-data (cast-for-lgbm test-csv))

(defn process-csvs []
  (with-open [in-file (io/reader "resources/train.csv")
              out-file (io/writer "resources/out/train.csv")]
    (->> (csv/parse-csv in-file)
         sc/mappify
         cast-for-lgbm
         (sc/spit-csv out-file))))

;; Spit out the files for LGBM
;; (sc/spit-csv "resources/out/train.csv" train-data)
;; (sc/spit-csv "resources/out/test.csv" test-data)
