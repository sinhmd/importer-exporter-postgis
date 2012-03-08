-- CITYMODEL.sql
--
-- Authors:     Prof. Dr. Thomas H. Kolbe <kolbe@igg.tu-berlin.de>
--              Gerhard K�nig <gerhard.koenig@tu-berlin.de>
--              Claus Nagel <nagel@igg.tu-berlin.de>
--              Alexandra Stadler <stadler@igg.tu-berlin.de>
--
-- Conversion:  Laure Fraysse <Laure.fraysse@etumel.univmed.fr>
--				Felix Kunde <felix-kunde@gmx.de>
--
-- Copyright:   (c) 2007-2011  Institute for Geodesy and Geoinformation Science,
--                             Technische Universit�t Berlin, Germany
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
-- Version | Date       | Description     | Author | Conversion
-- 2.0.0     2011-12-09   PostGIS version    TKol	  LFra	
--                                           GKoe     FKun
--                                           CNag
--                                           ASta
--
CREATE TABLE CITYMODEL (
	ID 									SERIAL NOT NULL,
	GMLID 								VARCHAR(256),
	GMLID_CODESPACE 					VARCHAR(1000),
	NAME 								VARCHAR(1000),
	NAME_CODESPACE 						VARCHAR(4000),
	DESCRIPTION 						VARCHAR(4000)
)
;

SELECT public.AddGeometryColumn('citymodel', 'envelope', 3068, 'POLYGON', 3);

ALTER TABLE CITYMODEL 
	ADD COLUMN CREATION_DATE 			DATE,
	ADD COLUMN TERMINATION_DATE 		DATE,
	ADD COLUMN LAST_MODIFICATION_DATE 	DATE,
	ADD COLUMN UPDATING_PERSON 			VARCHAR(256),
	ADD COLUMN REASON_FOR_UPDATE 		VARCHAR(4000),
	ADD COLUMN LINEAGE 					VARCHAR(256)
;

ALTER TABLE CITYMODEL
ADD CONSTRAINT CITYMODEL_PK PRIMARY KEY
(
ID
)
;