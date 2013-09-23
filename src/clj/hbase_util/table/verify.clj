(ns hbase-util.table.verify
  (:require [clojure.set :as set]
            [clj-yaml.core :as yaml]
            [hbase-util.util :as u]
            [hbase-util.table :as t]
            [hbase-util.config :as c])
  (:use [hbase-util.vars :only (conf admin)])
  (:import [java.io File]
           [hbase_util Util]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util Bytes]
           [org.apache.hadoop.hbase.client HBaseAdmin HTable]))

(defn splits
  "For a specific table, it computes the splits from 'cfg' and
also reads the actual splits from hbase table. A comparison
map is returned containing a single file, if the splits are the
same, else the map contains two files containing the expected
and actual splits"
  [{:keys [id] :as cfg}] ;; id maps to the table name
  (let [id (name id)
        f (fn [type] (str "/tmp/" id ".splits." (name type)))
        expected (-> cfg c/split-keys u/splits->strs)
        actual (t/split-keys id)]
    (if (= expected actual)
      {:file (u/spit-seq actual (f :no-change))}
      {:file-expected (u/spit-seq expected (f :expected))
       :file-actual (u/spit-seq actual (f :actual))})))

(defn remove-same
  "Removes entries where v1 and v2 are same"
  [m]
  (reduce (fn [m [k [v1 v2]]]
            (if (= v1 v2) m
                (assoc m k [v1 v2])))
          {} m))

(defn diff-map
  "Merges maps m1 and m2, entries in the result being
 either [(get m1 k) nil] or [nil (get m2 k)] or
 [(get m1 k) (get m2 k)]. If the values for a given key
 are same they are not included in the result"
  [m1 m2]
  (remove-same
   (merge-with
    (fn [[v1 _] [_ v2]] [v1 v2])
    (into {} (map (fn [[k v]] [k [v nil]]) m1))
    (into {} (map (fn [[k v]] [k [nil v]]) m2)))))


(defn column-diff
  "Compares the 'expected' and the 'actual' col-family pair.
Returns a map specifying the differences"
  [cfe cfa]
  (cond
   (nil? cfe) {:id (t/col-name cfa) :status :unexpected}
   (nil? cfa) {:id (t/col-name cfe) :status :expected}
   :default (assoc (diff-map (t/column-values cfe)
                           (t/column-values cfa))
              :id (t/col-name cfa))))

(defn column-map
  "Converts a coll of column descriptors to a map keyed on column names"
  [col-descs]
  (reduce (fn [m col-desc]
            (assoc m (t/col-name col-desc) col-desc))
          {} col-descs))

(defn column-pairs
  "Returns column family pairs. A nil is present if a column
family is missing in either expected or actual position."
  [tde tda]
  (vals (diff-map (-> tde .getFamilies column-map)
                  (-> tda .getFamilies column-map))))

(defn columns-diff
  [tde tda]
  (map #(apply column-diff %)
       (remove (fn [[exp act]] (= exp act))
               (column-pairs tde tda))))

(defn- table-diff
  "Constructs an 'expected' table descriptor from cfg given
and compares with the 'actual' table descriptor. Returns a
map specifying the differences"
  [{:keys [id] :as cfg}]
  (let [id (name id)
        expected (c/table-descriptor cfg)]
    (if (t/exists? id)
      (let [actual (t/table-descriptor id)]
        (merge {:id id
                :splits (splits cfg)
                :column-families (columns-diff expected actual)}
               (diff-map (t/table-values expected)
                         (t/table-values actual))))
      {:id id
       :status :expected})))

(defn- tables-diff
  [cfg f]
  (spit f (yaml/generate-string (map table-diff cfg))))

(defn verify
  "Reads tables configuration from 'in' file, as yaml,
 and dumps any difference found in 'out' file, in yaml format"
  [in out] (tables-diff (u/read-cfg in) out) 'done)
