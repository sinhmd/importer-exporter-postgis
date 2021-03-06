/*
 * This file is part of the 3D City Database Importer/Exporter.
 * Copyright (c) 2007 - 2013
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
package de.tub.citydb.modules.citygml.exporter.database.xlink;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

//import org.postgresql.largeobject.LargeObject;
//import org.postgresql.largeobject.LargeObjectManager;

import de.tub.citydb.config.Config;
import de.tub.citydb.log.Logger;
import de.tub.citydb.modules.citygml.common.database.xlink.DBXlinkTextureFile;
import de.tub.citydb.modules.common.event.CounterEvent;
import de.tub.citydb.modules.common.event.CounterType;
import de.tub.citydb.util.Util;

public class DBXlinkExporterTextureImage implements DBXlinkExporter {
	private final Logger LOG = Logger.getInstance();

	private final DBXlinkExporterManager xlinkExporterManager;
	private final Config config;
	private final Connection connection;

	private PreparedStatement psTextureImage;
	
	private String localPath;
	private String texturePath;
	private boolean texturePathIsLocal;
	private boolean overwriteTextureImage;
	private CounterEvent counter;

	public DBXlinkExporterTextureImage(Connection connection, Config config, DBXlinkExporterManager xlinkExporterManager) throws SQLException {
		this.xlinkExporterManager = xlinkExporterManager;
		this.config = config;
		this.connection = connection;

		init();
	}

	private void init() throws SQLException {
		localPath = config.getInternal().getExportPath();
		texturePathIsLocal = config.getProject().getExporter().getAppearances().isTexturePathRealtive();
		texturePath = config.getInternal().getExportTextureFilePath();
		overwriteTextureImage = config.getProject().getExporter().getAppearances().isSetOverwriteTextureFiles();
		counter = new CounterEvent(CounterType.TEXTURE_IMAGE, 1, this);

		psTextureImage = connection.prepareStatement("select TEX_IMAGE from SURFACE_DATA where ID=?");
	}

	public boolean export(DBXlinkTextureFile xlink) throws SQLException {
		String fileName = xlink.getFileURI();
		boolean isRemote = false;

		if (fileName == null || fileName.length() == 0) {
			LOG.error("Database error while exporting a texture file: Attribute TEX_IMAGE_URI is empty.");
			return false;
		}

		// check whether we deal with a remote image uri
		if (Util.isRemoteXlink(fileName)) {
			URL url = null;
			isRemote = true;

			try {
				url = new URL(fileName);
			} catch (MalformedURLException e) {
				LOG.error("Error while exporting a texture file: " + fileName + " could not be interpreted.");
				return false;
			}

			if (url != null) {
				File file = new File(url.getFile());
				fileName = file.getName();
			}
		}

		// start export of texture to file
		// we do not overwrite an already existing file. so no need to
		// query the database in that case.
		String fileURI;
		if (texturePathIsLocal)
			fileURI = localPath + File.separator + texturePath + File.separator + fileName;
		else
			fileURI = texturePath + File.separator + fileName;

		File file = new File(fileURI);
		if (!overwriteTextureImage && file.exists())
			return false;

		// try and read texture image attribute from surface_data table
		psTextureImage.setLong(1, xlink.getId());
		ResultSet rs = (ResultSet)psTextureImage.executeQuery();
		if (!rs.next()) {
			if (!isRemote) {
				// we could not read from database. if we deal with a remote
				// image uri, we do not really care. but if the texture image should
				// be provided by us, then this is serious...
				LOG.error("Error while exporting a texture file: " + fileName + " does not exist in database.");
			}

			rs.close();
			return false;
		}

		xlinkExporterManager.propagateEvent(counter);

		// read bytea data type
		byte[] imgBytes = rs.getBytes(1);
		try {
			FileOutputStream fos = new FileOutputStream(fileURI);
			fos.write(imgBytes);
			fos.close();
		} catch (FileNotFoundException fnfEx) {
			LOG.error("File not found " + fileName + ": " + fnfEx.getMessage());
		} catch (IOException ioEx) {
        	LOG.error("Failed to write texture file " + fileName + ": " + ioEx.getMessage());
        	return false;
		}           

/*		// read bytea data type, alternative way 
		
		InputStream imageStream = rs.getBinaryStream(1);
			
		if (imageStream == null) {
           	LOG.error("Database error while reading texture file: " + fileName);
           	return false;
        }
			
		try {
			byte[] imgBuffer = new byte[2048];
			FileOutputStream fos = new FileOutputStream(fileURI);
			int l;
			while ((l = imageStream.read(imgBuffer)) > 0) {
                fos.write(imgBuffer, 0, l);
            }
			fos.close();			
		} catch (FileNotFoundException fnfEx) {
			LOG.error("File not found " + fileName + ": " + fnfEx.getMessage());
		} catch (IOException ioEx) {
           	LOG.error("Failed to write texture file " + fileName + ": " + ioEx.getMessage());
           	return false;
		}           
*/
/*		// read large object (OID) data type from database
		// export of large objects requires length function for OID in PostgreSQL
		
		// All LargeObject API calls must be within a transaction block
		connection.setAutoCommit(false);

		// Get the Large Object Manager to perform operations with
		LargeObjectManager lobj = ((org.postgresql.PGConnection)connection).getLargeObjectAPI();

		// Open the large object for reading
		long oid = rs.getLong(1);
		if (oid == 0) {
			LOG.error("Database error while reading library object: " + fileName);
			return false;
		}
		LargeObject obj = lobj.open(oid, LargeObjectManager.READ);

		// Read the data
		byte buf[] = new byte[obj.size()];
		obj.read(buf, 0, obj.size());
		
		// Write the data
		try {
			FileOutputStream fos = new FileOutputStream(fileURI);
			fos.write(buf, 0, obj.size());
			obj.close();
			fos.close();
		} catch (FileNotFoundException fnfEx) {
			LOG.error("File not found " + fileName + ": " + fnfEx.getMessage());
		} catch (IOException ioEx) {
           	LOG.error("Failed to write texture file " + fileName + ": " + ioEx.getMessage());
           	return false;
		}   	
		
		connection.commit();
*/
		rs.close();
		return true;
	}

	@Override
	public void close() throws SQLException {
		psTextureImage.close();
	}

	@Override
	public DBXlinkExporterEnum getDBXlinkExporterType() {
		return DBXlinkExporterEnum.TEXTURE_IMAGE;
	}

}
