DROP DATABASE backup_restore_test_db;

CREATE DATABASE backup_restore_test_db;

\connect backup_restore_test_db

DO
$body$
BEGIN
   IF NOT EXISTS (
      SELECT
      FROM   pg_catalog.pg_user
      WHERE  usename = 'AtsUser') THEN

      CREATE USER "AtsUser" WITH SUPERUSER CREATEDB LOGIN PASSWORD 'AtsPassword';
   END IF;
END
$body$;

CREATE SCHEMA IF NOT EXISTS "AtsUser" AUTHORIZATION "AtsUser";

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA "AtsUser" TO "AtsUser";


CREATE TABLE IF NOT EXISTS public."people" (
    id integer PRIMARY KEY,
    firstname character varying(50),
    lastname character varying(50),
    age integer
);


ALTER TABLE public."people" OWNER TO AtsUser;

INSERT INTO people VALUES (1,'Chuck','Norris',62);
INSERT INTO people VALUES (2,'Jackie','Chan',52);
INSERT INTO people VALUES (3,'Bruce','Lee',32);
INSERT INTO people VALUES (4,'Al','Bundy',72);
INSERT INTO people VALUES (5,'Indiana','Jones',62);



