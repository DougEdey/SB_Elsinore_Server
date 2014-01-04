package com.sb.elsinore;
import java.util.Date;
import java.util.logging.Level;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;

public final class BrewDay {


	// generate the date time parameters
	DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
	DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");
	DateFormat lFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	
	// the actual dates
	Date updated = null;
	Date startDay = null;
	Date mashStart = null;
	Date mashEnd = null;
	Date spargeStart = null;
	Date spargeEnd = null;
	Date boilStart = null;
	Date boilEnd = null;
	Date chillStart = null;
	Date chillEnd = null;

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


	// start
	public Date getStart() {
		return startDay;
	}

	public String getStartString() {
		return lFormat.format(startDay);
	}

	public void setStart(Date startIn) {
		startDay = startIn;
	}

	public void setStart(String startIn) {
		setStart(parseDateString(startIn));
	}

	// mash in
	public Date getMashIn() {
		return mashStart;
	}

	public String getMashInString() {
		return lFormat.format(mashStart);
	}

	public void setMashIn(Date mashInIn) {
		mashStart = mashInIn;
	}

	public void setMashIn(String mashInIn) {
		setMashIn(parseDateString(mashInIn));
	}

	// mash out
	public Date getMashOut() {
		return mashEnd;
	}

	public String getMashOutString() {
		return lFormat.format(mashEnd);
	}

	public void setMashOut(Date mashOutIn) {
		mashEnd = mashOutIn;
	}

	public void setMashOut(String mashOutIn) {
		setMashOut(parseDateString(mashOutIn));
	}

	// sparge start
	public Date getSpargeStart() {
		return spargeStart;
	}

	public String getSpargeStartString() {
		return lFormat.format(spargeStart);
	}

	public void setSpargeStart(Date spargeStartIn) {
		spargeStart = spargeStartIn;
	}

	public void setSpargeStart(String spargeStartIn) {
		setSpargeStart(parseDateString(spargeStartIn));
	}

	// sparge end
	public Date getSpargeEnd() {
		return spargeEnd;
	}

	public String getSpargeEndString() {
		return lFormat.format(spargeEnd);
	}

	public void setSpargeEnd(Date spargeEndIn) {
		spargeEnd = spargeEndIn;
	}

	public void setSpargeEnd(String spargeEndIn) {
		setSpargeEnd(parseDateString(spargeEndIn));
	}

	// boil start
	public Date getBoilStart() {
		return boilStart;
	}

	public String getBoilStartString() {
		return lFormat.format(boilStart);
	}

	public void setBoilStart(Date boilStartIn) {
		boilStart = boilStartIn;
	}

	public void setBoilStart(String boilStartIn) {
		setBoilStart(parseDateString(boilStartIn));
	}
	
	// boil end
	public Date getBoilEnd() {
		return boilEnd;
	}

	public String getBoilEndString() {
		return lFormat.format(boilEnd);
	}

	public void setBoilEnd(Date boilEndIn) {
		boilEnd = boilEndIn;
	}

	public void setBoilEnd(String boilEndIn) {
		setBoilEnd(parseDateString(boilEndIn));
	}


	// chill start
	public Date getChillStart() {
		return chillStart;
	}

	public String getChillStartString() {
		return lFormat.format(chillStart);
	}

	public void setChillStart(Date chillStartIn) {
		chillStart = chillStartIn;
	}

	public void setChillStart(String chillStartIn) {
		setChillStart(parseDateString(chillStartIn));
	}

	// chill stop
	public Date getChillEnd() {
		return chillEnd;
	}

	public String getChillEndString() {
		return lFormat.format(chillEnd);
	}

	public void setChillEnd(Date chillEndIn) {
		chillEnd = chillEndIn;
	}

	public void setChillEnd(String chillEndIn) {
		setChillEnd(parseDateString(chillEndIn));
	}

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
	
	// get the JSON
	public JSONObject brewDayStatus() {
		JSONObject status = new JSONObject();

		if(startDay != null) {
			status.put("startDay", lFormat.format(startDay));
		}
		if(mashStart != null) {
			status.put("mashStart", lFormat.format(mashStart));
		}
		if(mashEnd != null) {
			status.put("mashEnd", lFormat.format(mashEnd));
		}
		if(spargeStart != null) {
			status.put("spargeStart", lFormat.format(spargeStart));
		}
		if(spargeEnd != null) {
			status.put("spargeEnd", lFormat.format(spargeEnd));
		}
		if(boilStart != null) {
			status.put("boilStart", lFormat.format(boilStart));
		}
		
		if(boilEnd != null) {
			status.put("boilEnd", lFormat.format(boilEnd));
		}
		if(chillStart != null) {
			status.put("chillStart", lFormat.format(chillStart));
		}
		if(chillEnd != null) {
			status.put("chillEnd", lFormat.format(chillEnd));
		}
		

		
		return status;
	}
		

}
