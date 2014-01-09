package com.sb.elsinore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;

public final class BrewDay {

	Map<String, Date> timers = new HashMap<String, Date>();

	// generate the date time parameters
	DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
	DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");
	DateFormat lFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	
	// the actual dates
	Date updated = null;
	
	//each of the setters can be via string or date
	// not checking the existing data for now, to allow multiple brew days without restarting the server
	Date parseDateString(String dateString) {
		Date dDate = null;

		// this better be a datestamp string
		try {
			dDate = new Date(Long.parseLong(dateString));
		} catch (NumberFormatException e) {
			BrewServer.log.log(Level.INFO, "Error: " + dateString + " could not be parsed as a long, trying date", e);
			try{
				dDate = lFormat.parse(dateString);
			} catch (ParseException p) {
				BrewServer.log.info("Could not parse date, giving up ya hoser");
			}
		}
		
		// check to see if we're null
		if(dDate == null) {
			BrewServer.log.info("Unparseable string provided " + dateString);
		}
		return dDate;
	}


	// Timers
	public Date getTimer(String name) {
		return timers.get(name);
	}

	public String getTimerString(String name) throws NullPointerException {
		return lFormat.format(timers.get(name));
	}

	public void setTimer(String name, Date startIn) {
		timers.put(name, startIn);
	}

	public void setTimer(String name, String startIn) {
		if (startIn == null || startIn.equals("null")) {
			timers.put(name, null);
		} else {
			setTimer(name, parseDateString(startIn));
		}
	}

	
	
	// Updated
	public Date getUpdate() {
		return updated;
	}
	
	public String getUpdatedString() {
		return lFormat.format(updated);
	}

	public void setUpdated(Date updatedIn) {
		updated = updatedIn;
	}

	public void setUpdated(String updatedIn) {
		setUpdated(parseDateString(updatedIn));
	}
	
	public void addTimer(String name) {
		
		if (timers.containsKey(name)) {
			BrewServer.log.info("Timer: " + name + " exists");
			return;
		}
		timers.put(name, null);
		BrewServer.log.info("Added new timer: " + name);
	}
	
	// get the JSON
	@SuppressWarnings("unchecked")
	public JSONObject brewDayStatus() {
		JSONObject status = new JSONObject();

		Iterator<Entry<String, Date>> it = timers.entrySet().iterator();

		Entry<String, Date> e = null;
		
		while (it.hasNext()) {
			e = it.next();
			if (e.getValue() != null ) {
				status.put(e.getKey(), lFormat.format(e.getValue()));
			}
		}
		
		return status;
	}
		

}
