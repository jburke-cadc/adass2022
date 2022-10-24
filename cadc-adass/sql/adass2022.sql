CREATE SCHEMA cadcmisc;

DROP TABLE IF EXISTS cadcmisc.adass2022;

CREATE TABLE cadcmisc.adass2022 (
    code character varying(6) NOT NULL PRIMARY KEY,
    type character varying(24) NOT NULL,
    state character varying(16) NOT NULL,
    title character varying(1024) NOT NULL,
    abstract text NOT NULL,
    speaker_code character varying(6) NOT NULL,
    speaker_name character varying(128) NOT NULL,
    speaker_email character varying(128) NOT NULL,
    username character varying(8) NOT NULL,
    password character varying(16) NOT NULL,
    folderUrl character varying(256) NOT NULL
);

grant usage on schema cadcmisc to public;

grant select on all tables in schema cadcmisc to public;
