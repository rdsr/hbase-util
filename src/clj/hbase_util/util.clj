(ns hbase-util.util
  (:require [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [clojure.stacktrace :as e])
  (:import [hbase_util Util]
           [java.io PushbackReader]
           [org.apache.hadoop.security UserGroupInformation]
           [org.apache.hadoop.hbase.util Bytes]
           [org.apache.hadoop.hbase HBaseConfiguration]
           [org.apache.hadoop.hbase.client HTable Put]))

(defn read-cfg [f]
  (-> f slurp yaml/parse-string))

(defn secure?
  [conf]
  (= (.get conf "hbase.security.authentication" "simple") "kerberos"))

(defn strs->bytes
  "Converts a collection of 'string' keys (splits) to a 2d byte array"
  [split-keys]
  (into-array (map #(Util/toBytesBinary %) split-keys)))

(defn to-bytes
  [v] (Bytes/toBytes v))

(defn kinit [user keytab]
  (UserGroupInformation/loginUserFromKeytab user keytab))

(defn spit-seq
  [s f]
  (with-open [w (io/writer f)]
    (doseq [i s]
      (.write w (str i "\n"))))
  f)

(defn splits->strs
  [splits] (map #(Util/toStringBinary %) splits))

(defn print-root-cause
  []
  (-> *e
      e/root-cause
      e/print-stack-trace))
