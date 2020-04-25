FROM cassandra:latest

EXPOSE 7000
EXPOSE 7001
EXPOSE 7199
EXPOSE 9042
EXPOSE 9160

COPY create.sql  create.sql
COPY run-db.sh run-db.sh

ENV CASSANDRA_KEYSPACE 1

ENTRYPOINT ["./run-db.sh"]
CMD ["cassandra", "-f"]
