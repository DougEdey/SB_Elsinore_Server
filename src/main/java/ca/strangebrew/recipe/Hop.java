package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;
import com.sb.elsinore.BrewServer;

import javax.annotation.Nonnull;
/**
 * $Id: Hop.java,v 1.17 2012/06/02 19:40:58 dougedey Exp $
 * Created on Oct 5, 2004
 *
 * Base class for hops.  This object doesn't do much except hold data and
 * get/set data.
 */
public class Hop extends Ingredient implements Comparable<Ingredient> {
	private double alpha;
	private String add;
	private int minutes;
	private double storage;
	private double IBU;

	// Hops should know about hop types
	static final public String LEAF = "Leaf";
	static final public String PELLET = "Pellet";
	static final public String PLUG = "Plug";
	static final public String BOIL = "Boil";
	static final public String FWH = "FWH";
	static final public String DRY = "Dry";
	static final public String MASH = "Mash";

	static final public String[] forms = { LEAF, PELLET, PLUG };
	static final public String[] addTypes = { BOIL, FWH, DRY, MASH };
    private double myrcene;
    private double cohumulone;
    private double caryophyllene;
    private double humulene;
    private String substitutes;
    private double hsi;
    private double beta;
    private String form;

    // Constructors:

	public Hop() {
		// default constructor
		setName("New Hop");
		setType(LEAF);
		setAdd(BOIL);
		setUnits(Quantity.OZ); // oz
	}

	public Hop(String u, String t) {
		setUnits(u);
		setType(t);
		setAdd(BOIL);
	}

	// get methods:
	public String getAdd() {
		return this.add;
	}

	public double getAlpha() {
		return this.alpha;
	}

	public double getIBU() {
		return this.IBU;
	}

	public int getMinutes() {
		return this.minutes;
	}

	public double getStorage() {
		return this.storage;
	}

	// Setter methods:
	public void setAdd(String a) {
        this.add = a;
	}

	public void setAlpha(double a) {
        this.alpha = a;
	}

	// public void setForm(String f){ form = f; }
	public void setIBU(double i) {
        this.IBU = i;
	}

	public void setMinutes(int m) {
        this.minutes = m;
	}

	public void setStorage(double s) {
        this.storage = s;
	}

	public String toXML() {
		return String.format("    <ITEM>\n" +
						"      <HOP>%s</HOP>\n" +
						"      <AMOUNT>%s</AMOUNT>\\n" +
						"      <UNITS>%s</UNITS>\n" +
						"      <FORM>%s</FORM>\n" +
						"      <ALPHA>%s</ALPHA>\n" +
						"      <COSTOZ>%s</COSTOZ>\n" +
						"      <ADD>%s</ADD>\n" +
						"      <DESCRIPTION>%s</DESCRIPTION>" +
						"      <DATE>%s</DATE>" +
						"    </ITEM>\n",
						getName(), getAmountAs(getUnits()), getUnitsAbrv(),
						getType(), this.alpha, getCostPerU(), this.add,
				SBStringUtils.subEntities(getDescription()),
						getDate());
	}

	public int compareTo(@Nonnull Ingredient i) {
		if (i instanceof Hop) {
			return compareTo((Hop) i);
		} else {
			return super.compareTo(i);
		}
	}

	
	public int compareTo(Hop h) {
		
		
		// Check to see if the additions are at the same time
		if (this.getMinutes() == 0 && h.getMinutes() == 0) {
			// Check to see if we have dry hopping
			if (this.getAdd().equals(h.getAdd())) {
				// Same addition, continue the compare
				return super.compareTo(h);
			} else {
				// Different addition type, so compare that. Boil is luckily
				// prior to Dry
				return this.getAdd().compareToIgnoreCase(h.getAdd());

			}
		} else {
			// Times are not the same, straightforward comparrison
			int result = ((Integer) h.getMinutes()).compareTo(this
					.getMinutes());
			return (result == 0 ? -1 : result);
		}
	}


    public void setTimeString(String timeString) {
        String[] parts = timeString.split(" ");
        try {
            if (parts.length == 2) {
                int period = (int) Double.parseDouble(parts[0]);
                if (parts[1].equals("days")) {
                    period = period * 60 * 24;
                }
                this.setMinutes(period);
            }
        } catch (NumberFormatException nfe) {
            BrewServer.LOG.info("Couldn't parse an integer.");
            BrewServer.LOG.warning(nfe.getMessage());
        }
    }

    public void setMyrcene(double myrcene) {
        this.myrcene = myrcene;
    }

    public double getMyrcene() {
        return this.myrcene;
    }

    public void setCohumulone(double cohumulone) {
        this.cohumulone = cohumulone;
    }

    public double getCohumulone() {
        return this.cohumulone;
    }

    public void setCaryophyllene(double caryophyllene) {
        this.caryophyllene = caryophyllene;
    }

    public double getCaryophyllene() {
        return this.caryophyllene;
    }

    public void setHumulene(double humulene) {
        this.humulene = humulene;
    }

    public double getHumulene() {
        return this.humulene;
    }

    public void setSubstitutes(String substitutes) {
        this.substitutes = substitutes;
    }

    public String getSubstitutes() {
        return this.substitutes;
    }

    public void setHsi(double hsi) {
        this.hsi = hsi;
    }

    public double getHsi() {
        return this.hsi;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getBeta() {
        return this.beta;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getForm() {
        return this.form;
    }
}
