# hbase-util

## Utilites for making it easier to interact with hbase

## Usage
### Working with local hbase cluster
 - lein repl

### Working with a distributed hbase cluster
 - mvn clean package
 - Copy target/hbase-util-*.zip to target machine
 - Unzip it on target machine
 - Run ./bin/hbase-util from within the extracted folder

[bin/hbase-util](./bin/hbase-util) expects..
 - 'hbase' script to be on the classpath.
 -  HBASE_CONF_DIR and HADOOP_CONF_DIR env variables set.

## Current set of features
### Create hbase tables
```clojure
> (doc create)
-------------------------
hbase-util.table.create/create
([f])
  Reads tables configuration from file 'f' and
creates the corresponding tables

> (create "conf/tables.yml")
```

### Reset hbase tables
```clojure
> (doc reset)
-------------------------
hbase-util.table.reset/reset
([f])
  Like (hbase shell's ) truncate, but doesn't delete/create
the tables, but instead, for each table..
  - Disables it
  - Deletes and re-creates all column familes
  - Enables the table again
This comes in handy if the logged-in user
has limited permissions. It also preservers
split information, unlike truncate.

> (reset "conf/tables.yml")
```

### Verify hbase tables
```clojure
> (doc verify)
-------------------------
hbase-util.table.verify/verify
([in out])
  Reads tables configuration from 'in' file, as yaml,
 and dumps any difference found in 'out' file, in yaml format

> (verify "conf/tables.yml" "/tmp/verfied.yml")
```

### Configuration

Tables configuration is specified in yaml format [see dev-tables.yml](./conf/dev-tables.yml).
This same yaml file serves both to 'create', 'reset' and 'verify' .

Splits for a table can be specified (in the input yaml conf file) either as
* ```
file: path/to/file
```

* ```
info: {negions: 16, algo: "HexStringSplit", first-row: "0", last-row "F"}
```

* ```
vals: ["0", "1", "2", "3"]
```

#### Split file
Table creation and validation routines assume that the non-printable characters in the split
file are hex-encoded. Note: this code honours lower-case hex digits like \xfe which is not
honoured by hbase currently
