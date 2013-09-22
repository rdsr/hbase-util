(defproject hbase-util "0.1.0"
  :description "Little things to make life easier working on Hbase"
  :source-paths ["src/clj"]
  :java-source-paths ["src/jvm"]
  :repl-options {:prompt (fn [_] "> ")
                 :welcome (println "Hbase utility shell")
                 :init-ns hbase-util.core}
  :dependencies [[clj-yaml "0.4.0"]
                 [org.apache.hadoop/hadoop-common "0.23.8" :scope "provided"]
                 [org.apache.hbase/hbase "0.94.5.6.1302190003" :scope "provided"]
                 [org.apache.zookeeper/zookeeper "3.4.5" :scope "provided"]
                 [org.clojure/tools.logging "0.2.6"]
                 [reply "0.2.1"]
                 [org.clojure/clojure "1.5.1"]])
