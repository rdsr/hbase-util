(ns hbase-util.table
  (:require [hbase-util.util :as u])
  (:use [hbase-util.vars :only (conf admin)]
        [clojure.tools.logging :only (warn)])
  (:import [org.apache.hadoop.hbase.util Bytes]
           [org.apache.hadoop.hbase HColumnDescriptor]
           [org.apache.hadoop.hbase.client HTable]))

(defn disable [id]
  (warn "Disabling table " id)
  (.disableTable admin id))

(defn enable [id]
  (warn "Enabling table " id)
  (.enableTable admin id))

(defn exists? [id]
  (.tableExists admin id))

(defn enabled? [id]
  (.isTableEnabled admin id))

(defn table-descriptor
  [^String id]
  (.getTableDescriptor admin (u/to-bytes id)))

(defn col-name
  [^HColumnDescriptor col-desc] (.getNamesAsString col-desc))

(defn has-family?
  [^String tid ^String cid]
  (let [tbl-desc (-> tid u/to-bytes table-descriptor)]
    (.hasFamily tbl-desc cid)))

(defn delete-column
  [tid cid]
  (warn "Deleting column family" cid "in table" tid)
  (.deleteColumn admin tid cid))

(defn add-column
  [tid cid]
  (warn "Creating column family" cid "in table" tid)
  (.addColumn admin tid cid))

(defn- ibw->str
  "Transforms the input map '{ImmutableBytesWritable -> ImmutableBytesWritable}'
using str representation for keys and values"
  [mv]
  (reduce
   (fn [m [k v]]
     (assoc m (-> k .get Bytes/toString) (-> v .get Bytes/toString)))
   {} mv))

(defn table-values
  "Returns the values map in a table
The keys and vals are converted to string."
  [col-desc] (-> col-desc .getValues ibw->str))

(defn col-values
  "Returns the values map in a col-family.
The keys and vals are converted to string."
  [table-desc] (-> table-desc .getValues ibw->str))

(defn split-keys
  [id]
  (-> conf (HTable. id) .getStartKeys u/splits->strs rest))
