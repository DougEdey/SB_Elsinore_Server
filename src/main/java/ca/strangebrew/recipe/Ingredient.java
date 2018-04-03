package ca.strangebrew.recipe;

import javax.annotation.Nonnull;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * $Id: Ingredient.java,v 1.8 2012/06/02 19:40:58 dougedey Exp $
 * Created on Oct 21, 2004
 * @author aavis
 *
 * Base class for all ingredients.  Dunno why I didn't do this
 * in the first place.
 */
public class Ingredient implements Comparable<Ingredient> {
	Quantity amount = new Quantity();
	private double costPerU;
	private Date dateBought = new Date();
	private String description = null;
	private String name="";
	private Quantity stock = new Quantity();
	private String type;
	private boolean modified;
    private Quantity inventory = new Quantity();
    private String origin;


    public Ingredient() {
        this.modified = true;
	}
	// override the equals so we can compare:
	public boolean equals(Object obj)                                    
	  {                                                                                                                              
	    if(obj == this)                                                    
	    return true;                                                                                    
	                                                                                                                                 
	    /* is obj reference null */                                                                                                       
	    if(obj == null)                                                                                 
	    return false;                                                                                                              
	                                                                                                                                 
	    /* Make sure references are of same type */                                                     
	                                                                                                                                 
	    if(!(getClass() == obj.getClass()))                                                             
	    return false;                                                                                   
	    else                                                                                            
	    {                                                                                                                            
	        Ingredient tmp = (Ingredient)obj;
            return tmp.name.equalsIgnoreCase(this.name);
	    }                                                                                                                            
	  }
	public double getAmountAs(String s){ return this.amount.getValueAs(s); }
	public double getStockAs(String s) { return this.stock.getValueAs(s); }
	public double getStock() { return this.stock.getValue(); }
	// Get methods:
	public double getCostPerU(){ return this.costPerU; }
	public Date getDate(){ return this.dateBought; }
	public String getDescription(){ return this.description; }
	public boolean getModified(){ return this.modified; }
	public String getName(){ return this.name; }
	public String getType(){ return this.type; }
	public String getUnits(){ return this.amount.getUnits(); }

    @SuppressWarnings("unused")
	public double getCostPerUAs(String to){
		// current value / new value * cost
		return this.costPerU;
	}
	
	public String getUnitsAbrv(){ return this.amount.getAbrv(); }
	
	public void setAmount(double a){
        this.amount.setAmount(a); }
	public void setStock(double a){ 
		if(a < 0.00)
            this.stock.setAmount(0);
		else
            this.stock.setAmount(a);
	}
	/**
	 * Handles a string of the form "d u", where d is a double
	 * amount, and u is a string of units.  For importing the
	 * quantity attribute from QBrew xml.
	 * @param a The new units.
	 */
	
	public void setAmountAndUnits(String a){
		int i = a.indexOf(" ");
		String d = a.substring(0,i);
		String u = a.substring(i);
		Double dAmount;
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
			Number number = format.parse(d.trim());
		    dAmount = number.doubleValue();
			
		} catch (NumberFormatException | ParseException m) {
			return;
		}
        this.amount.setAmount(dAmount);
        this.amount.setUnits(u.trim());
	}

	public void setAmountAs(double a, String u) {
		double converted = Quantity.convertUnit(u, this.amount.getUnits(), a);
        this.amount.setAmount(converted);
	}
	public void setCost(double c){
        this.costPerU = c; }
	public void setCost(String c){
		if (c.substring(0,1).equals("$")) {
			c = c.substring(1, c.length()); // trim leading "$"
		}
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
			Number number = format.parse(c.trim());
            this.costPerU = number.doubleValue();
			
		} catch (NumberFormatException | ParseException ignored) {
		}
    }
	public void setDate(String d){ 
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		try{
            this.dateBought = df.parse(d);
		}catch (ParseException ignored){
		}
		
	}
	
	public void setDescription(String d){
        this.description = d; }
	
	// Setter methods:
	public void setModified(boolean b){
        this.modified = b; }
	public void setName(String n){
        this.name = n; }
	public void setType(String t){
        this.type = t; }
	public void setUnits(String a){
        this.amount.setUnits(a); }
	
	public void convertTo(String newUnits){
		setCost(Quantity.convertUnit(newUnits, getUnits(), getCostPerU()));

        this.amount.convertTo(newUnits);
	}
	
	// implement to support comboboxes in Swing:
	public String toString(){
		return this.name;
	}

	public int compareTo(@Nonnull Ingredient i) {
        return this.getName().compareToIgnoreCase(i.getName());
	}

    public void setInventory(String inventory) throws ParseException {
        int i = inventory.indexOf(" ");
        String d = inventory.substring(0,i);
        String u = inventory.substring(i);
        Double dAmount;
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
            Number number = format.parse(d.trim());
            dAmount = number.doubleValue();

        } catch (NumberFormatException m) {
            return;
        }
        this.inventory.setAmount(dAmount);
        this.inventory.setUnits(u.trim());
    }

    public Quantity getInventory() {
        return this.inventory;
    }

    public Quantity getAmount() {
        return this.amount;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getOrigin() {
        return this.origin;
    }
}
