drop table if exists {scheme.name}.notifications_data;
CREATE TABLE {scheme.name}.notifications_data(
        id bigint not null unique,
        commonTypeId bigint not null,
        description varchar(1024) not null,
        email_title varchar(256),
        email_body varchar(1024),
        props json);
