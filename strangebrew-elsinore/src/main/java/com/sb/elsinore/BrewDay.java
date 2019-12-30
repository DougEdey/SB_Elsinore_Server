package com.sb.elsinore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 * A class to hold the information about the brew day for the timers.
 * 
 * @author Doug Edey
 * 
 */
@SuppressWarnings("unchecked")
public final class BrewDay {

    /**
     * The Zulu Date & Time format.
     */
    public static DateFormat lFormat = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ssZ");
    public static DateFormat mFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ssZ");
    public static DateFormat readableFormat = new SimpleDateFormat("dd/MM/yyyy'<br/>'HH:mm");

}
