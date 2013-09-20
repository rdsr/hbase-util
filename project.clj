(defproject hbase-util "0.1.0"
  :description "Little things to make life easier working on Hbase"
  :source-paths ["src/clj"]
  :java-source-paths ["src/jvm"]
  :resource-paths ["conf"]
  :repl-options {:prompt (fn [_] "=> ")
                 :welcome (println "Hbase utility shell")
                 :init-ns hbase-util.core}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-yaml "0.4.0"]
                 [reply "0.2.1"]
                 [org.apache.hbase/hbase "0.94.10"]
                 [org.apache.hadoop/hadoop-core "1.1.2"]
                 [org.apache.zookeeper/zookeeper "3.4.5"]])
  ;:repositories {"yahoo" "http://ymaven.corp.yahoo.com:9999/proximity/repository/public"})
