package com.sb.elsinore;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A class to hold the information about the brew day for the timers.
 * 
 * @author Doug Edey
 * 
 */
public final class BrewDay {

    /**
     * The Map of timers for the dates.
     */
    private Map<String, Object> timers = new ConcurrentHashMap<String, Object>();

    // generate the date time parameters
    /**
     * The Day format.
     */
    private DateFormat dFormat = new SimpleDateFormat("yyyy/MM/dd");
    /**
     * The Time format.
     */
    private DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");
    /**
     * The Zulu Date & Time format.
     */
    private DateFormat lFormat = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ssZ");

    /**
     * The Date this BrewDay was last updated.
     */
    private Date updated = null;

    /**
     * parse a string to a date object.
     * 
     * @param dateString
     *            The string to be parsed
     * @return a date object
     */
    public Date parseDateString(final String dateString) {
        Date dDate = null;

        if (dateString == null) {
            return dDate;
        }

        // this better be a datestamp string
        try {
            dDate = new Date(Long.parseLong(dateString));
        } catch (NumberFormatException e) {
            BrewServer.LOG.log(Level.INFO, "Error: " + dateString
                    + " could not be parsed as a long, trying date", e);
            try {
                dDate = lFormat.parse(dateString);
            } catch (ParseException p) {
                BrewServer.LOG.info("Could not parse date, giving up ya hoser");
            }
        }

        // check to see if we're null
        if (dDate == null) {
            BrewServer.LOG.info("Unparseable string provided " + dateString);
        }
        return dDate;
    }

    /**
     * Get the Time for the selected name.
     * 
     * @param name
     *            The timer name to look for
     * @return The entry: name, date
     */
    @SuppressWarnings("unchecked")
    public Entry<String, Date> getTimer(final String name) {

        return (Entry<String, Date>) timers.get(name);
    }

    /**
     * Get the selected timer date as a string.
     * 
     * @param name
     *            The timer to get.
     * @return The date as a string
     */
    public String getTimerString(final String name) {
        return lFormat.format(timers.get(name));
    }

    /**
     * Set the timer at name to the specified entry.
     * 
     * @param name
     *            The timer to reset.
     * @param timerData
     *            And entry with the timer data.
     */
    @SuppressWarnings("unchecked")
    public void setTimer(final String name, final Entry<String, Date> timerData) {
        HashMap<String, Date> timerElements = (HashMap<String, Date>) timers
                .get(name);

        if (timerElements == null) {
            timerElements = new HashMap<String, Date>();
        }
        
        timerElements.put(timerData.getKey(), timerData.getValue());
        timers.put(name, timerElements);
    }

    /**
     * Set the timer to the specified date string.
     * 
     * @param name
     *            The timer to set
     * @param startIn
     *            The start time string to set.
     */
    public void startTimer(final String name, final String startIn) {
        Date startDate = null;
        try {
            Long startTime = Long.parseLong(startIn);
            // If it's 0 we start now. If it's anything else it's a count down.
            startDate = new Date();
            if (startTime > 0) {
                startDate = new Date(startDate.getTime() + (1000 * startTime));
            }
        } catch (NumberFormatException nfe) {
            startDate = parseDateString(startIn);
        }

        Entry<String, Date> startEntry =
                new AbstractMap.SimpleEntry<String, Date>("start", startDate);
        setTimer(name, startEntry);
    }

    /**
     * Set the end of the timer to the specified stop time.
     *
     * @param name
     *            The name of the timer to stop.
     * @param stopIn
     *            The stop time as a string
     */
    public void stopTimer(final String name, final String stopIn) {
        HashMap<String, Date> valueEntry = (HashMap<String, Date>) timers.get(name);
        if (valueEntry == null) {
            LaunchControl.setMessage("Could not stop timer " + name);
            return;
        }
        Date endDate = valueEntry.get("end");
        if (endDate != null) {
            // Continue the timer
            continueTimer(name);
        } else {
            Entry<String, Date> stopEntry = new AbstractMap.SimpleEntry<String, Date>(
                    "end", parseDateString(stopIn));
            setTimer(name, stopEntry);
        }
    }

