(ns allstate-claims.core
  (:gen-class)
  (:require [me.raynes.conch :as sh]
            [allstate-claims.handle-data :as data]))

;; Bind lightgbm binary to clojure function
(sh/programs lightgbm)
