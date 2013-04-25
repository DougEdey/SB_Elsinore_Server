import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.json.JSONException;

public final class BrewDay {


	// generate the date time parameters
	DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
	DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");
	DateFormat lFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// the actual dates
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
			System.out.println("Error: " + dateString + " could not be parsed as a long, trying date");
			try{
				dDate = lFormat.parse(dateString);
			} catch (ParseException p) {
				System.out.println("Could not parse date, giving up ya hoser");
			}
		}
		
		// check to see if we're null
		if(dDate == null) {
			System.out.println("Unparseable string provided " + dateString);
		}
		return dDate;
	}


	// start
	void setStart(Date startIn) {
		startDay = startIn;
	}

	void setStart(String startIn) {
		setStart(parseDateString(startIn));
	}

	// mash in
	void setMashIn(Date mashInIn) {
		mashIn = mashInIn;
	}

	void setMashIn(String mashInIn) {
		setMashIn(parseDateString(mashInIn));
	}

	// mash out
	void setMashOut(Date mashOutIn) {
		mashOut = mashOutIn;
	}

	void setMashOut(String mashOutIn) {
		setMashOut(parseDateString(mashOutIn));
	}

	// sparge start
	void setSpargeStart(Date spargeStartIn) {
		spargeStart = spargeStartIn;
	}

	void setSpargeStart(String spargeStartIn) {
		setSpargeStart(parseDateString(spargeStartIn));
	}

	// sparge end
	void setSpargeEnd(Date spargeEndIn) {
		spargeEnd = spargeEndIn;
	}

	void setSpargeEnd(String spargeEndIn) {
		setSpargeEnd(parseDateString(spargeEndIn));
	}

	// boil start
	void setBoilStart(Date boilStartIn) {
		spargeEnd = boilStartIn;
	}

	void setBoilStart(String boilStartIn) {
		setBoilStart(parseDateString(boilStartIn));
	}

	// chill start
	void setChillStart(Date chillStartIn) {
		chillStart = chillStartIn;
	}

	void setChillStart(String chillStartIn) {
		setChillStart(parseDateString(chillStartIn));
	}

	// chill stop
	void setChillEnd(Date chillEndIn) {
		chillEnd = chillEndIn;
	}

	void setChillEnd(String chillEndIn) {
		setChillEnd(parseDateString(chillEndIn));
	}

	// get the JSON
	JSONObject brewDayStatus() {
		JSONObject status = new JSONObject();
		try {
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
		} catch (JSONException e) {
			System.out.println("Couldn't generate Date Status");
			e.printStackTrace();

		}

		
		return status;
	}
		

}
