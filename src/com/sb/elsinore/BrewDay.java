package com.sb.elsinore;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;

public final class BrewDay {


	// generate the date time parameters
	DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
	DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");
	DateFormat lFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// the actual dates
	Date updated = null;
	Date startDay = null;
	Date mashIn = null;
	Date mashOut = null;
	Date spargeStart = null;
	Date spargeEnd = null;
	Date boilStart = null;
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
			BrewServer.log.info("Error: " + dateString + " could not be parsed as a long, trying date");
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
	Date getStart() {
		return startDay;
	}

	String getStartString() {
		return lFormat.format(startDay);
	}

	void setStart(Date startIn) {
		startDay = startIn;
	}

	void setStart(String startIn) {
		setStart(parseDateString(startIn));
	}

	// mash in
	Date getMashIn() {
		return mashIn;
	}

	String getMashInString() {
		return lFormat.format(mashIn);
	}

	void setMashIn(Date mashInIn) {
		mashIn = mashInIn;
	}

	void setMashIn(String mashInIn) {
		setMashIn(parseDateString(mashInIn));
	}

	// mash out
	Date getMashOut() {
		return mashOut;
	}

	String getMashOutString() {
		return lFormat.format(mashOut);
	}

	void setMashOut(Date mashOutIn) {
		mashOut = mashOutIn;
	}

	void setMashOut(String mashOutIn) {
		setMashOut(parseDateString(mashOutIn));
	}

	// sparge start
	Date getSpargeStart() {
		return spargeStart;
	}

	String getSpargeStartString() {
		return lFormat.format(spargeStart);
	}

	void setSpargeStart(Date spargeStartIn) {
		spargeStart = spargeStartIn;
	}

	void setSpargeStart(String spargeStartIn) {
		setSpargeStart(parseDateString(spargeStartIn));
	}

	// sparge end
	Date getSpargeEnd() {
		return spargeEnd;
	}

	String getSpargeEndString() {
		return lFormat.format(spargeEnd);
	}

	void setSpargeEnd(Date spargeEndIn) {
		spargeEnd = spargeEndIn;
	}

	void setSpargeEnd(String spargeEndIn) {
		setSpargeEnd(parseDateString(spargeEndIn));
	}

	// boil start
	Date getBoilStart() {
		return boilStart;
	}

	String getBoilStartString() {
		return lFormat.format(boilStart);
	}

	void setBoilStart(Date boilStartIn) {
		boilStart = boilStartIn;
	}

	void setBoilStart(String boilStartIn) {
		setBoilStart(parseDateString(boilStartIn));
	}

	// chill start
	Date getChillStart() {
		return chillStart;
	}

	String getChillStartString() {
		return lFormat.format(chillStart);
	}

	void setChillStart(Date chillStartIn) {
		chillStart = chillStartIn;
	}

	void setChillStart(String chillStartIn) {
		setChillStart(parseDateString(chillStartIn));
	}

	// chill stop
	Date getChillEnd() {
		return chillEnd;
	}

	String getChillEndString() {
		return lFormat.format(chillEnd);
	}

	void setChillEnd(Date chillEndIn) {
		chillEnd = chillEndIn;
	}

	void setChillEnd(String chillEndIn) {
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
	JSONObject brewDayStatus() {
		JSONObject status = new JSONObject();

		if(startDay != null) {
			status.put("startDay", lFormat.format(startDay));
		}
		if(mashIn != null) {
			status.put("mashIn", lFormat.format(mashIn));
		}
		if(mashOut != null) {
			status.put("mashOut", lFormat.format(mashOut));
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
		if(chillStart != null) {
			status.put("chillStart", lFormat.format(chillStart));
		}
		if(chillEnd != null) {
			status.put("chillEnd", lFormat.format(chillEnd));
		}
		

		
		return status;
	}
		

}