    public void continueTimer(final String name) {
        HashMap<String, Date> valueEntry = (HashMap<String, Date>) timers.get(name);
        Date startDate = valueEntry.get("start");
        Date endDate = valueEntry.get("end");

        Long current = 0L;
        if (endDate != null && startDate != null) {
            // get the current duration.
            current = endDate.getTime() - startDate.getTime();
        }

        startDate = new Date(Calendar.getInstance().getTimeInMillis() - current);

        Entry<String, Date> stopEntry = new AbstractMap.SimpleEntry<String, Date>(
                "end", null);
        setTimer(name, stopEntry);
        Entry<String, Date> startEntry = new AbstractMap.SimpleEntry<String, Date>(
                "start", startDate);
        setTimer(name, startEntry);
    }
    
    public void resetTimer(String name) {
        Entry<String, Date> startEntry = new AbstractMap.SimpleEntry<String, Date>(
                "start", null);
        Entry<String, Date> stopEntry = new AbstractMap.SimpleEntry<String, Date>(
                "end", null);
        setTimer(name, startEntry);
        setTimer(name, stopEntry);
    }

    /**
     * Get the last date this brewday was updated.
     *
     * @return The date object that this brewday was last updated
     */
    public Date getUpdate() {
        return this.updated;
    }

    /**
     * Get the date this brewday was updated as a long format string.
     *
     * @return The string that represents when this brewday was last updated
     */
    public String getUpdatedString() {
        return lFormat.format(this.updated);
    }

    /**
     * Set the updated date of this brewday.
     * 
     * @param updatedIn
     *            Date to set the updated parameter of this brewday
     */
    public void setUpdated(final Date updatedIn) {
        this.updated = updatedIn;
    }

    /**
     * Set the update date of this brewday.
     * 
     * @param updatedIn
     *            String representing the updated date
     */
    public void setUpdated(final String updatedIn) {
        setUpdated(parseDateString(updatedIn));
    }

    /**
     * Create a new timer.
     * 
     * @param name
     *            The name of the new timer.
     */
    public void addTimer(final String name) {

        if (timers.containsKey(name)) {
            BrewServer.LOG.info("Timer: " + name + " exists");
            return;
        }
        timers.put(name, null);
        BrewServer.LOG.info("Added new timer: " + name);
    }

    /**
     * Get the Status as a JSON Object.
     * 
     * @return A JSONObject representing the status
     */
    @SuppressWarnings("unchecked")
    public JSONArray brewDayStatus() {
        JSONArray status = new JSONArray();

        Iterator<Entry<String, Object>> it = timers.entrySet().iterator();

        Entry<String, Object> e = null;
        Date currentDate = Calendar.getInstance().getTime();

        while (it.hasNext()) {
            e = it.next();
            if (e.getValue() != null) {

                // iterate the child hash map
                HashMap<String, Date> valueEntry = (HashMap<String, Date>) e
                        .getValue();
                Iterator<Entry<String, Date>> dateIt = valueEntry.entrySet()
                        .iterator();

                // get the Timer Name Object
                JSONObject timerJSON = new JSONObject();
                timerJSON.put("name", e.getKey());

                Date startDate = valueEntry.get("start");
                Date endDate = valueEntry.get("end");
                String mode = "none";
                Long seconds = 0L;

                if (startDate != null && endDate == null) {
                    // Are we counting up?
                    if (startDate.getTime() < currentDate.getTime()) {
                        seconds = (currentDate.getTime() - startDate.getTime()) / 1000;
                        mode = "up";
                    } else {
                        seconds = (startDate.getTime() - currentDate.getTime()) / 1000;
                        mode = "down";
                    }
                } else if (startDate != null && endDate != null) {
                    // Timer has stopped.
                    seconds = (endDate.getTime() - startDate.getTime()) / 1000;
                    mode = "stopped";
                }

                timerJSON.put(mode, seconds);

                status.add(timerJSON);
            }
        }

        return status;
    }

    /**
     * Return the current state of this Brewday as a JSON string.
     * 
     * @return The String representing the current timers.
     */
    public String getJSONDataString() {
        StringWriter out = new StringWriter();
        try {
            brewDayStatus().writeJSONString(out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

}
