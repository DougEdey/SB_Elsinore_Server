package com.sb.elsinore;
import java.util.AbstractMap;
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

	Map<String, Object> timers = new HashMap<String, Object>();

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
	public Entry<String, Date> getTimer(String name) {
		return (Entry<String, Date>) timers.get(name);
	}

	public String getTimerString(String name) throws NullPointerException {
		return lFormat.format(timers.get(name));
	}

	public void setTimer(String name, Entry<String, Date> timerData) {
		HashMap<String, Date> timerElements = 
				(HashMap<String, Date>) timers.get(name);
		
		if (timerElements == null) {
			timerElements = new HashMap<String, Date>();
		}
		
		timerElements.put(timerData.getKey(), timerData.getValue());
		
		timers.put(name, timerElements);
	}

	public void startTimer(String name, String startIn) {
		if (startIn == null || startIn.equals("null")) {
			timers.put(name, null);
		} else {
			Entry<String, Date> startEntry = 
					new AbstractMap.SimpleEntry<String, Date>("start", parseDateString(startIn));
			setTimer(name, startEntry);
		}
	}

	public void stopTimer(String name, String stopIn) {
		if (stopIn == null || stopIn.equals("null")) {
			timers.put(name, null);
		} else {
			Entry<String, Date> startEntry = 
					new AbstractMap.SimpleEntry<String, Date>("end", parseDateString(stopIn));
			setTimer(name, startEntry);
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

		Iterator<Entry<String, Object>> it = timers.entrySet().iterator();

		Entry<String, Object> e = null;
		
		while (it.hasNext()) {
			e = it.next();
			if (e.getValue() != null ) {
				
				// iterate the child hashmap
				HashMap<String, Date> valueEntry = (HashMap<String, Date>) e.getValue();
				Iterator<Entry<String, Date>> dateIt = valueEntry.entrySet().iterator();
				
				// get the Timer Name Object
				JSONObject timerJSON = (JSONObject) status.get(e.getKey());
				
				// Add one if there's not an object
				if (timerJSON == null) {
					timerJSON = new JSONObject();
				}
				
				while (dateIt.hasNext()) {
					Entry<String, Date> dateEntry = dateIt.next();
					timerJSON.put(dateEntry.getKey(), lFormat.format(dateEntry.getValue()));
				}
				
				status.put(e.getKey(), timerJSON);
			}
		}
		
		return status;
	}
		

}
