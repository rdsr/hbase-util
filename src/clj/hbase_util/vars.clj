(ns hbase-util.vars
  (:require [hbase-util.util :as u])
  (:import [java.io File]
           [org.apache.hadoop.conf Configuration]
           [org.apache.hadoop.hbase HBaseConfiguration]
           [org.apache.hadoop.hbase.client HBaseAdmin]))

(def conf (HBaseConfiguration/create))

;; Automatically login through JAAS client,
;; If not, uncomment this code.
;; (when (u/secure? conf)
;;   (u/kinit (System/getenv "grid_user")
;;            (System/getenv "grid_keytab")))

(def admin (HBaseAdmin. conf))
