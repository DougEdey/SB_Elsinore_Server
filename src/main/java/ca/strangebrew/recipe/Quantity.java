/*
 * $Id: Quantity.java,v 1.16 2008/01/19 01:05:40 jimcdiver Exp $
 * Created on Oct 7, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

package ca.strangebrew.recipe;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author aavis
 *
 * This is the class for amounts + units.
 * It's pretty smart, and knows how to convert itself to other units.
 * 
 */

public class Quantity {

    public static final String WEIGHT = "weight";
    public static final String VOLUME = "volume";
    public static final String PRESSURE = "pressure";
    public static final String OTHER = "other";

    private String type; // can be vol, weight, or temp
	private String unit = ""; // must match one of the known units
	private String abrv; // ditto
	private double value;

    public Quantity(String stringValue) {
        int i = stringValue.indexOf(" ");
        String d = stringValue.substring(0,i);
        String u = stringValue.substring(i);
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
        this.setAmount(dAmount);
        this.setUnits(u.trim());
    }

    public static String getUnitType(String units) {
        return null;
    }

    // why can't we have structs?????
	// because a static class is better for what we are trying to acomplish here!
	public static class Converter {
		public String abrv;
		public String unit;
		public double toBase;

		public Converter(String n, String a, double t) {
			unit = n;
			abrv = a;
			toBase = t;
		}
	}

	// TODO public static string all of this stuff!
	final static public String FL_OUNCES = "fl. ounces";
	final static public String OZ = "oz";
	final static public String GALLONS_US = "gallons US";
	final static public String GAL = "gal";
	final static public String LITRES = "litres";
	final static public String L = "l";
	final static public String MILLILITERS = "milliliters";
	final static public String ML = "ml";
	final static public String QUART_US = "quart US";
	final static public String QT = "qt";	
	final static public String OUNCES = "ounces";
//	final static public String OZ = "oz";
	final static public String POUNDS = "pounds";
	final static public String LB = "lb";
	final static public String MILLIGRAMS = "milligrams";
	final static public String MG = "mg";
	final static public String GRAMS = "grams";
	final static public String G = "g";
	final static public String KILOGRAMS = "kilgrams";
	final static public String KG = "kg";
	
	final static public Converter volUnits[] =
	{
		new Converter("barrel IMP", "bbl Imp", 0.023129837),
		new Converter("barrel US", "bbl", 0.032258065),
		new Converter(FL_OUNCES, OZ, 128),
		new Converter("gallons IMP", "gal Imp", 0.8327),
		new Converter(GALLONS_US, GAL, 1),
		new Converter(LITRES, L, 3.7854),
		new Converter(MILLILITERS, ML, 3785.4118),
		new Converter("pint US", "pt", 8),
		new Converter(QUART_US, QT, 4)
	};
	

	final static public Converter weightUnits[] =
	{
		new Converter(MILLIGRAMS, MG, 4535.9237),
		new Converter(GRAMS, G, 453.59237),
		new Converter(KILOGRAMS, KG, 0.45359237),
		new Converter(OUNCES, OZ, 16),
		new Converter(POUNDS, LB, 1),
		new Converter("ton S", "T", 0.0005),
		new Converter("tonne SI", "T SI", 0.000453592)
	};

	final static public Converter pressureUnits[] = 
	{
		new Converter("pounds per square inch", "psi", 1),
		new Converter("kilopascals", "KPa", 6.8947624),
		new Converter("bar", "bar", 0.068947635),
		new Converter("atmospheres", "atm", 0.0680460253)
	};

    final static public Converter otherUnits[] =
    {
        new Converter("packages", "pkgs", 1)
    };
	// Get/Set:

	public Quantity() {
		unit = "";
		type = "";
		abrv = "";		
	}

	public Quantity(String u, double am){
		setUnits(u);
		setAmount(am);
	}
	
	public Quantity(Quantity q) {
		this.type = q.type;
		this.unit = q.getUnits();
		this.abrv = q.getAbrv();
		this.value = q.getValue();
	}

	// This sets a quantity's unit, abrv, and type:
	public void setUnits(String s){
		String t = getTypeFromUnit(s);
		type = t;

		if (isAbrv(s)){
			unit = getUnitFromAbrv(t, s);;
			abrv = s;
		}
		// it's a unit
		else {
			unit = s;			
			abrv = getAbrvFromUnit(t, s);
		}
	}

	// set the amount only:
	public void setAmount(double am){
		value = am;
	}

	public double getValue(){ return value;	}
	public String getUnits(){ return unit; }
	public String getAbrv(){ return abrv; }

	public double getValueAs(String to){
		double fromBase ;
		double toBase;
		Converter[] u;

		// don't do any work if we're converting to ourselves
		if (to.equals(unit) || type.equals(abrv))
			return value;

		switch (type) {
			case Quantity.VOLUME:
				u = volUnits;
				break;
			case Quantity.PRESSURE:
				u = pressureUnits;
				break;
			default:
				u = weightUnits;
				break;
		}
		fromBase = getBaseValue(u, unit);
		toBase = getBaseValue(u, to);

		return value * (toBase / fromBase);
	}


