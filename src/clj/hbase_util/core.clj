(ns hbase-util.core
  (:require [hbase-util.table.create :as c]
            [hbase-util.table.reset :as r]
            [hbase-util.table.verify :as v]))

;; remove this hack
(def create c/create)
(def reset  r/reset)
(def verify v/verify)
