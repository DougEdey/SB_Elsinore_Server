package ca.strangebrew.recipe;

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
    private double minTemperature;
    private double maxTemperature;
    private String flocculation;
    private String bestFor;
    private int timesCultured;
    private int maxReuse;
    private boolean addToSecondary;

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

    public void setMinTemperature(double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public double getMinTemperature() {
        return minTemperature;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public double getMaxTemperature() {
        return maxTemperature;
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
}
