package com.sb.elsinore;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;

/**
 * A class to hold the information about the brew day for the timers.
 * @author Doug Edey
 *
 */
public final class BrewDay {

    /**
     * The Map of timers for the dates.
     */
    private Map<String, Object> timers =
            new ConcurrentHashMap<String, Object>();

    // generate the date time parameters
    /**
     * The Day format.
     */
    private DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * The Time format.
     */
    private DateFormat sFormat = new SimpleDateFormat("HH:mm:ss");
    /**
     * The Zulu Date & Time format.
     */
    private DateFormat lFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    /**
     * The Date this BrewDay was last updated.
     */
    private Date updated = null;

    /**
     * parse a string to a date object.
     * @param dateString The string to be parsed
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
            BrewServer.log.log(Level.INFO, "Error: " + dateString
                + " could not be parsed as a long, trying date", e);
            try {
                dDate = lFormat.parse(dateString);
            } catch (ParseException p) {
                BrewServer.log.info("Could not parse date, giving up ya hoser");
            }
        }

        // check to see if we're null
        if (dDate == null) {
            BrewServer.log.info("Unparseable string provided " + dateString);
        }
        return dDate;
    }


    /**
     * Get the Time for the selected name.
     * @param name The timer name to look for
     * @return The entry: name, date
     */
    @SuppressWarnings("unchecked")
    public Entry<String, Date> getTimer(final String name) {

        return (Entry<String, Date>) timers.get(name);
    }

    /**
     * Get the selected timer date as a string.
     * @param name The timer to get.
     * @return The date as a string
     */
    public String getTimerString(final String name) {
        return lFormat.format(timers.get(name));
    }

    /**
     * Set the timer at name to the specified entry.
     * @param name The timer to reset.
     * @param timerData And entry with the timer data.
     */
    @SuppressWarnings("unchecked")
    public void setTimer(final String name,
            final Entry<String, Date> timerData) {
        HashMap<String, Date> timerElements =
                (HashMap<String, Date>) timers.get(name);

        if (timerElements == null) {
            timerElements = new HashMap<String, Date>();
        }

        timerElements.put(timerData.getKey(), timerData.getValue());
        timers.put(name, timerElements);
    }

    /**
     * Set the timer to the specified date string.
     * @param name The timer to set
     * @param startIn The start time string to set.
     */
    public void startTimer(final String name, final String startIn) {
        Entry<String, Date> startEntry =
                new AbstractMap.SimpleEntry<String, Date>(
                        "start", parseDateString(startIn));
        setTimer(name, startEntry);
    }

    /**
     * Set the end of the timer to the specified stop time.
     * @param name The name of the timer to stop.
     * @param stopIn The stop time as a string
     */
    public void stopTimer(final String name, final String stopIn) {
        Entry<String, Date> stopEntry =
                new AbstractMap.SimpleEntry<String, Date>(
                        "end", parseDateString(stopIn));
        setTimer(name, stopEntry);
    }

    /**
     * Get the last date this brewday was updated.
     * @return The date object that this brewday was last updated
     */
    public Date getUpdate() {
        return this.updated;
    }

    /**
     * Get the date this brewday was updated as a long format string.
     * @return The string that represents when this brewday was last updated
     */
    public String getUpdatedString() {
        return lFormat.format(this.updated);
    }

    /**
     * Set the updated date of this brewday.
     * @param updatedIn Date to set the updated parameter of this brewday
     */
    public void setUpdated(final Date updatedIn) {
        this.updated = updatedIn;
    }

    /**
     * Set the update date of this brewday.
     * @param updatedIn String representing the updated date
     */
    public void setUpdated(final String updatedIn) {
        setUpdated(parseDateString(updatedIn));
    }

    /**
     * Create a new timer.
     * @param name The name of the new timer.
     */
    public void addTimer(final String name) {

        if (timers.containsKey(name)) {
            BrewServer.log.info("Timer: " + name + " exists");
            return;
        }
        timers.put(name, null);
        BrewServer.log.info("Added new timer: " + name);
    }

    /**
     * Get the Status as a JSON Object.
     * @return A JSONObject representing the status
     */
    @SuppressWarnings("unchecked")
    public JSONObject brewDayStatus() {
        JSONObject status = new JSONObject();

        Iterator<Entry<String, Object>> it = timers.entrySet().iterator();

        Entry<String, Object> e = null;

        while (it.hasNext()) {
            e = it.next();
            if (e.getValue() != null) {

                // iterate the child hashmap
                HashMap<String, Date> valueEntry =
                        (HashMap<String, Date>) e.getValue();
                Iterator<Entry<String, Date>> dateIt =
                        valueEntry.entrySet().iterator();

                // get the Timer Name Object
                JSONObject timerJSON = (JSONObject) status.get(e.getKey());

                // Add one if there's not an object
                if (timerJSON == null) {
                    timerJSON = new JSONObject();
                }

                while (dateIt.hasNext()) {
                    Entry<String, Date> dateEntry = dateIt.next();
                    if (dateEntry.getValue() != null) {
                        timerJSON.put(dateEntry.getKey(),
                                lFormat.format(dateEntry.getValue()));
                    }
                }

                status.put(e.getKey(), timerJSON);
            }
        }

        return status;
    }
}
