create sequence IF NOT EXISTS {scheme.name}.emails_id_seq;

CREATE TABLE IF NOT EXISTS {scheme.name}.emails
(
 id bigint default nextval('{scheme.name}.emails_id_seq'),
 "time" timestamp without time zone DEFAULT current_timestamp,
 title varchar(255) NOT NULL,
 body varchar(2048) NOT NULL,
 receivers json NOT NULL,
 sended boolean DEFAULT false 
);
