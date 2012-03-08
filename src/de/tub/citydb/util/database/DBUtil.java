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
package de.tub.citydb.util.database;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.citygml.CityGMLClass;
import org.postgis.Geometry;
import org.postgis.PGgeometry;

import de.tub.citydb.api.database.DatabaseSrs;
import de.tub.citydb.api.database.DatabaseSrsType;
import de.tub.citydb.api.gui.BoundingBox;
import de.tub.citydb.api.gui.BoundingBoxCorner;
import de.tub.citydb.config.project.general.FeatureClassMode;
import de.tub.citydb.database.DatabaseConnectionPool;
import de.tub.citydb.database.DatabaseMetaDataImpl;
import de.tub.citydb.util.Util;
import de.tub.citydb.util.database.IndexStatusInfo.IndexType;

public class DBUtil {
	private static final DatabaseConnectionPool dbConnectionPool = DatabaseConnectionPool.getInstance();
	private static volatile boolean cancelled = false;

	// use for interuptable operations
	private static CallableStatement callableStmt;
	private static Statement stmt;

	public static DatabaseMetaDataImpl getDatabaseInfo() throws SQLException {
		DatabaseMetaDataImpl metaData = new DatabaseMetaDataImpl();
		Connection conn = null;
		ResultSet rs = null;

		try {
			conn = dbConnectionPool.getConnectionWithTimeout();

			// get vendor specific meta data
			DatabaseMetaData dbMetaData = conn.getMetaData();			

			// get 3dcitydb specific meta data
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select * from geodb_pkg.util_db_metadata() as t");
			if (rs.next()) {
				DatabaseSrs srs = metaData.getReferenceSystem();
				srs.setSrid(rs.getInt("SRID"));
				srs.setGMLSrsName(rs.getString("GML_SRS_NAME"));
				srs.setDatabaseSrsName(rs.getString("COORD_REF_SYS_NAME"));
				srs.setType(DatabaseSrsType.fromValue(rs.getString("COORD_REF_SYS_KIND")));
				srs.setSupported(true);

//				metaData.setVersioning(Versioning.valueOf(rs.getString("VERSIONING")));

			} else
				throw new SQLException("Failed to retrieve metadata information from database.");

			metaData.setDatabaseProductName(dbMetaData.getDatabaseProductName());
			metaData.setDatabaseProductVersion(dbMetaData.getDatabaseProductVersion());
			metaData.setDatabaseMajorVersion(dbMetaData.getDatabaseMajorVersion());
			metaData.setDatabaseMinorVersion(dbMetaData.getDatabaseMinorVersion());

			return metaData;

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
			
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				stmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}
		}
	}

	public static void getSrsInfo(DatabaseSrs srs) throws SQLException {
		Connection conn = null;
		PreparedStatement psQuery = null;
		ResultSet rs = null;

		try {
			conn = dbConnectionPool.getConnection();

			psQuery = conn.prepareStatement("select split_part(srtext, '\"', 2) as coord_ref_sys_name, split_part(srtext, '[', 1) as coord_ref_sys_kind FROM spatial_ref_sys WHERE SRID = ? ");
			psQuery.setInt(1, srs.getSrid());
			rs = psQuery.executeQuery();
			if (rs.next()) {
				srs.setSupported(true);
				srs.setDatabaseSrsName(rs.getString(1));
				srs.setType(DatabaseSrsType.fromValue(rs.getString(2)));
			} else {
				DatabaseSrs tmp = DatabaseSrs.createDefaultSrs();
				srs.setDatabaseSrsName(tmp.getDatabaseSrsName());
				srs.setType(tmp.getType());
				srs.setSupported(false);
			}

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

			if (psQuery != null) {
				try {
					psQuery.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				psQuery = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				conn = null;
			}
		}
	}

//	public static String[] databaseReport(Workspace workspace) throws SQLException {
	public static String[] databaseReport() throws SQLException {
		String[] report = null;
		Connection conn = null;

		try {
			conn = dbConnectionPool.getConnection();
//			dbConnectionPool.gotoWorkspace(conn, workspace);		

			callableStmt = (CallableStatement)conn.prepareCall("{? = call geodb_pkg.stat_table_contents()}");
			callableStmt.registerOutParameter(1, Types.ARRAY);
			callableStmt.executeUpdate();

			Array result = callableStmt.getArray(1);

			if (!cancelled)
				report = (String[])result.getArray();

		} catch (SQLException sqlEx) {
			if (!cancelled)
				throw sqlEx;
		} finally {
			if (callableStmt != null) {
				try {
					callableStmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				callableStmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}

			cancelled = false;
		}

		return report;
	}

//	public static BoundingBox calcBoundingBox(Workspace workspace, FeatureClassMode featureClass) throws SQLException {
	public static BoundingBox calcBoundingBox(FeatureClassMode featureClass) throws SQLException {
		BoundingBox bbox = null;
		Connection conn = null;
		ResultSet rs = null;

		try {
			conn = dbConnectionPool.getConnection();
//			dbConnectionPool.gotoWorkspace(conn, workspace);	
			stmt = conn.createStatement();

			List<Integer> featureTypes = new ArrayList<Integer>();
			String query = "select ST_Extent(ST_Force_2d(envelope))::geometry from cityobject where envelope is not null";

			switch (featureClass) {
			case BUILDING:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.BUILDING));
				break;
			case CITYFURNITURE:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.CITY_FURNITURE));
				break;
			case CITYOBJECTGROUP:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.CITY_OBJECT_GROUP));
				break;
			case GENERICCITYOBJECT:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.GENERIC_CITY_OBJECT));
				break;
			case LANDUSE:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.LAND_USE));
				break;
			case RELIEFFEATURE:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.RELIEF_FEATURE));
				break;
			case TRANSPORTATION:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.TRANSPORTATION_COMPLEX));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.ROAD));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.RAILWAY));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.TRACK));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.SQUARE));
				break;
			case VEGETATION:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.PLANT_COVER));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.SOLITARY_VEGETATION_OBJECT));
				break;
			case WATERBODY:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.WATER_BODY));
				break;
			default:
				featureTypes.add(Util.cityObject2classId(CityGMLClass.BUILDING));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.CITY_FURNITURE));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.CITY_OBJECT_GROUP));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.GENERIC_CITY_OBJECT));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.LAND_USE));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.RELIEF_FEATURE));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.TRANSPORTATION_COMPLEX));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.ROAD));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.RAILWAY));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.TRACK));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.SQUARE));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.PLANT_COVER));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.SOLITARY_VEGETATION_OBJECT));
				featureTypes.add(Util.cityObject2classId(CityGMLClass.WATER_BODY));
			}

			if (!featureTypes.isEmpty())
				query += " and class_id in (" + Util.collection2string(featureTypes, ", ") + ") ";

			rs = stmt.executeQuery(query);
			BoundingBoxCorner lowerCorner = new BoundingBoxCorner(Double.MAX_VALUE);
			BoundingBoxCorner upperCorner = new BoundingBoxCorner(-Double.MAX_VALUE);

			if (rs.next()) {
				PGgeometry pgGeom = (PGgeometry)rs.getObject(1);		
				if (!rs.wasNull() && pgGeom != null) {
					Geometry geom = pgGeom.getGeometry();
					int dim = geom.getDimension();	
					if (dim == 2 || dim == 3) {
						double xmin, ymin, xmax, ymax;
						xmin = ymin = Double.MAX_VALUE;
						xmax = ymax = -Double.MAX_VALUE;
						
						xmin = (geom.getPoint(0).x);
						ymin = (geom.getPoint(0).y);
						xmax = (geom.getPoint(2).x);
						ymax = (geom.getPoint(2).y);

						lowerCorner.setX(xmin);
						lowerCorner.setY(ymin);
						upperCorner.setX(xmax);
						upperCorner.setY(ymax);	
					}		
				}
			}

			if (!cancelled)
				bbox = new BoundingBox(lowerCorner, upperCorner);

		} catch (SQLException sqlEx) {
			if (!cancelled)
				throw sqlEx;
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				stmt = null;
			}

			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				rs = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}

			cancelled = false;
		}

		return bbox;
	}

	private static IndexStatusInfo dropIndexes(IndexType type) throws SQLException {
		Connection conn = null;

		String call = type == IndexType.SPATIAL ? 
				"{? = call geodb_pkg.idx_switch_off_spatial_indexes()}" :
					"{? = call geodb_pkg.idx_switch_off_normal_indexes()}";
		// the hard way: drop the index
//		String call = type == IndexType.SPATIAL ? 
//				"{? = call geodb_pkg.idx_drop_spatial_indexes()}" :
//					"{? = call geodb_pkg.idx_drop_normal_indexes()}";

		try {
			conn = dbConnectionPool.getConnection();
			callableStmt = (CallableStatement)conn.prepareCall(call);
			callableStmt.registerOutParameter(1, Types.ARRAY);
			callableStmt.executeUpdate();

			Array result = callableStmt.getArray(1);
			return IndexStatusInfo.createFromDatabaseQuery((String[])result.getArray(), type);

		} catch (SQLException sqlEx) {
			throw sqlEx;
		} finally {
			if (callableStmt != null) {
				try {
					callableStmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				callableStmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}
		}
	}

	private static IndexStatusInfo createIndexes(IndexType type) throws SQLException {
		Connection conn = null;

		// switch index on (indIsValid = true)
		String call = type == IndexType.SPATIAL ?
				"{? = call geodb_pkg.idx_switch_on_spatial_indexes()}" :
					"{? = call geodb_pkg.idx_switch_on_normal_indexes()}";
		
		// create the index again
//		String call = type == IndexType.SPATIAL ? 
//				"{? = call geodb_pkg.idx_create_spatial_indexes()}" :
//					"{? = call geodb_pkg.idx_create_normal_indexes()}";

		try {
			conn = dbConnectionPool.getConnection();
			callableStmt = (CallableStatement)conn.prepareCall(call);
			callableStmt.registerOutParameter(1, Types.ARRAY);
			callableStmt.executeUpdate();

			Array result = callableStmt.getArray(1);
			return IndexStatusInfo.createFromDatabaseQuery((String[])result.getArray(), type);

		} catch (SQLException sqlEx) {
			throw sqlEx;
		} finally {
			if (callableStmt != null) {
				try {
					callableStmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				callableStmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}
		}
	}

	public static IndexStatusInfo getIndexStatus(IndexType type) throws SQLException {
		Connection conn = null;

		String call = type == IndexType.SPATIAL ? 
			"{? = call geodb_pkg.idx_status_spatial_indexes()}" :
				"{? = call geodb_pkg.idx_status_normal_indexes()}";

		try {
			conn = dbConnectionPool.getConnection();
			callableStmt = (CallableStatement)conn.prepareCall(call);
			callableStmt.registerOutParameter(1, Types.ARRAY);
			callableStmt.executeUpdate();

			Array result = callableStmt.getArray(1);
			return IndexStatusInfo.createFromDatabaseQuery((String[])result.getArray(), type);
			
		} catch (SQLException sqlEx) {
			throw sqlEx;
		} finally {
			if (callableStmt != null) {
				try {
					callableStmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				callableStmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}
		}
	}

	public static boolean isIndexed(String tableName, String columnName) throws SQLException {
		Connection conn = null;
		boolean isIndexed = false;

		try {
			conn = dbConnectionPool.getConnection();
			callableStmt = (CallableStatement)conn.prepareCall("{? = call geodb_pkg.idx_index_status(?, ?)}");
			callableStmt.setString(2, tableName);
			callableStmt.setString(3, columnName);
			callableStmt.registerOutParameter(1, Types.VARCHAR);
			callableStmt.executeUpdate();

			isIndexed = callableStmt.getString(1).equals("VALID");

		} catch (SQLException sqlEx) {
			throw sqlEx;
		} finally {
			if (callableStmt != null) {
				try {
					callableStmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				callableStmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}
		}

		return isIndexed;
	}

	public static IndexStatusInfo dropSpatialIndexes() throws SQLException {
		return dropIndexes(IndexType.SPATIAL);
	}

	public static IndexStatusInfo dropNormalIndexes() throws SQLException {
		return dropIndexes(IndexType.NORMAL);
	}

	public static IndexStatusInfo createSpatialIndexes() throws SQLException {
		return createIndexes(IndexType.SPATIAL);
	}

	public static IndexStatusInfo createNormalIndexes() throws SQLException {
		return createIndexes(IndexType.NORMAL);
	}

	public static IndexStatusInfo getStatusSpatialIndexes() throws SQLException {
		return getIndexStatus(IndexType.SPATIAL);
	}

	public static IndexStatusInfo getStatusNormalIndexes() throws SQLException {
		return getIndexStatus(IndexType.NORMAL);
	}

	public static String errorMessage(String errorCode) throws SQLException {
		String errorMessage = null;
		Connection conn = null;

		try {
			conn = dbConnectionPool.getConnection();
			callableStmt = (CallableStatement)conn.prepareCall("{? = call geodb_pkg.util_error_msg(?)}");
			callableStmt.setString(2, errorCode);
			callableStmt.registerOutParameter(1, Types.VARCHAR);
			callableStmt.executeUpdate();

			errorMessage = callableStmt.getString(1);

		} catch (SQLException sqlEx) {
			throw sqlEx;
		} finally {
			if (callableStmt != null) {
				try {
					callableStmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				callableStmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}
			}
		}

		return errorMessage;
	}

	public static void cancelOperation() {	
		cancelled = true;

		try {
			if (callableStmt != null)
				callableStmt.cancel();

			if (stmt != null)
				stmt.cancel();
		} catch (SQLException sqlEx) {
			//
		}
	}

	public static BoundingBox transformBBox(BoundingBox bbox, DatabaseSrs sourceSrs, DatabaseSrs targetSrs) throws SQLException {
		BoundingBox result = new BoundingBox(bbox);
		PreparedStatement psQuery = null;
		ResultSet rs = null;
		Connection conn = null;

		try {
			int sourceSrid = get2DSrid(sourceSrs);
			int targetSrid = get2DSrid(targetSrs);
			
			conn = dbConnectionPool.getConnection();
			psQuery = conn.prepareStatement("select ST_TRANSFORM(select ST_GeomFromText('POLYGON((? ?,? ?,? ?,? ?,? ?))'," + sourceSrid + ")," + targetSrid + ")");

			psQuery.setDouble(1, bbox.getLowerLeftCorner().getX());
			psQuery.setDouble(2, bbox.getLowerLeftCorner().getY());
			
			psQuery.setDouble(3, bbox.getUpperRightCorner().getX());
			psQuery.setDouble(4, bbox.getLowerLeftCorner().getY());
						
			psQuery.setDouble(5, bbox.getUpperRightCorner().getX());
			psQuery.setDouble(6, bbox.getUpperRightCorner().getY());
	
			psQuery.setDouble(7, bbox.getLowerLeftCorner().getX());
			psQuery.setDouble(8, bbox.getUpperRightCorner().getY());
			
			psQuery.setDouble(9, bbox.getLowerLeftCorner().getX());
			psQuery.setDouble(10, bbox.getLowerLeftCorner().getY());
			
			rs = psQuery.executeQuery();
			if (rs.next()) {
				PGgeometry pgGeom = (PGgeometry)rs.getObject(1);
				if (!rs.wasNull() && pgGeom != null) {
					Geometry geom = pgGeom.getGeometry();
					result.getLowerLeftCorner().setX(geom.getPoint(0).x);
					result.getLowerLeftCorner().setY(geom.getPoint(0).y);
					result.getUpperRightCorner().setX(geom.getPoint(2).x);
					result.getUpperRightCorner().setY(geom.getPoint(2).y);
				}
			}
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

			if (psQuery != null) {
				try {
					psQuery.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				psQuery = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				conn = null;
			}
		}

		return result;
	}

	public static int get2DSrid(DatabaseSrs srs) throws SQLException {
//		if (!srs.is3D())
			return srs.getSrid();

//		Connection conn = null;
//		PreparedStatement psQuery = null;
//		ResultSet rs = null;
//
//		try {
//			conn = dbConnectionPool.getConnection();
//			psQuery = conn.prepareStatement(srs.getType() == DatabaseSrsType.GEOGCS ? 
//					"select min(crs2d.srid) from spatial_ref_sys crs3d, spatial_ref_sys crs2d where crs3d.srid = "
//					+ srs.getSrid() + " and crs2d.srtext LIKE '%GEOGCS%'" : "");
//					
////					"select min(crs2d.srid) from sdo_coord_ref_sys crs3d, sdo_coord_ref_sys crs2d where crs3d.srid = "
////					+ srs.getSrid() + " and crs2d.coord_ref_sys_kind = 'GEOGRAPHIC2D' and crs3d.datum_id = crs2d.datum_id" :
////						"select cmpd_horiz_srid from sdo_coord_ref_sys where srid = " + srs.getSrid());
//
//			rs = psQuery.executeQuery();
//			if (rs.next()) 
//				return rs.getInt(1);
//			else
//				throw new SQLException("Failed to discover 2D equivalent for the 3D SRID " + srs.getSrid());
//			
//		} catch (SQLException sqlEx) {
//			throw sqlEx;
//		} finally {
//			if (rs != null) {
//				try {
//					rs.close();
//				} catch (SQLException sqlEx) {
//					throw sqlEx;
//				}
//
//				rs = null;
//			}
//
//			if (psQuery != null) {
//				try {
//					psQuery.close();
//				} catch (SQLException sqlEx) {
//					throw sqlEx;
//				}
//
//				psQuery = null;
//			}
//
//			if (conn != null) {
//				try {
//					conn.close();
//				} catch (SQLException sqlEx) {
//					throw sqlEx;
//				}
//
//				conn = null;
//			}
//		}
	}

	public static List<String> getAppearanceThemeList() throws SQLException {
		Connection conn = null;
		PreparedStatement psQuery = null;
		ResultSet rs = null;
		ArrayList<String> appearanceThemes = new ArrayList<String>();

		try {
			/*boolean workspaceExists = dbConnectionPool.existsWorkspace(workspace);

			String name = "'" + workspace.getName().trim() + "'";
			String timestamp = workspace.getTimestamp().trim();
			if (timestamp.trim().length() > 0)
				name += " at timestamp " + timestamp;

			if (!workspaceExists) {
				Logger.getInstance().error("Database workspace " + name + " is not available.");
			} */

			conn = dbConnectionPool.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select distinct theme from appearance order by theme");
			while (rs.next()) {
				appearanceThemes.add(rs.getString(1));
			}

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

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				stmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				conn = null;
			}
		}

		return appearanceThemes;
	}
	
	public static int getNumGlobalAppearances() throws SQLException {
		Connection conn = null;
		ResultSet rs = null;

		try {
			conn = dbConnectionPool.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select count(id) from appearance where cityobject_id is null");

			if (rs.next()) 
				return rs.getInt(1);
			else
				throw new SQLException("Failed to discover number of global appearances.");
			
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

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				stmt = null;
			}

			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException sqlEx) {
					throw sqlEx;
				}

				conn = null;
			}
		}
	}
}
