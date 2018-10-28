package ca.strangebrew.recipe;

/**
 * $Id: Yeast.java,v 1.1 2006/04/07 13:59:14 andrew_avis Exp $
 *
 * @author aavis
 * Created on Oct 21, 2004
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
    public Yeast() {
        // setName("A yeast");
    }

    public double getAttenuation() {
        return this.attenuation;
    }

    public void setAttenuation(double inAttenuation) {
        this.attenuation = inAttenuation;
    }

    public String getForm() {
        return this.form;
    }

    public void setForm(String inForm) {
        this.form = inForm;
    }

    public String getLaboratory() {
        return this.laboratory;
    }

    public void setLaboratory(String laboratory) {
        this.laboratory = laboratory;
    }

    public String getProductId() {
        return this.productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     * Returns the min temperature of this Yeast in C.
     *
     * @return The minimum fermentation temperature of this yeast
     */
    public double getMinTemperature() {
        if (this.minTemperatureUnit != null && this.minTemperatureUnit.equals("F")) {
            return BrewCalcs.fToC(this.minTemperature);
        }
        return this.minTemperature;
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
            this.logger.warn("Couldn't parse Min TemperatureModel: {}", newMinTemperature);
        }
    }

    public String getMinTemperatureString() {
        return this.minTemperature + " " + this.minTemperatureUnit;
    }

    /**
     * Returns the max temperature of this Yeast in C.
     *
     * @return The maximum fermentation temperature of this yeast
     */
    public double getMaxTemperature() {
        if (this.maxTemperatureUnit != null && this.maxTemperatureUnit.equals("F")) {
            return BrewCalcs.fToC(this.minTemperature);
        }
        return this.maxTemperature;
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
            this.logger.warn("Couldn't parse Min TemperatureModel: {}", newMaxTemperature);
        }
    }

    public String getMaxTemperatureString() {
        return this.maxTemperature + " " + this.maxTemperatureUnit;
    }

    public String getFlocculation() {
        return this.flocculation;
    }

    public void setFlocculation(String flocculation) {
        this.flocculation = flocculation;
    }

    public String getBestFor() {
        return this.bestFor;
    }

    public void setBestFor(String bestFor) {
        this.bestFor = bestFor;
    }

    public int getTimesCultured() {
        return this.timesCultured;
    }

    public void setTimesCultured(int timesCultured) {
        this.timesCultured = timesCultured;
    }

    public int getMaxReuse() {
        return this.maxReuse;
    }

    public void setMaxReuse(int maxReuse) {
        this.maxReuse = maxReuse;
    }

    public void addToSecondary(boolean addToSecondary) {
        this.addToSecondary = addToSecondary;
    }

    public boolean isAddToSecondary() {
        return this.addToSecondary;
    }

    public String getCultureDate() {
        return this.cultureDate;
    }

    public void setCultureDate(String cultureDate) {
        this.cultureDate = cultureDate;
    }
}
