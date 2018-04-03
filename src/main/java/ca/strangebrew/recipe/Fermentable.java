
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
        this.pppg = p;
        this.lov = l;
		setAmount(a);
		setUnits(u);
        this.mashed = true;
        this.prime = false;
	}
	
	public Fermentable(Fermentable f) {
		setName(f.getName());
        this.pppg = f.getPppg();
        this.lov = f.getLov();
		setAmount(f.getAmountAs(f.getUnits()));
		setUnits(f.getUnits());
        this.mashed = f.getMashed();
        this.prime = f.getPrime();
        this.percent = f.getPercent();
	}

	public Fermentable(String u) {
		setName("");
        this.pppg = 1.000;
		setUnits(u);
	}

	public Fermentable(){
		// default constructor
		setName("");
        this.pppg = 0;
        this.lov = 0;
		setAmount(1.0);
		setUnits("");
        this.mashed = true;
        this.prime = false;
	}

	static public double getBasePppg() {
		return basePppg;
	}

	// getter methods:
	public double getLov(){ return this.lov; }
	public boolean getMashed(){ return this.mashed; }
	public double getPercent() { return this.percent; }
	public double getPppg(){ return this.pppg; }
	public boolean getSteep(){return this.steeped; }
	public boolean getPrime() { return this.prime; }
	public boolean ferments() { return this.fermentable; }

	// setter methods:	
	public void setLov(double l){

        this.lov = l;
	}
	public void setMashed(boolean m){
        this.mashed = m; }
	public void setPercent(double p){
        this.percent = p; }
	public void setPppg(double p){
        this.pppg = p; }
	public void setSteep(boolean s){
        this.steeped = s; }
	public void setPrime(boolean b) {
        this.prime = b; }
	public void ferments(boolean f) {
        this.fermentable = f; }
	
	// Need to add the spaces and type attributes to make this
	// backwards-compatible with SB1.8:
	public String toXML(){
	    StringBuilder sb = new StringBuilder();
	    sb.append( "    <ITEM>\n" );
	    sb.append("      <MALT>").append(getName()).append("</MALT>\n");
	    sb.append("      <AMOUNT>").append(getAmountAs(getUnits())).append("</AMOUNT>\n");
	    sb.append("      <PERCENT>").append(SBStringUtils.format(this.percent, 1)).append("</PERCENT>\n");
	    sb.append("      <UNITS>").append(getUnitsAbrv()).append("</UNITS>\n");
	    sb.append("      <POINTS>").append(this.pppg).append("</POINTS>\n");
	    sb.append("      <LOV>").append(this.lov).append("</LOV>\n");
	    sb.append("      <MASHED>").append(this.mashed).append("</MASHED>\n");
	    sb.append("      <STEEPED>").append(this.steeped).append("</STEEPED>\n");
	    sb.append("      <FERMENTS>").append(this.fermentable).append("</FERMENTS>\n");
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
        return this.origin;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getSupplier() {
        return this.supplier;
    }

    public void setCoarseFineDiff(double coarseFineDiff) {
        this.coarseFineDiff = coarseFineDiff;
    }

    public double getCoarseFineDiff() {
        return this.coarseFineDiff;
    }

    public void setMoisture(double moisture) {
        this.moisture = moisture;
    }

    public double getMoisture() {
        return this.moisture;
    }

    public void setDiastaticPower(double diastaticPower) {
        this.diastaticPower = diastaticPower;
    }

    public double getDiastaticPower() {
        return this.diastaticPower;
    }

    public void setIbuGalPerLb(double ibuGalPerLb) {
        this.ibuGalPerLb = ibuGalPerLb;
    }

    public double getIbuGalPerLb() {
        return this.ibuGalPerLb;
    }

    public void setMaxInBatch(double maxInBatch) {
        this.maxInBatch = maxInBatch;
    }

    public double getMaxInBatch() {
        return this.maxInBatch;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getProtein() {
        return this.protein;
    }

    public boolean addAfterBoil() {
        return this.addAfterBoil;
    }
}
