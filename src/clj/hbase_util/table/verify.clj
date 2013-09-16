(ns hbase-util.table.verify
  (:require [clojure.set :as set]
            [clj-yaml.core :as yaml]
            [hbase-util.util :as u]
            [hbase-util.table.create :as c])
  (:use [hbase-util.vars :only (conf admin)])
  (:import [java.io File]
           [hbase_util Util]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util Bytes]
           [org.apache.hadoop.hbase.client HBaseAdmin HTable]))

(defn splits
  [{:keys [id] :as cfg}]
  (let [id (name id)
        f  (fn [type]
             (str "/tmp/" id ".splits." (name type)))
        expected (-> cfg c/split-keys u/splits->strs)
        actual   (-> conf (HTable. id) .getStartKeys u/splits->strs rest)]
    (if (= expected actual)
      {:file (u/spit-seq actual (f :actual))}
      {:file-expected (u/spit-seq expected (f :expected))
       :file-actual (u/spit-seq actual (f :actual))})))

(defn diff [m1 m2]
  (set/difference
   (-> m1 keys set) (-> m2 keys set)))

(defn comm [m1 m2]
  (set/intersection
   (-> m1 keys set) (-> m2 keys set)))

(defn values
  [me ma]
  (merge
   (reduce (fn [m k] (assoc m k [(get me k) :xxx])) {} (diff me ma))
   (reduce (fn [m k] (assoc m [:xxx (get ma k)])) {} (diff ma me))
   (reduce (fn [m k]
             (if (= (get me k) (get ma k))
               m (assoc m k [(get me k) (get ma k)])))
           {} (comm me ma))))

(defn ibw->str
  [mv]
  (reduce
   (fn [m [k v]]
     (assoc m (-> k .get Bytes/toString) (-> v .get Bytes/toString)))
   {} mv))

(defn col-family
  [cfe cfa]
  (cond
   (nil? cfe) {:id (.getNameAsString cfe) :status "expected, but missing"}
   (nil? cfa) {:id (.getNameAsString cfa) :status "unexpected but present"}
   :default (merge {:id (.getNameAsString cfa)}
                   (values (-> cfe .getValues ibw->str)
                           (-> cfa .getValues ibw->str)))))

(defn col-family-pairs
  ""
  [tde tda]
  (loop [[cfe & erest] (-> tde .getFamilies seq)
         [cfa & arest] (-> tda .getFamilies seq)
         acc []]
    (if (and (nil? cfe) (nil? cfa))
      acc
      (let [ide (.getNameAsString cfe)
            ida (.getNameAsString cfa)
            cmp (compare ide ida)]
        (cond
         (zero? cmp) (recur erest arest (conj acc [cfe cfa]))
         (neg? cmp)  (recur erest (cons cfa arest) (conj acc [cfe nil]))
         (pos? cmp)  (recur (cons cfe erest) arest (conj acc [nil cfa])))))))

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
    (if (.tableExists admin id)
      (let [actual (.getTableDescriptor admin (.getBytes id))]
        (merge {:id id
                :splits (splits cfg)
                :column-families (col-families expected actual)}
               (values (-> expected .getValues ibw->str)
                       (-> expected .getValues ibw->str))))
      {:id id
       :status "expected, but missing"})))

(defn- tables
  [cfg f]
  (spit f (yaml/generate-string (map table cfg))))

(defn verify
  "Reads tables configuration from 'in' file as yaml
 and dump any difference found in 'out' in yaml format"
  [in out] (tables (u/read-cfg in) out) 'done)
