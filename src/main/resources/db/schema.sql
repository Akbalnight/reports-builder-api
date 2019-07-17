CREATE TABLE IF NOT EXISTS {reports.scheme.table} (
  id bigint NOT NULL,
  name varchar(255) DEFAULT NULL,
  title varchar(255) DEFAULT NULL,
  created_by varchar(255) DEFAULT NULL,
  query_descriptor text DEFAULT NULL,
  type varchar(255) DEFAULT NULL,
  is_public boolean,
  is_favorite boolean,
  description text DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE sequence IF NOT EXISTS {reports.scheme.table}_id_seq;
