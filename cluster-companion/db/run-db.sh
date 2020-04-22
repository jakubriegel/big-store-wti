#!/bin/bash

if [[ -n "$CASSANDRA_KEYSPACE" && $1 = 'cassandra' ]]; then
  until cqlsh --file "create.sql"; do
    echo "Cassandra still unavailable" >> start_log
    sleep 2
  done && echo "provisioning successful" >> start_log &
fi

exec /docker-entrypoint.sh "$@"
