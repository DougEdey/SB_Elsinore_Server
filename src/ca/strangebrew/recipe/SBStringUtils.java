package ca.strangebrew.recipe;
/*
 * Created on Jun 13, 2005
 * by aavis
 *
 */


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author aavis
 *
 */
public class SBStringUtils {

	/**
	 * Returns a multi-line tooltip by wrapping
	 * the String input in <html> and inserting breaks <br>.
	 * @param len	the maximum line-length.  The method will insert
	 * a break at the space closest to this number.
	 * @param input	the input String.
	 * @return the new multi-lined string.
	 */
	public static String multiLineToolTip(int len, String input) {
		String s = "";
		int length = len;
		if (input == null)
			return "";
		if (input.length() < length)
			return input;

		int i = 0;
		int lastSpace = 0;

		while (i + length < input.length()) {
			String temp = input.substring(i, i + length);
			lastSpace = temp.lastIndexOf(" ");
			s += temp.substring(0, lastSpace) + "<br>";
			i += lastSpace + 1;
		}
		s += input.substring(i, input.length());

		s = "<html>" + s + "</html>";

		return s;
	}

	/**
	 * Returns a string with all ocurrences of special characters 
	 * replaced by their corresponding xml entities.
	 * @param input	the string you want to replace all special characters
	 * in
	 * @return a string with valid xml entites
	 */
	public static String subEntities(String input) {

		String sub[][] = {{"&", "&amp;"}, {"<", "&lt;"}, {">", "&gt;"}, {"'", "&apos;"},
				{"\"", "&quot;"}};
		String output = input;
		int index = 0;
		if (input == null)
			return "";
		for (int i = 0; i < sub.length; i++) {
			while (output.indexOf(sub[i][0], index) != -1) {
				index = output.indexOf(sub[i][0], index);
				output = output.substring(0, index) + sub[i][1]
						+ output.substring(index + sub[i][0].length());
				index = index + sub[i][0].length();
			}
		}

		return output;
	}
	
	
	final public static NumberFormat nf = NumberFormat.getNumberInstance();
	final public static DecimalFormat df = (DecimalFormat)nf; 
	final public static NumberFormat myNF = NumberFormat.getCurrencyInstance(); // Use the country currency
	final public static DateFormat dateFormatShort = DateFormat.getDateInstance(DateFormat.SHORT);
	final public static DateFormat dateFormatFull = DateFormat.getDateInstance(DateFormat.FULL);
	
	public static String format(double value, int decimal){
		
		String pattern = "0";
		switch (decimal){
			case 0: pattern = "0";
			break;
			case 1: pattern = "0.0";
			break;
			case 2: pattern = "0.00";
			break;
			case 3: pattern = "0.000";
			break;
			
		}
		df.applyPattern(pattern);
		return df.format(value);		
	}
	
	public static String xmlElement(String elem, String content, int i) {
		String s = "";
		for (int j = 0; j<i;j++)
			s += " ";		
		s += "<" + elem + ">" + content + "</" + elem + ">\n";		
		return s;
	}
	
	public static String getAppPath(String type) throws UnsupportedEncodingException{
		String appRoot = "";
		String path = "";
		String slash = System.getProperty("file.separator");
		
		String jpath = new File(SBStringUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath();
		//File nFile = new File(jpath).getParentFile().getPath();
		appRoot = URLDecoder.decode(jpath, "UTF-8");
		Debug.print("Path: "+ appRoot);
		
		
		if (type.equals("data"))
			path = appRoot + slash + "src" + slash + "ca" 
				+ slash + "strangebrew" + slash + "data";
		else if (type.equals("icons"))
			path = appRoot + slash + "src" + slash + "ca" 
			+ slash + "strangebrew" + slash + "icons";
		else if (type.equals("recipes"))
			path = appRoot + slash + "recipes";
		else if (type.equals("help"))
			path = "file://" + appRoot + slash + "help" + slash;
		else if (type.equals("ini"))
			path = appRoot + slash;
		else
			path = appRoot;
		
		return path;
	}
	
	static public String capitalize(String orig) {
		StringBuffer buf = new StringBuffer(orig);
		for (int i = 0; i < orig.length(); i++ ) {
			if (i == 0 || orig.charAt(i - 1) == ' ')  {
				if (Character.isLowerCase(orig.charAt(i))) {
					buf.setCharAt(i, Character.toUpperCase(orig.charAt(i)));
				}
			}
		}
		
		String r = new String(buf);
		return r;
	}
	
	public static double round(double d, int decimalPlace){
		// see the Javadoc about why we use a String in the constructor
		// http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace,BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}	
}
