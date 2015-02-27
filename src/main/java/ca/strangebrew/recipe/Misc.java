package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * $Id $
 * @author Doug Edey
 *
 */
public class Misc extends Ingredient {

	private String comments;
	private String stage;
	private int time;
	static final public String MASH = "Mash";
	static final public String BOIL = "Boil";
	static final public String PRIMARY = "Primary";
	static final public String SECONDARY = "Secondary";
	static final public String BOTTLE = "Bottle";
	static final public String KEG = "Keg";
	static final public String[] stages = {MASH, BOIL, PRIMARY, SECONDARY, BOTTLE, KEG};
    private String use;
    private String useFor;

    // default constructor
	public  Misc() {
		setName("");
		setCost(0);
		setDescription("");
		setUnits(Quantity.G);
	}
	
	// get methods
	public String getComments(){ return comments; }
	public String getStage(){ return stage; }
	public int getTime(){ return time; }
	
	// set methods
	public void setComments(String c){ comments = c; }
	public void setStage(String s){ stage = s; }
	public void setTime(int t){ time = t; }

	public String toXML(){
	    StringBuilder sb = new StringBuilder();
	    sb.append("    <ITEM>\n");
	    sb.append("      <NAME>").append(getName()).append("</NAME>\n");
        if (Quantity.getTypeFromUnit(this.getUnits()).equals(Quantity.WEIGHT)) {
            sb.append("      <AMOUNT>").append(getAmountAs(Quantity.KG)).append("</AMOUNT>\n");
            sb.append("      <UNITS>" + Quantity.KG + "</UNITS>\n" );
            sb.append("      <AMOUNT_IS_WEIGHT>true</AMOUNT_IS_WEIGHT>\n");
        } else {
            sb.append("      <AMOUNT>").append(getAmountAs(Quantity.LITRES)).append("</AMOUNT>\n");
            sb.append("      <UNITS>" + Quantity.LITRES + "</UNITS>\n" );
            sb.append("      <AMOUNT_IS_WEIGHT>false</AMOUNT_IS_WEIGHT>\n");
        }
	    sb.append("      <STAGE>").append(stage).append("</STAGE>\n");
	    sb.append("      <TIME>").append(time).append("</TIME>\n");
	    sb.append("      <COMMENTS>").append(SBStringUtils.subEntities(comments)).append("</COMMENTS>\n");
	    sb.append("    </ITEM>\n");
	    return sb.toString();
	}

    public Element createElement(Document recipeDocument) {
        Element miscElement = recipeDocument.createElement("MISC");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        miscElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(this.getName());
        miscElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent(this.getType());
        miscElement.appendChild(tElement);

        tElement = recipeDocument.createElement("USE");
        tElement.setTextContent(this.getUse());
        miscElement.appendChild(tElement);

        tElement = recipeDocument.createElement("USE_FOR");
        tElement.setTextContent(this.getUseFor());
        miscElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TIME");
        tElement.setTextContent(Integer.toString(this.getTime()));
        miscElement.appendChild(tElement);

        if (Quantity.getTypeFromUnit(this.amount.getUnits()).equals("WEIGHT")) {
            tElement = recipeDocument.createElement("AMOUNT");
            tElement.setTextContent(Double.toString(this.getAmountAs(Quantity.KG)));
            miscElement.appendChild(tElement);

            tElement = recipeDocument.createElement("AMOUNT_IS_WEIGHT");
            tElement.setTextContent("true");
            miscElement.appendChild(tElement);
        } else {
            tElement = recipeDocument.createElement("AMOUNT");
            tElement.setTextContent(Double.toString(this.getAmountAs(Quantity.LITRES)));
            miscElement.appendChild(tElement);

            tElement = recipeDocument.createElement("AMOUNT_IS_WEIGHT");
            tElement.setTextContent("false");
            miscElement.appendChild(tElement);
        }

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(this.comments);
        miscElement.appendChild(tElement);

        return miscElement;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public String getUse() {
        return use;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
    }

    public String getUseFor() {
        return useFor;
    }
}
