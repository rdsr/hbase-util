(defproject hbase-util "0.1.0"
  :source-paths ["src/clj"]
  :java-source-paths ["src/jvm"]
  :resource-paths ["conf"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-yaml "0.4.0"]
                 [org.apache.hbase/hbase "0.94.5.6.1302190003"]
                 [org.apache.hadoop/hadoop-common "0.23.9"]
                 [org.apache.zookeeper/zookeeper "3.4.5"]
                 [org.codehaus.jackson/jackson-core-asl "1.7.1"]
                 [org.codehaus.jackson/jackson-mapper-asl "1.7.1"]
                 [org.codehaus.jackson/jackson-jaxrs "1.7.1"]
                 [org.codehaus.jackson/jackson-xc "1.7.1"]
                 [org.apache.hadoop/hadoop-common "0.23.8"
                  :exclusions [org.codehaus.jackson/jackson-core-asl
                               org.codehaus.jackson/jackson-mapper-asl
                               org.codehaus.jackson/jackson-jaxrs
                               org.codehaus.jackson/jackson-xc]]])
