/*
 * Created on Jun 8, 2005
 * by aavis
 *
 */
package ca.strangebrew.recipe;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 
 * @author aavis
 *
 * Note class to store notes
 * Eventually this will all go into a much larger "Brew Session" class
 */

public class Note {
	
	private Date date;
	private String type;
	private String note;
	static final public String PLANNING = "Planning";
	static final public String BREWED = "Brewed";
	static final public String FERMENTATION = "Fermentation";
	static final public String RACKED = "Racked";
	static final public String CONDITIONED = "Conditioned";
	static final public String KEGGED = "Kegged";
	static final public String BOTTLED = "Bottled";
	static final public String TASTING = "Tasting";
	static final public String CONTEST = "Contest";
	static final public String[] types = {PLANNING, BREWED, FERMENTATION, RACKED, CONDITIONED, KEGGED, BOTTLED, TASTING, CONTEST};
	
	public Note() {
        this.date = new Date();
        this.type = PLANNING;
        this.note = "";
	}
	
	/**
	 * @return Returns the date.
	 */
	public Date getDate() {
		return this.date;
	}
	/**
	 * @param date The date to set.
	 */
	public void setDate(Date date) {
		this.date = date;
	}
	/**
	 * @return Returns the note.
	 */
	public String getNote() {
		return this.note;
	}
	/**
	 * @param note The note to set.
	 */
	public void setNote(String note) {
		this.note = note;
	}
	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return this.type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}		
	
	public String toXML(){
		    StringBuffer sb = new StringBuffer();
		    sb.append( "    <ITEM>\n" );
		    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");		
		    sb.append("      <DATE>").append(df.format(getDate())).append("</DATE>\n");
		    sb.append("      <TYPE>").append(getType()).append("</TYPE>\n");
		    sb.append("      <NOTE>").append(getNote()).append("</NOTE>\n");
		    sb.append( "    </ITEM>\n" );
		    return sb.toString();
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
	    sb.append(getType()).append(": ");
	    sb.append( getNote() );
	    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");		
	    sb.append(" (").append(df.format(getDate())).append(")\n");
	    return sb.toString();
	}
	
	

}