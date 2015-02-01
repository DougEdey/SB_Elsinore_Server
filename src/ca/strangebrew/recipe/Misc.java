package ca.strangebrew.recipe;

import ca.strangebrew.SBStringUtils;

/**
 * $Id $
 * @author aavis
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
	    StringBuffer sb = new StringBuffer();
	    sb.append( "    <ITEM>\n" );
	    sb.append( "      <NAME>"+getName()+"</NAME>\n" );
	    sb.append( "      <AMOUNT>"+getAmountAs(getUnits())+"</AMOUNT>\n" );
	    sb.append( "      <UNITS>"+getUnits()+"</UNITS>\n" );
	    sb.append( "      <STAGE>"+stage+"</STAGE>\n" );
	    sb.append( "      <TIME>"+time+"</TIME>\n" );
	    sb.append( "      <COMMENTS>"+SBStringUtils.subEntities(comments)+"</COMMENTS>\n" );
	    sb.append( "    </ITEM>\n" );
	    return sb.toString();
	}
}
