(ns hbase-util.config
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [hbase-util.util :as u])
  (:use [hbase-util.vars :only (conf)])
  (:import [hbase_util Util]
           [org.apache.hadoop.hbase HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util RegionSplitter]))

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
  [{:keys [splits]}] (u/strs->bytes (:vals splits)))

(defmethod split-keys
  :file
  [{:keys [splits]}] (u/strs->bytes (read-splits (:file splits))))

(defmethod split-keys
  :info
  [{:keys [splits]}] (create-splits (:info splits)))

(defmethod split-keys
  :default [_] nil)

(defn column-descriptor
  "Creates a HColumnDescriptor from a 'cfg' map"
  [{:keys [id] :as cfg}] ;;  id maps to a column-name
  (let [hcd (HColumnDescriptor. (name id))]
    (doseq [[k v] (dissoc cfg :id)]
      (.setValue hcd (-> k name s/upper-case) (str v)))
    hcd))

(defn column-descriptors
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
