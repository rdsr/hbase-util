(ns hbase-util.core
  (:require [hbase-util.table.create :as c]
            [hbase-util.table.verify :as v]))

(def create c/create)
(def create-noadmin c/create-noadmin)
(def verify v/verify)
