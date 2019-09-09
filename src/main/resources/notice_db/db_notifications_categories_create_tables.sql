drop table if exists {scheme.name}.notification_category;
CREATE TABLE {scheme.name}.notification_category(
  id bigserial primary key,
  value character varying not null unique
);
