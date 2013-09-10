(ns hbase-util.table.verify
  (:require [clojure.set :as set]
            [clj-yaml.core :as yaml]
            [hbase-util.util :as u]
            [hbase-util.table.create :as c])
  (:import [java.io File]
           [hbase_util Util]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration HTableDescriptor HColumnDescriptor]
           [org.apache.hadoop.hbase.util Bytes]
           [org.apache.hadoop.hbase.client HBaseAdmin HTable]))

(def ^:private conf (HBaseConfiguration/create))
(def ^:private admin (HBaseAdmin. conf))

(defn splits
  [{:keys [id] :as cfg}]
  (let [id (name id)
        expected (-> cfg c/split-keys u/splits->strs)
        actual (-> conf (HTable. id) .getStartKeys u/splits->strs rest)]
    (if (= expected actual)
      {:file (u/spit-seq actual (str id ".splits"))}
      {:file-expected (u/spit-seq expected (str id ".expected.splits"))
       :file-actual (u/spit-seq actual (str id ".actual.splits"))})))

(defn mdiff [m1 m2]
  (set/difference
   (-> m1 keys set) (-> m2 keys set)))

(defn mcomm [m1 m2]
  (set/intersection
   (-> m1 keys set) (-> m2 keys set)))

(defn values
  [me ma]
  (merge
   (reduce (fn [m k] (assoc m k [(get me k) :xxx])) {} (mdiff me ma))
   (reduce (fn [m k] (assoc m [:xxx (get ma k)])) {} (mdiff ma me))
   (reduce (fn [m k]
             (if (= (get me k) (get ma k))
               m (assoc m k [(get me k) (get ma k)])))
           {}
           (mcomm me ma))))

(defn ibw->str
  [mv]
  (reduce
   (fn [m [k v]]
     (assoc m (-> k .get Bytes/toString) (-> v .get Bytes/toString)))
   {} mv))

(defn col-family
  [cfe cfa]
  (cond
   (nil? cfe) {:id (.getNameAsString cfa) :state "expected but missing"}
   (nil? cfa) {:id (.getNameAsString cfe) :state "unexpected but present"}
   :default (merge {:id (.getNameAsString cfa)}
                   (values (-> cfe .getValues ibw->str)
                           (-> cfa .getValues ibw->str)))))

(defn col-family-pairs
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
         (neg? cmp) (recur erest (cons cfa arest) (conj acc [cfe nil]))
         (pos? cmp) (recur (cons cfa arest) arest (conj acc [nil cfa])))))))

(defn col-families
  [tde tda]
  (map #(apply col-family %) (col-family-pairs tde tda)))

(defn table
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
       :state "expected, but missing"})))

(defn tables
  [cfg]
  (spit "dumpp"
        (yaml/generate-string
         (map table cfg))))
