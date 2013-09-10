(ns hbase-util.table.create
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [hbase-util.util :as u])
  (:import [java.io File]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util Bytes RegionSplitter]
           [org.apache.hadoop.hbase.client HBaseAdmin HTable]))

(def conf (HBaseConfiguration/create))
(def admin (HBaseAdmin. conf))

;; processing splits

(defn strs->bytes
  "Converts a collection of 'string' split keys to a 2d byte array"
  [split-keys]
  (into-array (map u/to-bytes split-keys)))

(defn read-splits
  "Reads splits from file as strings"
  [f] (line-seq (io/reader f)))

(defn create-splits
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
  :vals [{:keys [splits]}] (strs->bytes (:vals splits)))

(defmethod split-keys
  :file [{:keys [splits]}] (strs->bytes (read-splits (:file splits))))

(defmethod split-keys
  :info [{:keys [splits]}] (create-splits (:info splits)))


(defn column-descriptor
  [{:keys [id] :as cfg}]
  (let [hcd (HColumnDescriptor. (name id))]
    (doseq [[k v] (dissoc cfg :id)]
      (.setValue hcd (-> k name s/upper-case) (str v)))
    hcd))

(defn column-descritors
  [{:keys [column-families]}]
  (map column-descriptor column-families))

(defn table-descriptor
  [{:keys [id] :as cfg}]
  (let [htd (HTableDescriptor. (name id))]
    (doseq [[k v] (dissoc cfg :column-families :splits :id)]
      (.setValue htd (-> k name s/upper-case) (str v)))
    (doseq [cf (column-descritors cfg)]
      (.addFamily htd cf))
    htd))

(defn create-table
  [cfg]
  (.createTable
   admin (table-descriptor cfg) (split-keys cfg)))

(defn create-tables
  [cfg]
  (doseq [tbl-cfg cfg]
    (create-table tbl-cfg))
  'ok)
