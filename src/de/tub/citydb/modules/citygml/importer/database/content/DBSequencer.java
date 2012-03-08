/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2011
 * Institute for Geodesy and Geoinformation Science
 * Technische Universitaet Berlin, Germany
 * http://www.gis.tu-berlin.de/
 *
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see 
 * <http://www.gnu.org/licenses/>.
 * 
 * The development of the 3D City Database Importer/Exporter has 
 * been financially supported by the following cooperation partners:
 * 
 * Business Location Center, Berlin <http://www.businesslocationcenter.de/>
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * Berlin Senate of Business, Technology and Women <http://www.berlin.de/sen/wtf/>
 */
package de.tub.citydb.modules.citygml.importer.database.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class DBSequencer {
	private Connection conn;
	private HashMap<DBSequencerEnum, PreparedStatement> psIdMap;

	public DBSequencer(Connection conn) throws SQLException {
		this.conn = conn;
		psIdMap = new HashMap<DBSequencerEnum, PreparedStatement>();
	}

	public long getDBId(DBSequencerEnum sequence) throws SQLException {
		if (sequence == null)
			return 0;

		PreparedStatement pstsmt = psIdMap.get(sequence);
		if (pstsmt == null) {
			pstsmt = conn.prepareStatement("select nextval('" + sequence.toString() + "')");
//			pstsmt = conn.prepareStatement("select " + sequence.toString() + ".nextval from dual");
			psIdMap.put(sequence, pstsmt);
		}

		ResultSet rs = null;
		long id = 0;

		try {
			rs = pstsmt.executeQuery();

			if (rs.next())
				id = rs.getLong(1);

		} catch (SQLException sqlEx) {
			throw sqlEx;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				rs = null;
			}
		}

		return id;
	}
	
	public void close() throws SQLException {
		for (PreparedStatement stmt : psIdMap.values())
			stmt.close();
	}
}
