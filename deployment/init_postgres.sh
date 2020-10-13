for table in "uc4user" "uc4course" "uc4authentication" "uc4certificate"; do
  kubectl exec --stdin svc/postgres -n postgres -- psql -U postgresadmin -d postgres \
  -c "CREATE USER $table WITH LOGIN ENCRYPTED PASSWORD '$table';" \
  -c "CREATE DATABASE $table WITH OWNER=$table;"
done

for table in "uc4user" "uc4course" "uc4authentication" "uc4certificate"; do
  kubectl exec --stdin svc/postgres -n postgres -- psql -U $table -d postgres \
    -c "\c \"$table\"" \
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