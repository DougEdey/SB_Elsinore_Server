
package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;

/**
 * Created on Oct 4, 2004
 * $Id: Fermentable.java,v 1.11 2012/06/02 19:40:58 dougedey Exp $
 * @author aavis
 *
 * This is the base malt class.  It doesn't do much, except hold data
 * and get/set data
 */

public class Fermentable extends Ingredient  {
	
	// base data
	private double pppg;
	private double lov;
	private boolean mashed;
	private boolean steeped;
	private boolean fermentable = true;
	private double percent;
	private boolean prime;
	
	final static private double basePppg = 1.047;
    private boolean addAfterBoil = false;
    private String origin;
    private String supplier;
    private double coarseFineDiff;
    private double moisture;
    private double diastaticPower;
    private double ibuGalPerLb;
    private double maxInBatch;
    private double protein;

    // constructors:
	public Fermentable(String n, double p, double l, double a, String u) {
		setName(n);
		pppg = p;
		lov = l;
		setAmount(a);
		setUnits(u);
		mashed = true;
		prime = false;
	}
	
	public Fermentable(Fermentable f) {
		setName(f.getName());
		pppg = f.getPppg();
		lov = f.getLov();
		setAmount(f.getAmountAs(f.getUnits()));
		setUnits(f.getUnits());
		mashed = f.getMashed();
		prime = f.getPrime();
		percent = f.getPercent();
	}

	public Fermentable(String u) {
		setName("");
		pppg = 1.000;
		setUnits(u);
	}

	public Fermentable(){
		// default constructor
		setName("");
		pppg = 0;
		lov = 0;
		setAmount(1.0);
		setUnits("");
		mashed = true;
		prime = false;
	}

	static public double getBasePppg() {
		return basePppg;
	}

	// getter methods:
	public double getLov(){ return lov; }
	public boolean getMashed(){ return mashed; }
	public double getPercent() { return percent; }
	public double getPppg(){ return pppg; }
	public boolean getSteep(){return steeped; }
	public boolean getPrime() { return prime; }
	public boolean ferments() { return fermentable; } 

	// setter methods:	
	public void setLov(double l){
		
		lov = l; 
	}
	public void setMashed(boolean m){ mashed = m; }
	public void setPercent(double p){ percent = p; }
	public void setPppg(double p){ pppg = p; }
	public void setSteep(boolean s){ steeped = s; }
	public void setPrime(boolean b) { prime = b; }
	public void ferments(boolean f) { fermentable = f; }
	
	// Need to add the spaces and type attributes to make this
	// backwards-compatible with SB1.8:
	public String toXML(){
	    StringBuilder sb = new StringBuilder();
	    sb.append( "    <ITEM>\n" );
	    sb.append("      <MALT>").append(getName()).append("</MALT>\n");
	    sb.append("      <AMOUNT>").append(getAmountAs(getUnits())).append("</AMOUNT>\n");
	    sb.append("      <PERCENT>").append(SBStringUtils.format(percent, 1)).append("</PERCENT>\n");
	    sb.append("      <UNITS>").append(getUnitsAbrv()).append("</UNITS>\n");
	    sb.append("      <POINTS>").append(pppg).append("</POINTS>\n");
	    sb.append("      <LOV>").append(lov).append("</LOV>\n");
	    sb.append("      <MASHED>").append(mashed).append("</MASHED>\n");
	    sb.append("      <STEEPED>").append(steeped).append("</STEEPED>\n");
	    sb.append("      <FERMENTS>").append(fermentable).append("</FERMENTS>\n");
	    sb.append("      <COSTLB>").append(getCostPerU()).append("</COSTLB>\n");
	    sb.append("      <DESCRIPTION>").append(SBStringUtils.subEntities(getDescription())).append("</DESCRIPTION>\n");
	    sb.append( "    </ITEM>\n" );
	    return sb.toString();
	}

    public void addAfterBoil(boolean addAfterBoil) {
        this.addAfterBoil = addAfterBoil;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setCoarseFineDiff(double coarseFineDiff) {
        this.coarseFineDiff = coarseFineDiff;
    }

    public double getCoarseFineDiff() {
        return coarseFineDiff;
    }

    public void setMoisture(double moisture) {
        this.moisture = moisture;
    }

    public double getMoisture() {
        return moisture;
    }

    public void setDiastaticPower(double diastaticPower) {
        this.diastaticPower = diastaticPower;
    }

    public double getDiastaticPower() {
        return diastaticPower;
    }

    public void setIbuGalPerLb(double ibuGalPerLb) {
        this.ibuGalPerLb = ibuGalPerLb;
    }

    public double getIbuGalPerLb() {
        return ibuGalPerLb;
    }

    public void setMaxInBatch(double maxInBatch) {
        this.maxInBatch = maxInBatch;
    }

    public double getMaxInBatch() {
        return maxInBatch;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getProtein() {
        return protein;
    }

    public boolean addAfterBoil() {
        return addAfterBoil;
    }
}
