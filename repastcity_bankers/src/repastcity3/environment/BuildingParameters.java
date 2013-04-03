package repastcity3.environment;

import repastcity3.exceptions.NoDensityException;


public interface BuildingParameters {
	
	int getDensity() throws NoDensityException;

	void setDensity(String id);

}
