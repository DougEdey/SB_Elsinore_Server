package ca.strangebrew.recipe;

import com.sb.elsinore.BrewServer;

/**
 * $Id: Yeast.java,v 1.1 2006/04/07 13:59:14 andrew_avis Exp $
 * @author aavis
 * Created on Oct 21, 2004
 *
 */
public class Yeast extends Ingredient {
	// I'm not sure what else needs to be added to yeast,
	// but if we need to, we can add it here
	private double attenuation = 80.0;
	private String form = "dry";
    private String laboratory;
    private String productId;
    private double minTemperature = 0.0;
    private double maxTemperature = 0.0;
    private String flocculation = null;
    private String bestFor = null;
    private int timesCultured = -1;
    private int maxReuse = -1;
    private boolean addToSecondary;
    private String minTemperatureUnit = null;
    private String maxTemperatureUnit = null;
    private String cultureDate;

    // should handle defaults here:
	public Yeast(){
		// setName("A yeast");
	}
	
	public void setAttenuation(double inAttenuation) {
	    this.attenuation = inAttenuation;
	}
	
	public double getAttenuation() {
	    return this.attenuation;
	}

	public void setForm(String inForm) {
	    this.form = inForm;
	}
	
	public String getForm() {
	    return this.form;
	}

    public void setLaboratory(String laboratory) {
        this.laboratory = laboratory;
    }

    public String getLaboratory() {
        return laboratory;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }

    public void setMinTemperature(double newMinTemperature) {
        this.minTemperature = newMinTemperature;
    }

    public void setMinTemperature(String newMinTemperature) {
        try {
            String[] exploded = newMinTemperature.split(" ");
            if (exploded.length == 2) {
                this.minTemperature = Double.parseDouble(exploded[0].trim());
                this.minTemperatureUnit = exploded[1];
            }
        } catch (NumberFormatException nfe) {
            BrewServer.LOG.warning("Couldn't parse Min Temperature: " + newMinTemperature);
        }
    }

    /**
     * Returns the min temperature of this Yeast in C.
     * @return The minimum fermentation temperature of this yeast
     */
    public double getMinTemperature() {
        if (this.minTemperatureUnit != null && this.minTemperatureUnit.equals("F")) {
            return BrewCalcs.fToC(minTemperature);
        }
        return minTemperature;
    }

    public String getMinTemperatureString() {
        return this.minTemperature + " " + this.minTemperatureUnit;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public void setMaxTemperature(String newMaxTemperature) {
        try {
            String[] exploded = newMaxTemperature.split(" ");
            if (exploded.length == 2) {
                this.maxTemperature = Double.parseDouble(exploded[0].trim());
                this.maxTemperatureUnit = exploded[1];
            }
        } catch (NumberFormatException nfe) {
            BrewServer.LOG.warning("Couldn't parse Min Temperature: " + newMaxTemperature);
        }
    }

    /**
     * Returns the max temperature of this Yeast in C.
     * @return The maximum fermentation temperature of this yeast
     */
    public double getMaxTemperature() {
        if (this.maxTemperatureUnit != null && this.maxTemperatureUnit.equals("F")) {
            return BrewCalcs.fToC(minTemperature);
        }
        return maxTemperature;
    }

    public String getMaxTemperatureString() {
        return this.maxTemperature + " " + this.maxTemperatureUnit;
    }

    public void setFlocculation(String flocculation) {
        this.flocculation = flocculation;
    }

    public String getFlocculation() {
        return flocculation;
    }

    public void setBestFor(String bestFor) {
        this.bestFor = bestFor;
    }

    public String getBestFor() {
        return bestFor;
    }

    public void setTimesCultured(int timesCultured) {
        this.timesCultured = timesCultured;
    }

    public int getTimesCultured() {
        return timesCultured;
    }

    public void setMaxReuse(int maxReuse) {
        this.maxReuse = maxReuse;
    }

    public int getMaxReuse() {
        return maxReuse;
    }

    public void addToSecondary(boolean addToSecondary) {
        this.addToSecondary = addToSecondary;
    }

    public boolean isAddToSecondary() {
        return addToSecondary;
    }

    public void setCultureDate(String cultureDate) {
        this.cultureDate = cultureDate;
    }

    public String getCultureDate() {
        return cultureDate;
    }
}
