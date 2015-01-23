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
}
