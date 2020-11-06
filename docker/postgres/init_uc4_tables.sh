#!/bin/bash

echo "Copy config from /uc4"
cp /uc4/postgresql.conf /var/lib/postgresql/data/postgresql.conf 2> /dev/null

echo
echo "Creating tables for the following services: $SERVICES"

echo
for table in $SERVICES; do
  psql -U $POSTGRES_USER \
  -c "CREATE USER uc4$table WITH LOGIN ENCRYPTED PASSWORD 'uc4$table';" \
  -c "CREATE DATABASE uc4$table WITH OWNER=uc4$table;"
done

echo
for table in $SERVICES; do
  psql -U uc4$table -d postgres \
    -c "\c \"uc4$table\"" \
    -c "CREATE TABLE IF NOT EXISTS journal (
      ordering BIGSERIAL,
      persistence_id VARCHAR(255) NOT NULL,
      sequence_number BIGINT NOT NULL,
      deleted BOOLEAN DEFAULT FALSE,
      tags VARCHAR(255) DEFAULT NULL,
      message BYTEA NOT NULL,
      PRIMARY KEY(persistence_id, sequence_number)
    );" \
    -c "CREATE UNIQUE INDEX journal_ordering_idx ON journal(ordering);" \
    -c "CREATE TABLE IF NOT EXISTS snapshot (
      persistence_id VARCHAR(255) NOT NULL,
      sequence_number BIGINT NOT NULL,
      created BIGINT NOT NULL,
      snapshot BYTEA NOT NULL,
      PRIMARY KEY(persistence_id, sequence_number)
    );" \
    -c "CREATE TABLE read_side_offsets (
      read_side_id VARCHAR(255), tag VARCHAR(255),
      sequence_offset bigint, time_uuid_offset char(36),
      PRIMARY KEY (read_side_id, tag)
    );"
done