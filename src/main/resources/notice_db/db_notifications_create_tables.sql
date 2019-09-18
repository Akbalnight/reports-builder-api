drop table if exists {scheme.name}.notifications;
drop sequence if exists {scheme.name}.notifications_id_seq;
CREATE TABLE {scheme.name}.notifications(
	"time" timestamp without time zone DEFAULT current_timestamp, 
    id uuid default uuid_generate_v4(),
    idinitiator integer,
	idUser integer,
	idObject varchar(1024), 
	idType integer,
	idParent uuid,
	status json default '{"read":"no","important":"no","deleted":"no"}'::json,
	description varchar(1024));
