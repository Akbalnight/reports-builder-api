drop table if exists {scheme.name}.sendout;
drop sequence if exists {scheme.name}.sendout_id_seq;

CREATE TABLE {scheme.name}.sendout(
  "time" timestamp without time zone DEFAULT current_timestamp,
  id uuid default uuid_generate_v4(),
  idinitiator integer,
  typeId integer not null,
  objectId varchar(1024) not null,
  objects json,
  sended boolean not null default false);
