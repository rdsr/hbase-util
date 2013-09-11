(ns hbase-util.vars
  (:import [java.io File]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration]
           [org.apache.hadoop.hbase.client HBaseAdmin]))

(def conf (HBaseConfiguration/create))
(def admin (HBaseAdmin. conf))
