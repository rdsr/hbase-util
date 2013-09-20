(ns hbase-util.table.create
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [hbase-util.vars :as v]
            [hbase-util.util :as u])
  (:use [hbase-util.vars :only (conf admin)])
  (:import [java.io File]
           [hbase_util Util]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util Bytes RegionSplitter]
           [org.apache.hadoop.hbase.client HBaseAdmin HTable]))

;; processing splits

(defn- strs->bytes
  "Converts a collection of 'string' keys (splits) to
a 2d byte array"
  [split-keys]
  (into-array (map #(Util/toBytesBinary %) split-keys)))

;;(def hex-strs->bytes (partial strs->bytes #(Util/toBytesBinary %)))

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

(defn- column-descriptor
  "Creates a HColumnDescriptor from a 'cfg' map"
  [{:keys [id] :as cfg}]
  (let [hcd (HColumnDescriptor. (name id))]
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

(defn- create-cf-noadmin
  [tid cd]
  (let [tid (name tid)
        td (.getTableDescriptor admin (.getBytes tid))
        id (.getNameAsString cd)]
    (when (.hasFamily td (.getBytes id))
      (println "deleting column family" id "in table" tid)
      (.deleteColumn admin tid id))
    (println "creating column family" id "in table" tid)
    (.addColumn admin tid cd)))

(defn- create-cfs-noadmin
  [{:keys [id] :as cfg}]
  (doseq [cf (column-descriptors cfg)]
    (create-cf-noadmin id cf)))

(defn- create-table-noadmin
  [{:keys [id] :as cfg}]
  (let [id (name id)]
    (when-not (.tableExists admin id)
      (println "Table" id "doesn't exist"))
    (when (.isTableEnabled admin id)
      (println "disabling table " id)
      (.disableTable admin id))
    (create-cfs-noadmin cfg)
    (println "enabling table " id)
    (.enableTable admin id)))

(defn- create-tables-noadmin
  [cfg]
  (doseq [tbl-cfg cfg]
    (create-table-noadmin tbl-cfg)))

(defn create-noadmin
  [f]
  (create-tables-noadmin (u/read-cfg f)))
