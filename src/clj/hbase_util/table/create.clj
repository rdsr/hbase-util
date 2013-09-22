(ns hbase-util.table.create
  (:require [hbase-util.util :as u]
            [hbase-util.config :as c])
  (:use [hbase-util.vars :only (admin)]))

(defn- create-table
  [cfg]
  "Creates a table from the supplied config"
  (.createTable admin
                (c/table-descriptor cfg)
                (c/split-keys cfg)))

(defn- create-tables
  [cfg]
  (doseq [tbl-cfg cfg]
    (create-table tbl-cfg)))

(defn create
  "Reads tables configuration from file 'f' and
creates the corresponding tables"
  [f] (create-tables (u/read-cfg f)) 'done)
