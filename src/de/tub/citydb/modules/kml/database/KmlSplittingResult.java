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
package de.tub.citydb.modules.kml.database;

import org.citygml4j.model.citygml.CityGMLClass;
import de.tub.citydb.config.project.kmlExporter.DisplayForm;

public class KmlSplittingResult {

	private long id;
	private String gmlId;
	private DisplayForm displayForm;
	private CityGMLClass cityObjectType;

	public KmlSplittingResult(long id, String gmlId, CityGMLClass cityObjectType, DisplayForm displayForm) {
		this.setId(id);
		this.setGmlId(gmlId);
		this.setCityObjectType(cityObjectType);
		this.setDisplayForm(displayForm);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setGmlId(String gmlId) {
		this.gmlId = gmlId;
	}

	public String getGmlId() {
		return gmlId;
	}

	public void setDisplayForm(DisplayForm displayForm) {
		this.displayForm = displayForm;
	}

	public DisplayForm getDisplayForm() {
		return displayForm;
	}

	public CityGMLClass getCityObjectType() {
		return cityObjectType;
	}

	public void setCityObjectType(CityGMLClass cityObjectType) {
		this.cityObjectType = cityObjectType;
	}
	
	public boolean isBuilding() {
		return getCityObjectType().compareTo(CityGMLClass.BUILDING) == 0;
	}
		
	public boolean isCityObjectGroup() {
		return getCityObjectType().compareTo(CityGMLClass.CITY_OBJECT_GROUP) == 0;
	}

	public boolean isVegetation() {
		return (getCityObjectType().compareTo(CityGMLClass.SOLITARY_VEGETATION_OBJECT) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.PLANT_COVER) == 0);
	}

	public boolean isGenericCityObject() {
		return getCityObjectType().compareTo(CityGMLClass.GENERIC_CITY_OBJECT) == 0;
	}

	public boolean isCityFurniture() {
		return getCityObjectType().compareTo(CityGMLClass.CITY_FURNITURE) == 0;
	}

	public boolean isWaterBody() {
		return (getCityObjectType().compareTo(CityGMLClass.WATER_BODY) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.WATER_CLOSURE_SURFACE) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.WATER_GROUND_SURFACE) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.WATER_SURFACE) == 0);
	}

	public boolean isLandUse() {
		return getCityObjectType().compareTo(CityGMLClass.LAND_USE) == 0;
	}

	public boolean isTransportation() {
		return (isTrafficArea() || isTransportationComplex());
	}

	public boolean isTrafficArea() {
		return (getCityObjectType().compareTo(CityGMLClass.TRAFFIC_AREA) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.AUXILIARY_TRAFFIC_AREA) == 0);
	}

	public boolean isTransportationComplex() {
		return (getCityObjectType().compareTo(CityGMLClass.TRANSPORTATION_COMPLEX) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.TRACK) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.RAILWAY) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.ROAD) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.SQUARE) == 0);
	}

	public boolean isRelief() {
		return (getCityObjectType().compareTo(CityGMLClass.RELIEF_FEATURE) == 0 /* ||
				getCityObjectType().compareTo(CityGMLClass.RASTER_RELIEF) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.MASSPOINT_RELIEF) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.BREAKLINE_RELIEF) == 0 ||
				getCityObjectType().compareTo(CityGMLClass.TIN_RELIEF) == 0 */);
	}
}
