# jbpm-postgresql-lo-cleaner

A tool to delete large objects in PostgreSQL for jBPM.

## Environment
BPMS 6.3.0
BPMS 6.4.0

PostgreSQL 9

## Why don't we use vacuumlo?

vacuumlo analyzes database to find orphaned large objects. But it only looks at 'oid' columns. In jBPM, there are 'text' columns which hold large objects. In Java codes, they are String @Lob fields which are considered as "CLOB". These 'text' columns are not analyzed so vacuumlo could delete non-orphaned (= active) large objects.

jbpm-postgresql-lo-cleaner analyzes all columns which are associated to large objects so safely deletes orphaned large objects.


## How to use

- Edit jbpm-postgresql-lo-cleaner.properties to meet your environment
- $ jbpm-postgresql-lo-analyzer.sh
 - It only analyze tables and log the information to output-analyze.log. If you hit an Exception, don't go ahead with jbpm-postgresql-lo-cleaner.sh. Please let me know.  messages like "ERROR: relation "deploymentstore" does not exist" are okay to ignore because you just don't have the table.
- $ jbpm-postgresql-lo-cleaner.sh
 - It analyzes tables and also deletes orphaned large objects. Logs will go to output-delete.log

