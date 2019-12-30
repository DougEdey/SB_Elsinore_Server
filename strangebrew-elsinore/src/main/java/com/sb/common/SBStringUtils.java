/*
 * Created on Jun 13, 2005
 * by aavis
 *
 */

package com.sb.common;

import com.sb.elsinore.Messages;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Pattern;

/**
 * @author aavis
 *
 */
public class SBStringUtils {

	private static Pattern tempRegexp = null;

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
		if (input == null)
			return "";
		if (input.length() < len)
			return input;

		int i = 0;
		int lastSpace = 0;

		while (i + len < input.length()) {
			String temp = input.substring(i, i + len);
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
		appRoot = URLDecoder.decode(jpath, "UTF-8");	
		
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

		return new String(buf);
	}
	
	public static double round(double d, int decimalPlace){
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}


    /**
     * Format the incoming time to a human value from minutes.
     * @param timeMinutes The time in minutes to format.
     * @return A string representing the time.
     */
    public static String formatTime(double timeMinutes) {
        if (timeMinutes <= 90) {
            return timeMinutes + " " + Messages.MINS;
        }
        // Hours
        double time = timeMinutes/60.0;
        if (time < 24) {
            return time + " " + Messages.HOURS;
        }
        // Days
        time = time/24;
        if (time < 7) {
            return time + " " + Messages.DAYS;
        }
        // Weeks
        time = time/7;
        return time + " " + Messages.WEEKS;
    }


	public static Pattern getTempRegexp() {
		if (tempRegexp == null) {
			tempRegexp = Pattern.compile("^(-?)([0-9]+)(.[0-9]{1,2})?$");
		}
		return tempRegexp;
	}
}
