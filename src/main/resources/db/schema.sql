CREATE TABLE IF NOT EXISTS report (
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
CREATE sequence IF NOT EXISTS report_id_seq;
-- 07.06.19 HLASKUTE-1139: UPDATE schema add view to provide additional columns
CREATE VIEW history_values_by_hour_view AS
    SELECT h.*, p.full_name as "pipeline_full_name", w.full_name as "highway_full_name"
    FROM history_values_by_hour h
	LEFT JOIN pipeline p on p.id = h.pipeline_id
	LEFT JOIN highway w on w.id = h.highway_id;

CREATE VIEW history_values_by_day_view AS
    SELECT h.*, p.full_name as "pipeline_full_name", w.full_name as "highway_full_name"
    FROM history_values_by_day h
	LEFT JOIN pipeline p on p.id = h.pipeline_id
	LEFT JOIN highway w on w.id = h.highway_id;
