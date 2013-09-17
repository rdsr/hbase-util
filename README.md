# hbase-util

## Utilites for making it easier to interact with hbase

## Usage
* copy hase-site.xml under conf
* run bin/hbase-util

## Current set of utilities
### Create (and verifying) hbase tables
```clojure
=> (create "conf/tables.yml")
```

Tables configuration is specified in yaml format [see is-tables.yml](./conf/is-tables.yml).
This same yaml file serves both for creation and for later verification purposes.
Say, for instance, the tables were created by someone else, it can be verified whether
the tables were correctly created by running the below command

```clojure
=> (verify "conf/tables.yml" "/tmp/output")
```
/tmp/output is the file where differences in configuration if any between the corresponding
tables are stored, in yaml format.


### Table configuration
Splits for a table can be specified (in the input yaml conf file) either as
* ```yaml
splits:
  file : path/to/file
```
* ```yaml
splits:
  info : {negions: 16, algo: "HexStringSplit" first-row: "0" last-row "F"}
```
* ```yaml
splits:
  vals : ["0", "1", "2", "3"]
````

#### Split file
Table creation and validation routines assume that the non-printable characters in the split
file are hex-encoded. Note: this code honours lower-case hex digits like \xfe which is not
honoured by hbase currently