	public void add(double v, String u){
		// convert v from u to current units
		// then add it
		Quantity q = new Quantity(u,v);
		double v2 = q.getValueAs(getUnits());
		value += v2;
	}

	public void convertTo(String to){
		if(!Objects.equals(to, this.unit)) {
			value = Quantity.convertUnit(unit, to, value);
			setUnits(to);
		}
	}

	//	 implement to support comboboxes in Swing:
	public String getName(){
		return unit;
	}	

	// private functions:	
	static private double getBaseValue(Converter[] u, String n){
		int i=0;
		while (i < u.length
				&& !u[i].abrv.equalsIgnoreCase(n)
				&& !u[i].unit.equalsIgnoreCase(n)) {
			i++;
		}
		if (i >= u.length)
			return 1;
		else 
			return u[i].toBase;
	}

	static private String getUnitFromAbrv(String t, String a){
		int i=0;
		Converter[] u;

		if (Objects.equals(t, "vol"))
			u = volUnits;
		else if (Objects.equals(t, "pressure"))
			u = pressureUnits;
		else // assume weight
			u = weightUnits;

		while (i < u.length
				&& !u[i].abrv.equalsIgnoreCase(a)) {
			i++;
		}
		if (i >= u.length)
			return null;
		else 
			return u[i].unit;
	}


	static private boolean isAbrv(String a){

		Converter[] u;
		String t = getTypeFromUnit(a);
		if (Objects.equals(t, "vol"))
			u = volUnits;
		else if (Objects.equals(t, "pressure"))
			u = pressureUnits;
		else // assume weight
			u = weightUnits;

		for (Converter unit  : u){
			if (unit.abrv.equalsIgnoreCase(a))
				return true;
		}
		return false;

	}

	static private String getAbrvFromUnit(String t, String s){
		int i=0;	
		Converter[] u;

		if (Objects.equals(t, "vol"))
			u = volUnits;
		else if (Objects.equals(t, "pressure"))
			u = pressureUnits;
		else // assume weight
			u = weightUnits;

		while (i < u.length
				&& !u[i].unit.equalsIgnoreCase(s)) {
			i++;
		}
		if (i >= u.length)
			return null;
		else 
			return u[i].abrv;
	}

	public static String getTypeFromUnit(String s){
		
		for (Converter weight : weightUnits) {
			if (weight.unit.equalsIgnoreCase(s) ||
					weight.abrv.equalsIgnoreCase(s)) {
				return Quantity.WEIGHT;
			}
		}
		
		for (Converter volume : volUnits) {
			if (volume.unit.equalsIgnoreCase(s) ||
					volume.abrv.equalsIgnoreCase(s)) {
				return Quantity.VOLUME;
			}
		}
		
		for (Converter pressure : pressureUnits) {
			if (pressure.unit.equalsIgnoreCase(s) ||
					pressure.abrv.equalsIgnoreCase(s)) {
				return Quantity.PRESSURE;
			}
		}

        for (Converter other : otherUnits) {
            if (other.unit.equalsIgnoreCase(s) ||
                    other.abrv.equalsIgnoreCase(s)) {
                return Quantity.OTHER;
            }
        }

		return "undefined";
	}

	/*
	 * These are "generic" functions you can call on any quantity object (or just
	 * create a new one).  No!.. we have static functions for a reason. Creating and garbage
	 * collecting objects bad.
	 */

	static public List<String> getListofUnits(String type, boolean abrv) {
		List<String> list = new ArrayList<>();
		switch (type){

            case "weight":
			for (Converter w : weightUnits) {
                if (abrv)
                    list.add(w.abrv);
                else
                    list.add(w.unit);

            }
                break;
            case "pressure":
			for (Converter p: pressureUnits) {
                if (abrv)
                    list.add(p.abrv);
                else
                    list.add(p.unit);
            }
                break;
            default:
			for (Converter v: volUnits) {
                if (abrv)
                    list.add(v.abrv);
                else
                    list.add(v.unit);
            }
		}			

		return list;
	}

	static public List<String> getListofUnits(String type) {
		return (getListofUnits(type, false));
	}
	
	static public String getVolAbrv(String unit) {
		Quantity q = new Quantity();
		q.setUnits(unit);
		return q.abrv;
	}

	// let's just convert a unit from something to something else
	public static double convertUnit(String from, String to, double value){
		Quantity q = new Quantity(from,value);
		return q.getValueAs(to);
	}

    /**
     * Return the current Quantity as a string of "<volume> <units>"
     * @return the current quantity
     */
    @Override
    public String toString() {
        return this.getValue() + " " + this.getUnits();
    }
}
