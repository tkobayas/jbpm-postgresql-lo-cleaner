#!/bin/bash

java -cp target/jbpm-postgresql-lo-cleaner-1.0.0.jar:lib/postgresql-9.4.1212.jar -Ddelete.enabled=true com.redhat.gss.jbpm.PostgreSQLLOCleaner >> output-delete.log 2>&1