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
      {:file (u/spit-seq actual (f :actual))}
      {:file-expected (u/spit-seq expected (f :expected))
       :file-actual (u/spit-seq actual (f :actual))})))

(defn diff
  "Returns the set difference of the keys from maps m1 and m2"
  [m1 m2]
  (set/difference
   (-> m1 keys set) (-> m2 keys set)))

(defn comm
  "Returns the set intersection of the keys from maps m1 and m2"
  [m1 m2]
  (set/intersection
   (-> m1 keys set) (-> m2 keys set)))

(defn values
  "Computes a difference map. If key is present in both
the input maps, the computed entry in the resulting difference
map will be [key [expected-value actual-value]]. If a key
is missing in either of the map a '' is added in it's place"
  [me ma] ;; expected and actual maps
  (merge
   (reduce (fn [m k] (assoc m k [(get me k) nil])) {} (diff me ma))
   (reduce (fn [m k] (assoc m [nil (get ma k)])) {} (diff ma me))
   (reduce (fn [m k]
             (if (= (get me k) (get ma k))
               m (assoc m k [(get me k) (get ma k)])))
           {} (comm me ma))))


(defn col-family
  "Compares the 'expected' and the 'actual' col-family pair.
Returns a map specifying the differences"
  [cfe cfa]
  (cond
   (nil? cfe) {:id (t/col-name cfe) :status "expected, but missing"}
   (nil? cfa) {:id (t/col-name cfa) :status "unexpected but present"}
   :default (merge {:id (t/col-name cfa)}
                   (values (t/descriptor-values cfe)
                           (t/descriptor-values cfa)))))

(defn col-family-pairs
  "Returns a seq of col-family pairs, each pair containing
the 'expected' and the 'actual' col-family. If a column
family is missing in either the expected table-descriptor
or the actual table-descriptor a nil is added in it's place.
Note: tde.getFamilies returns sorted col-family descriptors,
Also, the assumption is that there has to be atleast one
column-family descriptor in both table-descriptors"
  [tde tda]
  (loop [[cfe & erest] (-> tde .getFamilies seq) ;;todo check seq may not be required
         [cfa & arest] (-> tda .getFamilies seq)
         acc []]
    (if (and (nil? cfe) (nil? cfa))
      acc
      (let [ide (t/col-name cfe)
            ida (t/col-name cfa)
            cmp (compare ide ida)]
        (cond
         (zero? cmp) (recur erest arest (conj acc [cfe cfa]))
         (neg? cmp) (recur erest (cons cfa arest) (conj acc [cfe nil]))
         (pos? cmp) (recur (cons cfe erest) arest (conj acc [nil cfa])))))))

(defn col-families
  [tde tda]
  (map #(apply col-family %) (col-family-pairs tde tda)))

(defn- table
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
                :column-families (col-families expected actual)}
               (values (t/descriptor-values expected)
                       (t/descriptor-values actual))))
      {:id id
       :status "expected, but missing"})))

(defn- tables
  [cfg f]
  (spit f (yaml/generate-string (map table cfg))))

(defn verify
  "Reads tables configuration from 'in' file, as yaml,
 and dumps any difference found in 'out' file, in yaml format"
  [in out] (tables (u/read-cfg in) out) 'done)
