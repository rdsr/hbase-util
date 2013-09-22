(ns hbase-util.table.create
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [hbase-util.vars :as v]
            [hbase-util.util :as u])
  (:use [hbase-util.vars :only (conf admin)]
        [clojure.tools.logging :only (info warn error)])
  (:import [java.io File]
           [hbase_util Util]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util Bytes RegionSplitter]
           [org.apache.hadoop.hbase.client HBaseAdmin HTable]))

;; processing splits

(defn- read-splits
  "Reads splits from file as strings"
  [f] (line-seq (io/reader f)))

(defn- create-splits
  "Creates splits as 2d byte array from information given"
  [{:keys [nregions algo first-row last-row]}]
  (let [rs (RegionSplitter/newSplitAlgoInstance conf algo)]
    (doto rs
      (.setFirstRow first-row)
      (.setLastRow last-row))
    (.split rs nregions)))

(defmulti split-keys
  (fn [{:keys [splits]}]
    (some #{:vals :file :info} (keys splits))))

(defmethod split-keys
  :vals
  [{:keys [splits]}] (strs->bytes (:vals splits)))

(defmethod split-keys
  :file
  [{:keys [splits]}] (strs->bytes (read-splits (:file splits))))

(defmethod split-keys
  :info
  [{:keys [splits]}] (create-splits (:info splits)))

(defmethod split-keys
  :default [_] nil)

(defn- column-descriptor
  "Creates a HColumnDescriptor from a 'cfg' map"
  [{:keys [column-name] :as cfg}]
  (let [hcd (HColumnDescriptor. (name column-name))]
    (doseq [[k v] (dissoc cfg :id)]
      (.setValue hcd (-> k name s/upper-case) (str v)))
    hcd))

(defn- column-descriptors
  [{:keys [column-families]}]
  (map column-descriptor column-families))

(defn table-descriptor
  "Creates a HTableDescriptor from a 'cfg' map"
  [{:keys [id] :as cfg}]
  (let [htd (HTableDescriptor. (name id))]
    (doseq [[k v] (dissoc cfg :column-families :splits :id)]
      (.setValue htd (-> k name s/upper-case) (str v)))
    (doseq [cf (column-descriptors cfg)]
      (.addFamily htd cf))
    htd))

(defn- create-table
  [cfg]
  (.createTable admin
                (table-descriptor cfg)
                (split-keys cfg)))

(defn- create-tables
  [cfg]
  (doseq [tbl-cfg cfg]
    (create-table tbl-cfg)))

(defn create
  "Reads tables configuration from file 'f' and
creates the corresponding tables"
  [f] (create-tables (u/read-cfg f)) 'done)

;; ----

(defn- truncate-column-family
  [table-name column-descriptor]
  (let [table-name (name table-name)
        table-descriptor (.getTableDescriptor admin (u/to-bytes table-name))
        id (.getNameAsString column-descriptor)]
    (when (.hasFamily table-descriptor (u/to-bytes id))
      ;(println "deleting column family" id "in table" table-name)
      (.deleteColumn admin table-name id))
    ;(println "creating column family" id "in table" table-name)
    (.addColumn admin table-name column-descriptor)))

(defn- truncate-column-families
  [{:keys [id] :as cfg}]
  (doseq [cf (column-descriptors cfg)]
    (create-cf-noadmin id cf)))

(defn- truncate-table-limited-perms
  [{:keys [id] :as cfg}]
  (let [id (name id)]
    (if (.tableExists admin id)
      (do (when (.isTableEnabled admin id)
          (warn "disabling table " id)
          (.disableTable admin id))
          (create-cfs-noadmin cfg)
          (warn "enabling table " id)
          (.enableTable admin id))
      (error "table" id "doesn't exist"))))

(defn- reset-tables
  [cfg]
  (doseq [tbl-cfg cfg]
    (create-table-noadmin tbl-cfg)))

(defn reset
    "Like truncate, but doesn't delete/create
the tables, but instead disables table, deletes
and re-creates all column familes, and then
enables tables again. This comes in handy
if the logged-in user has limited permissions.
TODO: mention about split info preservation"
  [f]
  (reset-tables (u/read-cfg f)) 'done)
