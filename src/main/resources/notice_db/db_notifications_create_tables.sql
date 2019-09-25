CREATE TABLE IF NOT EXISTS {scheme.name}.notifications(
	"time" timestamp without time zone DEFAULT current_timestamp, 
    id uuid default public.uuid_generate_v4(),
    idinitiator integer,
	idUser integer,
	idObject varchar(1024), 
	idType integer,
	idParent uuid,
	status json default '{"read":"no","important":"no","deleted":"no"}'::json,
	description varchar(1024));
