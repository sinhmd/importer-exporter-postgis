-- CREATE_RO_USER.sql
--
-- Authors:     Felix Kunde <felix-kunde@gmx.de>
--
-- Copyright:   (c) 2007-2012, Institute for Geodesy and Geoinformation Science,
--                             Technische Universitšt Berlin, Germany
--                             http://www.igg.tu-berlin.de
--
--              This skript is free software under the LGPL Version 2.1.
--              See the GNU Lesser General Public License at
--              http://www.gnu.org/copyleft/lgpl.html
--              for more details.
-------------------------------------------------------------------------------
-- About:
--
--
-------------------------------------------------------------------------------
--
-- ChangeLog:
--
-- Version | Date       | Description     | Author
-- 1.0.0     2010-06-22   PostGIS version   FKun
--

-- the script has to be called by the psql-console
-- use psql -h host -p 5432 -U Username -d database to login
-- then execute the script: \i CREATE_RO_USER.sql
-- (it's more easy to use the pgAdminIII for that) 

\prompt 'Please enter a username for the read-only user: ' RO_USERNAME
\prompt 'Please enter a password for the read-only user: ' RO_PASSWORD

CREATE ROLE :RO_USERNAME WITH NOINHERIT LOGIN PASSWORD :'RO_PASSWORD';

GRANT SELECT ON ALL TABLES IN SCHEMA public TO :RO_USERNAME;