package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class WaterProfile implements Comparable<WaterProfile> {
	private String name;
	private String description;
	private double ca;
	private double mg;
	private double na;
	private double so4;
	private double hco3;
	private double cl;
	private double hardness;
	private double tds;
	private double ph;
	private double alkalinity;
    private String notes;
    private Quantity amount;

    public WaterProfile() {
		name = "Distilled/RO";
		ph = 5.80000019073486;
	}
	
	public WaterProfile(String name) {
		this.name = name;
	}

	public double getAlkalinity() {
		return alkalinity;
	}

	public double getCa() {
		return ca;
	}

	public double getCl() {
		return cl;
	}

	public String getDescription() {
		return description;
	}

	public double getHardness() {
		return hardness;
	}

	public double getHco3() {
		return hco3;
	}

	public double getMg() {
		return mg;
	}

	public double getNa() {
		return na;
	}

	public String getName() {
		return name;
	}

	public double getPh() {
		return ph;
	}

	public double getSo4() {
		return so4;
	}

	public double getTds() {
		return tds;
	}

	public void setAlkalinity(double alkalinity) {
		this.alkalinity = alkalinity;
	}

	public void setCa(double ca) {
		this.ca = ca;
	}

	public void setCl(double cl) {
		this.cl = cl;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setHardness(double hardness) {
		this.hardness = hardness;
	}

	public void setHco3(double hco3) {
		this.hco3 = hco3;
	}

	public void setMg(double mg) {
		this.mg = mg;
	}

	public void setNa(double na) {
		this.na = na;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPh(double ph) {
		this.ph = ph;
	}

	public void setSo4(double so4) {
		this.so4 = so4;
	}

	public void setTds(double tds) {
		this.tds = tds;
	}
	
	public String toString() {		
		
		return String.format("%s => %3.1fCa %3.1fMg %3.1fNa %3.1fSo4 %3.1fHCO3 %3.1fCl %3.1fHardness %3.1fTDS %3.1fpH %3.1fAlk",
                name, ca, mg, na, so4, hco3, cl, hardness, tds, ph, alkalinity);
	}
	
	public String toXML(int indent) {
		String xml = "";

		xml += SBStringUtils.xmlElement("NAME", name, indent);
		xml += SBStringUtils.xmlElement("CA", Double.toString(ca), indent);
		xml += SBStringUtils.xmlElement("MG", Double.toString(mg), indent);
		xml += SBStringUtils.xmlElement("NA", Double.toString(na), indent);
		xml += SBStringUtils.xmlElement("SO4", Double.toString(so4), indent);
		xml += SBStringUtils.xmlElement("HCO3", Double.toString(hco3), indent);
		xml += SBStringUtils.xmlElement("CL", Double.toString(cl), indent);
		xml += SBStringUtils.xmlElement("HARDNESS", Double.toString(hardness), indent);
		xml += SBStringUtils.xmlElement("TDS", Double.toString(tds), indent);
		xml += SBStringUtils.xmlElement("PH", Double.toString(ph), indent);
		xml += SBStringUtils.xmlElement("ALKALINITY", Double.toString(alkalinity), indent);

		return xml;
	}

    public Element getElement(Document recipeDocument) {
        Element waterElement = recipeDocument.createElement("WATER");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(this.getName());
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CALCIUM");
        tElement.setTextContent(Double.toString(this.ca));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BICARBONATE");
        tElement.setTextContent(Double.toString(this.hco3));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("SULFATE");
        tElement.setTextContent(Double.toString(this.so4));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CHLORIDE");
        tElement.setTextContent(Double.toString(this.cl));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("SODIUM");
        tElement.setTextContent(Double.toString(this.na));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("MAGNESIUM");
        tElement.setTextContent(Double.toString(this.mg));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("PH");
        tElement.setTextContent(Double.toString(this.ph));
        waterElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(this.notes);
        waterElement.appendChild(tElement);

        if (this.amount != null && this.amount.getValue() != 0.0) {
            tElement = recipeDocument.createElement("DISPLAY_AMOUNT");
            tElement.setTextContent(this.amount.toString());
            waterElement.appendChild(tElement);
        }

        return waterElement;
    }
	public int compareTo(WaterProfile w) {
		int result = this.getName().compareToIgnoreCase(w.getName());				
		return (result == 0 ? -1 : result);		
	}

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setAmount(String newAmount) {
        int i = newAmount.indexOf(" ");
        String d = newAmount.substring(0,i);
        String u = newAmount.substring(i);
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

    public Quantity getAmount() {
        return amount;
    }
}
