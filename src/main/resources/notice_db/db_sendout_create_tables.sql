CREATE TABLE IF NOT EXISTS {scheme.name}.sendout(
  "time" timestamp without time zone DEFAULT current_timestamp,
  id uuid default public.uuid_generate_v4(),
  idinitiator integer,
  typeId integer not null,
  objectId varchar(1024) not null,
  objects json,
  sended boolean not null default false);
