(ns hbase-util.table.reset
  (:require [hbase-util.util :as u]
            [hbase-util.config :as c])
  (:use [hbase-util.table]
        [clojure.tools.logging :only (error)]))

(defn- reset-column
  "Deletes and recreates a colum-family. The column
family is created even if it didn't exist before"
  [tid col-desc]
  (let [tid (name tid)
        cid (col-name col-desc)]
    (when (has-family? tid cid)
      (delete-column tid cid))
    (add-column tid col-desc)))

(defn- reset-columns
  [{:keys [id] :as cfg}] ;; id maps to table name
  (doseq [col-desc (c/column-descriptors cfg)]
    (reset-column id col-desc)))

(defn- reset-table
  [{:keys [id] :as cfg}] ;; id maps to table name
  (let [id (name id)]
    (if (exists? id)
      (do (when (enabled? id)
            (disable id))
          (reset-columns cfg)
          (enable id))
      (error "table" id "doesn't exist"))))

(defn- reset-tables
  [cfg]
  (doseq [tbl-cfg cfg]
    (reset-table tbl-cfg)))

(defn reset
    "Like truncate, but doesn't delete/create
the tables, but instead:
  - Disable all tables,
  - Deletes and re-creates all column familes,
  - Enables tables again.
This comes in handy if the logged-in user
has limited permissions. It also preservers
split information, unlike truncate."
  [f]
  (reset-tables (u/read-cfg f)) 'done)
