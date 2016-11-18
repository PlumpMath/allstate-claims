(ns allstate-claims.handle-data
  (:require [clojure-csv.core :as csv]
            [clojure.java.io :as io]
            [semantic-csv.core :as sc]
            [huri.core :as h]
            [clojure.string :as str]))

#_(defn load-csv [filename]
    (-> filename
        slurp
        csv/parse-csv
        sc/mappify))

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

(defn cast-for-lgbm
  "Finds the categorical variables, dummifies all of them."
  [ms]
  (let [cols (map name (h/cols ms))
        cat-ks (map keyword (filter #(str/starts-with? % "cat") cols))]
    (dummify-all ms cat-ks)))

#_(def train-csv (load-csv "resources/train.csv"))
#_(def test-csv (load-csv "resources/test.csv"))

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

(def temp-train (take 100 train-csv))

(sc/spit-csv "resources/out/temp-train.csv" temp-train)
