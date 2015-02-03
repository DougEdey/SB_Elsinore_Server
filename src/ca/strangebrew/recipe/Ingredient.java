package ca.strangebrew.recipe;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.strangebrew.recipe.Quantity.Converter;

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
	private String description="";
	private String name="";
	private Quantity stock = new Quantity();
	private String type;
	private boolean modified;
    private Quantity inventory = new Quantity();


    public Ingredient() {
		modified = true;
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
	      if(tmp.name.equalsIgnoreCase(this.name)){	    	  
	       return true;                            
	      }
	      else                                                                                          
	       return false;                                                                                
	    }                                                                                                                            
	  }
	public double getAmountAs(String s){ return amount.getValueAs(s); }
	public double getStockAs(String s) { return stock.getValueAs(s); }
	public double getStock() { return stock.getValue(); }
	// Get methods:
	public double getCostPerU(){ return costPerU; }
	public Date getDate(){ return dateBought; }
	public String getDescription(){ return description; }
	public boolean getModified(){ return modified; }
	public String getName(){ return name; }	
	public String getType(){ return type; }
	public String getUnits(){ return amount.getUnits(); }
	
	public double getCostPerUAs(String to){
		// current value / new value * cost
		return costPerU;
	}
	
	public String getUnitsAbrv(){ return amount.getAbrv(); }
	
	public void setAmount(double a){ amount.setAmount(a); }
	public void setStock(double a){ 
		if(a < 0.00) 
			stock.setAmount(0);
		else
			stock.setAmount(a); 
	}
	/**
	 * Handles a string of the form "d u", where d is a double
	 * amount, and u is a string of units.  For importing the
	 * quantity attribute from QBrew xml.
	 * @param a
	 */
	
	public void setAmountAndUnits(String a){
		int i = a.indexOf(" ");
		String d = a.substring(0,i);
		String u = a.substring(i);
		Double dAmount = 0.0;
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
			Number number = format.parse(d.trim());
		    dAmount = number.doubleValue();
			
		} catch (NumberFormatException m) {
			return;
		} catch (ParseException e) {
			return;
		}
		amount.setAmount(dAmount);
		amount.setUnits(u.trim());
	}
	public void setAmountAs(double a, String u) {
		double converted = Quantity.convertUnit(u, amount.getUnits(), a);
		amount.setAmount(converted);
	}
	public void setCost(double c){ costPerU = c; }
	public void setCost(String c){
		if (c.substring(0,1).equals("$")) {
			c = c.substring(1, c.length()); // trim leading "$"
		}
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
			Number number = format.parse(c.trim());
		    costPerU = number.doubleValue();
			
		} catch (NumberFormatException m) {
		} catch (ParseException e) {
		}
	}
	public void setDate(String d){ 
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		try{
		    dateBought = df.parse(d);
		}catch (ParseException p){
		}
		
	}
	
	public void setDescription(String d){ description = d; }
	
	// Setter methods:
	public void setModified(boolean b){ modified = b; }
	public void setName(String n){ name = n; }
	public void setType(String t){ type = t; }
	public void setUnits(String a){ amount.setUnits(a); }
	
	public void convertTo(String newUnits){
		setCost(Quantity.convertUnit(newUnits, getUnits(), getCostPerU()));		
		
		amount.convertTo(newUnits);		
	}
	
	// implement to support comboboxes in Swing:
	public String toString(){
		return name;
	}

	public int compareTo(Ingredient i) {
		int result = this.getName().compareToIgnoreCase(i.getName());				
		return result;				
	}

    public void setInventory(String inventory) {
        int i = inventory.indexOf(" ");
        String d = inventory.substring(0,i);
        String u = inventory.substring(i);
        Double dAmount = 0.0;
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
            Number number = format.parse(d.trim());
            dAmount = number.doubleValue();

        } catch (NumberFormatException m) {
            return;
        } catch (ParseException e) {
            return;
        }
        this.inventory.setAmount(dAmount);
        this.inventory.setUnits(u.trim());
    }

    public Quantity getInventory() {
        return inventory;
    }

    public Quantity getAmount() {
        return amount;
    }
}
