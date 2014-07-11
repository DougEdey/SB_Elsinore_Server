package com.sb.elsinore;

import jGPIO.InvalidGPIOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sb.elsinore.NanoHTTPD.Response.Status;
import com.sb.elsinore.NanoHTTPD.Response;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.SequenceInputStream;

/**
 * A custom HTTP server for Elsinore.
 * Designed to be very simple and lightweight
 * @author Doug Edey
 *
 */
public class BrewServer extends NanoHTTPD {

    /**
     * The Root Directory of the files to be served.
     */
    private File rootDir;

    /**
     * The Logger object.
     */
    public static final Logger LOG = Logger.getLogger("com.sb.manager.Server");

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE.
     */
    private static final Map<String, String> MIME_TYPES
        = new HashMap<String, String>() {
        /**
         * The Serial UID.
         */
        private static final long serialVersionUID = 1L;

        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("txt", "text/plain");
            put("asc", "text/plain");
            put("gif", "image/gif");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("mp3", "audio/mpeg");
            put("m3u", "audio/mpeg-url");
            put("mp4", "video/mp4");
            put("ogv", "video/ogg");
            put("flv", "video/x-flv");
            put("mov", "video/quicktime");
            put("swf", "application/x-shockwave-flash");
            put("js", "application/javascript");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("ogg", "application/x-ogg");
            put("zip", "application/octet-stream");
            put("exe", "application/octet-stream");
            put("class", "application/octet-stream");
            put("json", "application/json");
        }
    };


    public Map<String, String> ParseParams(Map<String, String> params) {
        Set<Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param = null;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();
        String inputUnit = null;

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            BrewServer.LOG.info("Key: " + param.getKey());
            BrewServer.LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getValue());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                    inputUnit = param.getKey();
                }
            } catch (Exception e) {
                BrewServer.LOG.info("couldn't read " + param.getValue()
                    + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null) {
            // Use the JSON Data
            BrewServer.LOG.info("Found valid data for " + inputUnit);
            parms = (Map<String, String>) incomingData;
        } else {
            inputUnit = params.get("form");
            parms = params;
        }
        
        if( inputUnit != null )
        {
            parms.put("inputunit", inputUnit);
        }

        return parms;

    }
    /**
     * Constructor to create the HTTP Server.
     * @param port The port to run on
     * @throws IOException If there's an issue starting up
     */
    public BrewServer(final int port) throws IOException {

        super(port);
        // just serve up on port 8080 for now
        BrewServer.LOG.info("Launching on port " + port);

        //setup the logging handlers
        Handler[] lH = LOG.getHandlers();
        for (Handler h : lH) {
            h.setLevel(Level.INFO);
            LOG.info("Log handler: " + h.toString());
        }

        if (lH.length == 0) {
            LOG.addHandler(new ConsoleHandler());

            // default level, this can be changed
            try {
                LOG.info("Debug System property: "
                        + System.getProperty("debug"));
                if (System.getProperty("debug").equalsIgnoreCase("INFO")) {
                    LOG.setLevel(Level.INFO);
                    LOG.info("Enabled logging at an info level");
                }
            } catch (NullPointerException e) {
                LOG.setLevel(Level.WARNING);
            }
        }

        this.rootDir = new File(
            BrewServer.class.getProtectionDomain().getCodeSource()
            .getLocation().getPath()).getParentFile();
        LOG.info("Root Directory is: " + rootDir.toString());
        
        if (rootDir.exists() && rootDir.isDirectory()) {
            LOG.info("Root directory: " + rootDir.toString());
        }
    }

    /**
     * Parse the parameters to update the MashProfile.
     * @param parms The parameters map from the original request
     * @return True if the profile is updated successfully.
     */
    private boolean updateMashProfile(final Map<String, String> parms) {
        String inputUnit = null;
        Set<Entry<String, String>> incomingParams = parms.entrySet();
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param = null;
        JSONObject incomingData = new JSONObject();
        JSONParser parser = new JSONParser();

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            BrewServer.LOG.info("Key: " + param.getKey());
            BrewServer.LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getKey());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                }
            } catch (Exception e) {
                BrewServer.LOG.info("couldn't read " + param.getValue()
                    + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null) {
            // Use the JSON Data
            BrewServer.LOG.info("Found valid data for " + inputUnit);
            System.out.println(incomingData.toJSONString());

            MashControl mControl = null;

            // Default to Fahrenheit
            String tempUnit = "F";
            if (incomingData.containsKey("temp_unit")) {
                tempUnit = incomingData.get("temp_unit").toString();
                incomingData.remove("temp_unit");
            }

            String pid = null;

            if (incomingData.containsKey("pid")) {
                pid = incomingData.get("pid").toString();
                mControl = LaunchControl.findMashControl(pid);

                if (mControl == null) {
                    // Add a new MashControl to the list
                    mControl = new MashControl();
                    LaunchControl.addMashControl(mControl);
                }

                mControl.setOutputControl(pid);
                incomingData.remove("pid");
            } else {
                BrewServer.LOG.warning("Couldn't find the PID for this update");
                return false;
            }

            // Iterate through the JSON
            for (Object key: incomingData.keySet()) {
                String cKey = key.toString();

                try {
                    int stepCount = Integer.parseInt(cKey);
                    // We have a good step count, parse the value
                    JSONObject valueObj = (JSONObject) incomingData.get(key);

                    int duration = Integer.parseInt(
                            valueObj.get("duration").toString());
                    BigDecimal temp = new BigDecimal(
                            valueObj.get("temp").toString());

                    String method = valueObj.get("method").toString();
                    String type = valueObj.get("type").toString();

                    // Add the step
                    MashStep newStep = mControl.addMashStep(stepCount);
                    newStep.setDuration(duration);
                    newStep.setTemp(temp);
                    newStep.setMethod(method);
                    newStep.setType(type);
                    newStep.setTempUnit(tempUnit, false);

                    BrewServer.LOG.info(newStep.toString());
                } catch (NumberFormatException e) {
                    // Couldn't work out what this is
                    BrewServer.LOG.warning("Couldn't parse " + cKey);
                    BrewServer.LOG.warning(incomingData.get(key).toString());
                }
            }
            LaunchControl.startMashControl(pid);
        }
        return true;
    }

    /**
     * Toggle the state of the mash profile on/off.
     * @param parameters The parameters from the original request.
     * @return True if set, false if there's an error
     */
    private boolean toggleMashProfile(final Map<String, String> parameters) {

        String pid = null;
        if (parameters.containsKey("pid")) {
            pid = parameters.get("pid");
        } else {
            BrewServer.LOG.warning("No PID provided to toggle Mash Profile");
            return false;
        }

        boolean activate = false;
        if (parameters.containsKey("status")) {
            if (parameters.get("status").equalsIgnoreCase("activate")) {
                activate = true;
            }
        } else {
            BrewServer.LOG.warning("No Status provided to toggle Mash Profile");
            return false;
        }

        int position = -1;
        if (parameters.containsKey("position")) {
            try {
                position = Integer.parseInt(parameters.get("position"));
            } catch (NumberFormatException e) {
                BrewServer.LOG.warning(
                    "Couldn't parse positional argument: "
                    + parameters.get("position")
                );
                return false;
            }
        }

        MashControl mObj = LaunchControl.findMashControl(pid);

        if (mObj == null) {
            return false;
        }

        Entry<Integer, MashStep> mashEntry = mObj.getCurrentMashStep();

        // No active mash step
        int stepToUse = -1;
        if (mashEntry == null) {
            if (position >= 0) {
                stepToUse = position;
                BrewServer.LOG.warning(
                    "Using initial mash step for " + pid
                    + " at position " + position);
            } else {
                // No position wanted, activate the first step
                stepToUse = 0;
                BrewServer.LOG.warning("Using initial mash step for " + pid);
            }
        } else {
            // We have a mash step position
            if (position >= 0) {
                stepToUse = position;
                BrewServer.LOG.warning("Using mash step for " + pid
                    + " at position " + position);
            }

            if (!activate) {
                // We're de-activating everything
                stepToUse = -1;
            } else {
                BrewServer.LOG.warning("A mash is in progress for "
                    + pid + " but no positional argument is set");
                return false;
            }
        }

        if (stepToUse >= 0 && activate) {
            mObj.activateStep(stepToUse);
            BrewServer.LOG.warning("Activated "
                + pid + " step at " + stepToUse);
        } else {
            mObj.deactivateStep(stepToUse);
            BrewServer.LOG.warning("Deactivated "
                + pid + " step at " + stepToUse);
        }

        return true;
    }

    /**
     * Read the incoming parameters and edit the vessel as appropriate.
     * @param params The parameters from the client.
     * @return True is success, false if failure.
     */
    private Response editVessel(final Map<String, String> params) {
        String auxpin = "", newName = "", gpio = "";
        String inputUnit = "";

        Set<Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param = null;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();
        String error_msg = "No Changes Made";

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            BrewServer.LOG.info("Key: " + param.getKey());
            BrewServer.LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getValue());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                    inputUnit = param.getKey();
                }
            } catch (Exception e) {
                BrewServer.LOG.info("couldn't read " + param.getValue()
                    + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null) {
            // Use the JSON Data
            BrewServer.LOG.info("Found valid data for " + inputUnit);
            parms = (Map<String, String>) incomingData;
        } else {
            inputUnit = params.get("form");
            parms = params;
        }

        // Fall back to the old style
        if (parms.containsKey("new_name")) {
            newName = parms.get("new_name");
        }

        if (parms.containsKey("new_gpio")) {
            gpio = parms.get("new_gpio");
        }

        if (parms.containsKey("auxpin")) {
            auxpin = parms.get("auxpin");
        }

        if (inputUnit.equals("")) {
            BrewServer.LOG.warning("No Valid input unit");
            error_msg = "No Valid Input Unit";
        }

        Temp tProbe = LaunchControl.findTemp(inputUnit);
        PID tPID = LaunchControl.findPID(inputUnit);

        if (tProbe != null && !newName.equals("")) {
            tProbe.setName(newName);
            BrewServer.LOG.warning("Updated temp name " + newName);
        }

        if (tPID != null && !newName.equals("")) {
            tPID.getTemp().setName(newName);
            BrewServer.LOG.warning("Updated PID Name" + newName);
        }

        if (newName.equals("")) {
            newName = inputUnit;
        }

        if (!gpio.equals("")) {
            // The GPIO is Set.
            if (tPID == null) {
                // No PID already, create one.
                tPID = new PID(tProbe, newName, gpio);
                tPID.setAux(auxpin);
                LaunchControl.addPID(tPID);
                BrewServer.LOG.warning("Create PID");
                return new Response(Status.OK, MIME_TYPES.get("txt"),
                    "PID Added");
            } else if (!tPID.getGPIO().equals(gpio)) {
                // We have a PID, set it to the new value
                tPID.setGPIO(gpio);
                return new Response(Status.OK, MIME_TYPES.get("txt"),
                        "PID Updated");
            }
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add or update a Temperature probe to the system,"
            + " incoming object"
            + " should be a JSON Literal of \nold_name: {details}");
        usage.put("new_name", "The name of the Temperature Probe to add");
        usage.put("new_gpio", "The GPIO for the PID to work on");
        usage.put("aux_gpio", "The Auxilliary GPIO for the PID to work on");
        usage.put("Error", "Invalid parameters passed " + error_msg
            + " = " + params.toString());
        
        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
            usage.toJSONString());

    }

    /**
     * Add a new timer to the brewery.
     * @param params The parameter list
     * @return True if added ok
     */
    private Response addTimer(final Map<String, String> params) {
        String oldName = "", newName = "", gpio = "";
        String inputUnit = "";

        Set<Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param = null;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            BrewServer.LOG.info("Key: " + param.getKey());
            BrewServer.LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getValue());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                    inputUnit = param.getKey();
                }
            } catch (Exception e) {
                BrewServer.LOG.info("couldn't read " + param.getValue()
                    + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null) {
            // Use the JSON Data
            BrewServer.LOG.info("Found valid data for " + inputUnit);
            parms = (Map<String, String>) incomingData;
        } else {
            inputUnit = params.get("form");
            parms = params;
        }

        // Fall back to the old style
        if (parms.containsKey("new_name")) {
            newName = parms.get("new_name");
        }

        if (LaunchControl.addTimer(newName, "")) {
            return new Response(Status.OK, MIME_TYPES.get("txt"),
                "Timer Added");
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new pump to the system.");
        usage.put("new_name", "The name of the timer to add");
        usage.put("mode", "The mode for the timer, increment, or decrement (optional)");
        usage.put("Error", "Invalid parameters passed " + params.toString());

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
            usage.toJSONString());
    }
    
    /**
     * Add a new pump to the brewery.
     * @param params The parameter list
     * @return True if added ok
     */
    private Response addPump(final Map<String, String> params) {
        String newName = "", gpio = "";
        String inputUnit = "";

        Set<Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param = null;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            BrewServer.LOG.info("Key: " + param.getKey());
            BrewServer.LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getValue());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                    inputUnit = param.getKey();
                }
            } catch (Exception e) {
                BrewServer.LOG.info("couldn't read " + param.getValue()
                    + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null) {
            // Use the JSON Data
            BrewServer.LOG.info("Found valid data for " + inputUnit);
            parms = (Map<String, String>) incomingData;
        } else {
            inputUnit = params.get("form");
            parms = params;
        }

        // Fall back to the old style
        if (parms.containsKey("new_name")) {
            newName = parms.get("new_name");
        }

        if (parms.containsKey("new_gpio")) {
            gpio = parms.get("new_gpio");
        }

        if (LaunchControl.addPump(newName, gpio)) {
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Pump Added");
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new pump to the system");
        usage.put("new_name", "The name of the pump to add");
        usage.put("new_gpio", "The GPIO for the pump to work on");
        usage.put("Error", "Invalid parameters passed " + params.toString());
        
        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
            usage.toJSONString());
    }

    /**
     * Read the incoming parameters and update the PID as appropriate.
     * @param params The parameters from the client
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    private Response updatePID(final Map<String, String> params) {
        String temp, mode = "off";
        BigDecimal dTemp = new BigDecimal(0),
            duty = new BigDecimal(0),
            cycle = new BigDecimal(0),
            setpoint = new BigDecimal(0),
            p = new BigDecimal(0),
            i = new BigDecimal(0),
            d = new BigDecimal(0),
            min = new BigDecimal(0),
            max = new BigDecimal(0),
            time = new BigDecimal(0);

        JSONObject sub_usage = new JSONObject();
        Map<String, String> parms = ParseParams(params);
        String inputUnit = parms.get("inputunit");

        // Fall back to the old style
        sub_usage.put("dutycycle", "The new duty cycle % to set");
        if (parms.containsKey("dutycycle")) {
            temp = parms.get("dutycycle");
            try {
                dTemp = new BigDecimal(temp);
                duty = dTemp;
                BrewServer.LOG.info("Duty cycle: " + duty);
            } catch (NumberFormatException nfe) {
                System.out.print("Bad duty");
            }
        }

        sub_usage.put("dutycycle", "The new cycle time in seconds to set");
        if (parms.containsKey("cycletime")) {
            temp = parms.get("cycletime");
            try {
                dTemp = new BigDecimal(temp);
                cycle = dTemp;
                BrewServer.LOG.info("Cycle time: " + cycle);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad cycle");
            }
        }

        sub_usage.put("setpoint", "The new target temperature to set");
        if (parms.containsKey("setpoint")) {
            temp = parms.get("setpoint");
            try {
                dTemp = new BigDecimal(temp);
                setpoint = dTemp;
                BrewServer.LOG.info("Set Point: " + setpoint);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad setpoint");
            }
        }

        sub_usage.put("p", "The new proportional value to set");
        if (parms.containsKey("p")) {
            temp = parms.get("p");
            try {
                dTemp = new BigDecimal(temp);
                p = dTemp;
                BrewServer.LOG.info("P: " + p);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad p");
            }
        }

        sub_usage.put("p", "The new integral value to set");
        if (parms.containsKey("i")) {
            temp = parms.get("i");
            try {
                dTemp = new BigDecimal(temp);
                i = dTemp;
                BrewServer.LOG.info("I: " + i);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad i");
            }
        }

        sub_usage.put("p", "The new differential value to set");
        if (parms.containsKey("d")) {
            temp = parms.get("d");
            try {
                dTemp = new BigDecimal(temp);
                d =  dTemp;
                BrewServer.LOG.info("D: " + d);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad d");
            }
        }

        sub_usage.put("mode", "The new mode to set");
        if (parms.containsKey("mode")) {
            mode = parms.get("mode");
            BrewServer.LOG.info("Mode: " + mode);
        }

        sub_usage.put("min", "The minimum temperature to enable the output (HYSTERIA)");
        if (parms.containsKey("min")) {
            try {
                dTemp = new BigDecimal(parms.get("min"));
                min = dTemp;
                BrewServer.LOG.info("Min: " + mode);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad minimum");
            }
        }

        sub_usage.put("max", "The maximum temperature to disable the output (HYSTERIA)");
        if (parms.containsKey("max")) {
            try {
                dTemp = new BigDecimal(parms.get("max"));
                max = dTemp;
                BrewServer.LOG.info("Max: " + mode);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad maximum");
            }
        }

        sub_usage.put("time", "The minimum time when enabling the output (HYSTERIA)");
        if (parms.containsKey("time")) {
            try {
                dTemp = new BigDecimal(parms.get("time"));
                time = dTemp;
                BrewServer.LOG.info("Time: " + time);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.info("Bad time");
            }
        }

        BrewServer.LOG.info("Form: " + inputUnit);

        JSONObject usage = new JSONObject();
        usage.put(":PIDname", sub_usage);

        PID tPID = LaunchControl.findPID(inputUnit);
        if (tPID != null) {
            if (mode.equalsIgnoreCase("hysteria")) {
                tPID.setHysteria(min, max, time);
                tPID.useHysteria();
            } else {
                BrewServer.LOG.info(mode + ":" + duty + ":" + cycle + ":" + setpoint
                    + ":" + p + ":" + i + ":" + d);
                tPID.updateValues(mode, duty, cycle, setpoint, p, i, d);
            }
            return new Response(Status.OK,
                MIME_HTML, "PID " + inputUnit + " updated");
        } else {
            BrewServer.LOG.warning("Attempted to update a non existent PID, "
                    + inputUnit + ". Please check your client");
            usage.put("Error", "Non existent PID specified: " + inputUnit);
            return new Response(
                Status.BAD_REQUEST,
                MIME_TYPES.get("json"),
                usage.toJSONString());
        }

    }

    /**
     * The main method that checks the data coming into the server.
     * @param uri The URI requested
     * @param method The type of the request (GET/POST/DELETE)
     * @param header The header map from the request
     * @param parms The incoming Parameter map.
     * @param files A map of incoming files.
     * @return A NanoHTTPD Response Object
     */
    public final Response serve(final String uri, final Method method,
        final Map<String, String> header, final Map<String, String> parms,
        final Map<String, String> files) {

        BrewServer.LOG.info("URL : " + uri + " method: " + method);
        
        // parms contains the properties here
        if (uri.toLowerCase().equals("/mashprofile")) {
            if (updateMashProfile(parms)) {
                return new NanoHTTPD.Response(
                    Status.OK, MIME_HTML, "Updated MashProfile");
            }
            // TODO: Report Errors
            return new NanoHTTPD.Response(
                Status.BAD_REQUEST, MIME_HTML,
                "Failed to update Mashprofile");
        }

        if (uri.toLowerCase().equals("/togglemash")) {
            if (toggleMashProfile(parms)) {
                return new NanoHTTPD.Response(Status.OK,
                    MIME_HTML, "Toggled mash profile");
            }
            // TODO: Report Errors
            return new NanoHTTPD.Response(Status.BAD_REQUEST,
                MIME_HTML, "Failed to toggle MashProfile");
        }

        if (uri.toLowerCase().equals("/editdevice")) {
            return editVessel(parms);
        }
        if (uri.toLowerCase().equals("/updatepid")) {
            // parse the values if possible
            return updatePID(parms);
        }

        if (uri.toLowerCase().equals("/updateday")) {
            // we're storing the data for the brew day
            String tempDateStamp;
            BrewDay brewDay = LaunchControl.getBrewDay();

            // updated date
            if (parms.containsKey("updated")) {
                tempDateStamp = parms.get("updated");
                brewDay.setUpdated(tempDateStamp);
            } else {
                // we don't have an updated datestamp
                return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "No update datestamp, not updating a thang! YA HOSER!");
            }

            Iterator<Entry<String, String>> it =
                parms.entrySet().iterator();
            Entry<String, String> e = null;

            while (it.hasNext()) {
                e = it.next();

                if (e.getKey().endsWith("Start")) {
                    int trimEnd = e.getKey().length() - "Start".length();
                    String name = e.getKey().substring(0, trimEnd);
                    brewDay.startTimer(name, e.getValue());
                } else if (e.getKey().endsWith("End"))  {
                    int trimEnd = e.getKey().length() - "End".length();
                    String name = e.getKey().substring(0, trimEnd);
                    brewDay.stopTimer(name, e.getValue());
                }
            }

            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Updated Brewday");
        }

        if (uri.toLowerCase().equals("/updatepump")) {
            if (parms.containsKey("toggle")) {
                String pumpname = parms.get("toggle");
                Pump tempPump = LaunchControl.findPump(pumpname);
                if (tempPump != null) {
                    if (tempPump.getStatus()) {
                        tempPump.turnOff();
                    } else {
                        tempPump.turnOn();
                    }

                    return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                        "Updated Pump");
                } else {
                    JSONObject usage = new JSONObject();
                    usage.put("Error", "Invalid name supplied: " + pumpname);
                    usage.put("toggle", "The name of the Pump to toggle on/off");
                    return new Response(Status.BAD_REQUEST,
                        MIME_TYPES.get("txt"),
                        "Invalid pump: " + pumpname + " provided.");
                }
            }
        }

        if (uri.toLowerCase().equals("/toggleaux")) {
            if (parms.containsKey("toggle")) {
                String pidname = parms.get("toggle");
                PID tempPID = LaunchControl.findPID(pidname);
                if (tempPID != null) {
                    tempPID.toggleAux();
                    return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                        "Updated Aux for " + pidname);
                } else {
                    LOG.warning("Invalid PID: " + pidname + " provided.");
                    JSONObject usage = new JSONObject();
                    usage.put("Error", "Invalid name supplied: " + pidname);
                    usage.put("toggle", "The name of the PID to toggle the aux output for");
                    return new Response(usage.toJSONString());
                }
            }
        }

        if (uri.toLowerCase().equals("/getstatus")) {
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    LaunchControl.getJSONStatus());
        }

        if (uri.toLowerCase().equals("/controller")) {
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                LaunchControl.getControlPage());
        }

        if (uri.toLowerCase().equals("/timers")) {
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                LaunchControl.getBrewDay().brewDayStatus().toString());
        }
        
        if (uri.toLowerCase().equals("/graph")) {
            
            return serveFile("/templates/static/graph/graph.html", header, rootDir);
        }
        
        if (uri.toLowerCase().startsWith("/graph-data")) {
            return getGraphData(parms);
            
        }

        if (uri.toLowerCase().equals("/addpump")) {
            return addPump(parms);
        }

        if (uri.toLowerCase().equals("/addtimer")) {
            return addTimer(parms);
        }

        if (uri.toLowerCase().equals("/addvolpoint")) {
            return addVolumePoint(parms);
        }

        if (!uri.equals("") && new File(rootDir, uri).exists()) {
            return serveFile(uri, header, rootDir);
        }

        JSONObject usage = new JSONObject();
        usage.put("controller", "Get the main controller page");
        usage.put("getstatus", "Get the current status as a JSON object");
        usage.put("timers", "Get the current timer status");

        usage.put("addpump", "Add a new pump");
        usage.put("addtimer", "Add a new timer");
        usage.put("addvolpoint", "Add a new volume point");

        usage.put("toggleaux", "toggle an aux output");
        usage.put("mashprofile", "Set a mash profile for the output");
        usage.put("editdevice", "Edit the settings on a device");

        usage.put("updatepid", "Update the PID Settings");
        usage.put("updateday", "Update the brewday information");
        usage.put("updatepump", "Change the pump status off/on");

        BrewServer.LOG.info("Invalid URI: " + uri);
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
            usage.toJSONString());
    }

    /**
     * Serves file from homeDir and its' subdirectories (only).
     * Uses only URI, ignores all headers and HTTP parameters.
     * @param incomingUri The URI requested
     * @param header The headers coming in
     * @param homeDir The root directory.
     * @return A NanoHTTPD Response for the file
     */
    public Response serveFile(final String incomingUri,
            final Map<String, String> header,
            final File homeDir) {
        Response res = null;
        String uri = incomingUri;

        // Make sure we won't die of an exception later
        if (!homeDir.isDirectory()) {
            res = new Response(Response.Status.INTERNAL_ERROR,
                NanoHTTPD.MIME_PLAINTEXT,
                "INTERNAL ERRROR: serveFile(): "
                + "given homeDir is not a directory.");
        }

        if (res == null) {
            // Remove URL arguments
            uri = uri.trim().replace(File.separatorChar, '/');
            if (uri.indexOf('?') >= 0) {
                uri = uri.substring(0, uri.indexOf('?'));
            }

            // Prohibit getting out of current directory
            if (uri.startsWith("src/main")
                    || uri.endsWith("src/main") || uri.contains("../")) {
                res = new Response(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "FORBIDDEN: Won't serve ../ for security reasons.");
            }
        }

        File f = new File(homeDir, uri);
        if (res == null && !f.exists()) {
            res = new Response(Response.Status.NOT_FOUND,
                NanoHTTPD.MIME_PLAINTEXT,
                "Error 404, file not found.");
        }

        // List the directory, if necessary
        if (res == null && f.isDirectory()) {
            // Browsers get confused without '/' after the
            // directory, send a redirect.
            if (!uri.endsWith("/")) {
                uri += "/";
                res = new Response(Response.Status.REDIRECT,
                    NanoHTTPD.MIME_HTML,
                    "<html><body>Redirected: <a href=\"" + uri + "\">" + uri
                    + "</a></body></html>");
                res.addHeader("Location", uri);
            }

            if (res == null) {
                // First try index.html and index.htm
                if (new File(f, "index.html").exists()) {
                    f = new File(homeDir, uri + "/index.html");
                } else if (new File(f, "index.htm").exists()) {
                    f = new File(homeDir, uri + "/index.htm");
                    // No index file, list the directory if it is readable
                } else if (f.canRead()) {
                    String[] files = f.list();
                    String msg =
                        "<html><body><h1>Directory " + uri + "</h1><br/>";

                    if (uri.length() > 1) {
                        String u = uri.substring(0, uri.length() - 1);
                        int slash = u.lastIndexOf('/');
                        if (slash >= 0 && slash < u.length()) {
                            msg +=
                                "<b><a href=\"" + uri.substring(0, slash + 1)
                                + "\">..</a></b><br/>";
                        }
                    }

                    if (files != null) {
                        for (int i = 0; i < files.length; ++i) {
                            File curFile = new File(f, files[i]);
                            boolean dir = curFile.isDirectory();
                            if (dir) {
                                msg += "<b>";
                                files[i] += "/";
                            }

                            msg += "<a href=\"" + encodeUri(uri + files[i])
                                + "\">" + files[i] + "</a>";

                            // Show file size
                            if (curFile.isFile()) {
                                long len = curFile.length();
                                msg += " &nbsp;<font size=2>(";
                                if (len < 1024) {
                                    msg += len + " bytes";
                                } else if (len < 1024 * 1024) {
                                    msg += len / 1024 + "."
                                        + (len % 1024 / 10 % 100) + " KB";
                                } else {
                                    msg += len / (1024 * 1024) + "."
                                        + len % (1024 * 1024) / 10 % 100 + " MB";
                                }
                                msg += ")</font>";
                            }
                            msg += "<br/>";
                            if (dir) {
                                msg += "</b>";
                            }
                        }
                    }
                    msg += "</body></html>";
                    res = new Response(msg);
                } else {
                    res = new Response(Response.Status.FORBIDDEN,
                        NanoHTTPD.MIME_PLAINTEXT,
                        "FORBIDDEN: No directory listing.");
                }
            }
        }
        try {
            if (res == null) {
                // Get MIME type from file name extension, if possible
                String mime = null;
                int dot = f.getCanonicalPath().lastIndexOf('.');
                if (dot >= 0) {
                    mime = MIME_TYPES.get(
                        f.getCanonicalPath().substring(dot + 1).toLowerCase());
                }
                if (mime == null) {
                    mime = NanoHTTPD.MIME_HTML;
                }
                // Calculate etag
                String etag = Integer.toHexString(
                    (f.getAbsolutePath() + f.lastModified() + "" + f.length()
                    ).hashCode()
                );

                // Support (simple) skipping:
                long startFrom = 0;
                long endAt = -1;
                String range = header.get("range");
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length());
                        int minus = range.indexOf('-');
                        try {
                            if (minus > 0) {
                                startFrom = Long.parseLong(
                                    range.substring(0, minus));
                                endAt = Long.parseLong(
                                    range.substring(minus + 1));
                            }
                        } catch (NumberFormatException ignored) {
                            BrewServer.LOG.info(ignored.getMessage());
                        }
                    }
                }

                // Change return code and add Content-Range header
                // when skipping is requested
                long fileLen = f.length();
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = new Response(
                            Response.Status.RANGE_NOT_SATISFIABLE,
                            NanoHTTPD.MIME_PLAINTEXT, "");
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                        res.addHeader("ETag", etag);
                    } else {
                        if (endAt < 0) {
                            endAt = fileLen - 1;
                        }
                        long newLen = endAt - startFrom + 1;
                        if (newLen < 0) {
                            newLen = 0;
                        }

                        final long dataLen = newLen;
                        FileInputStream fis = new FileInputStream(f) {
                            @Override
                            public int available() throws IOException {
                                return (int) dataLen;
                            }
                        };
                        fis.skip(startFrom);

                        res = new Response(Response.Status.PARTIAL_CONTENT,
                            mime, fis);
                        res.addHeader("Content-Length", "" + dataLen);
                        res.addHeader("Content-Range", "bytes " + startFrom
                            + "-" + endAt + "/" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                } else {
                    if (etag.equals(header.get("if-none-match"))) {
                        res = new Response(Response.Status.NOT_MODIFIED,
                            mime, "");
                    } else {
                        res = new Response(Response.Status.OK,
                            mime, new FileInputStream(f));
                        res.addHeader("Content-Length", "" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                }
            }
        } catch (IOException ioe) {
            res = new Response(Response.Status.FORBIDDEN,
                NanoHTTPD.MIME_PLAINTEXT,
                "FORBIDDEN: Reading file failed.");
        }

        res.addHeader("Accept-Ranges", "bytes");
        // Announce that the file server accepts partial content requestes
        return res;
    }

    /**
     * URL-encodes everything between "/"-characters.
     * Encodes spaces as '%20' instead of '+'.
     * @param uri The URI to be encoded
     * @return The Encoded URI
     */
    private String encodeUri(final String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/")) {
                newUri += "/";
            } else if (tok.equals(" ")) {
                newUri += "%20";
            } else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                    BrewServer.LOG.info(ignored.getMessage());
                }
            }
        }
        return newUri;
    }

    /**
     * Add a new data point for the volume reading.
     * @param params List of parameters, must include "name" and "volume".
     *  Optional: probe details for onewire or direct ADC
     * @return A NanoHTTPD response
     */
    private Response addVolumePoint(Map<String, String> params) {
        JSONObject usage = new JSONObject();
        Map<String, String> parms = ParseParams(params);
        String inputUnit = parms.get("inputunit");
        usage.put("name", "Temperature probe name to add a volume point to");
        usage.put("volume",
                "Volume that is in the vessel to add a datapoint for");
        usage.put("units",
                "The Volume units to use (only required when setting up)");
        usage.put("onewire_address",
                "The one wire address to be used for analogue reads");
        usage.put("onewire_offset",
                "The one wire offset to be used for analogue reads");
        usage.put("adc_pin",
                "The ADC Pin to be used for analogue reads");

        System.out.println(params);
        String name = parms.get("name").trim();
        String volume = parms.get("volume").trim();
        String units = parms.get("units").trim();

        String onewire_add = parms.get("onewire_address").trim();
        String onewire_offset = parms.get("onewire_offset").trim();
        String adc_pin = parms.get("adc_pin").trim();

        String error_msg = "";

        if (name == null || volume == null) {
            error_msg = "No name or volume supplied";
            usage.put("Error", "Invalid parameters: " + error_msg);
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }

        // We have a name and volume, lookup the temp probe
        Temp t = LaunchControl.findTemp(name);
        if (t == null) {
            error_msg = "Invalid temperature probe name supplied ("
                + name + ")";
            usage.put("Error", "Invalid parameters: " + error_msg);
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }
        // We have a temp probe, check to see if there's a valid volume input
        if (!t.hasVolume()) {
            if (onewire_add == null && onewire_offset == null
                    && adc_pin == null) {
                error_msg =
                  "No volume input setup, and no valid volume inputs provided";
                usage.put("Error", "Invalid parameters: " + error_msg);
                return new Response(Response.Status.BAD_REQUEST,
                        MIME_TYPES.get("json"), usage.toJSONString());
            }

            // Check for ADC Pin
            if (adc_pin != null && !adc_pin.equals("")) {
                int adcpin = Integer.parseInt(adc_pin);
                if (0 < adcpin  || adcpin > 7) {
                    error_msg = "Invalid ADC Pin offset";
                    usage.put("Error", "Invalid parameters: " + error_msg);
                    return new Response(Response.Status.BAD_REQUEST,
                            MIME_TYPES.get("json"), usage.toJSONString());
                }
                try {
                    if (!t.setupVolumes(adcpin, units)) {
                        error_msg = "Could not setup volumes for " + adcpin
                            + " Units: " + units;
                        usage.put("Error", "Invalid parameters: " + error_msg);
                        return new Response(Response.Status.BAD_REQUEST,
                            MIME_TYPES.get("json"), usage.toJSONString());
                    }
                } catch (InvalidGPIOException g) {
                    error_msg = "Invalid GPIO Pin " + g.getMessage();
                        usage.put("Error", "Invalid parameters: " + error_msg);
                        return new Response(Response.Status.BAD_REQUEST,
                            MIME_TYPES.get("json"), usage.toJSONString());
                }

            } else if (onewire_add != null && onewire_offset != null
                && !onewire_add.equals("") && !onewire_offset.equals("")) {
                // Setup Onewire pin
                if (!t.setupVolumes(onewire_add, onewire_offset, units)) {
                    error_msg = "Could not setup volumes for " + onewire_add
                            + " offset: " + onewire_offset + " Units: " + units;
                    usage.put("Error", "Invalid parameters: " + error_msg);
                    return new Response(Response.Status.BAD_REQUEST,
                            MIME_TYPES.get("json"), usage.toJSONString());
                }
            }
        }

        // Should be good to go now
        try {
            BigDecimal actualVol = new BigDecimal(volume);
            if (t.addVolumeMeasurement(actualVol)) {
                return new Response(Response.Status.OK,
                    MIME_TYPES.get("json"), "{'Result': 'OK'}");
            }
        } catch (NumberFormatException nfe) {
            error_msg = "Could not setup volumes for " + volume
                + " Units: " + units;
            usage.put("Error", "Invalid parameters: " + error_msg);
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }

        return new Response(Response.Status.BAD_REQUEST,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    /**
     * Get the graph data 
     * @param params A list of specific parameters
     * @return the JSON Response data
     */
    public NanoHTTPD.Response getGraphData(Map<String, String> params) {
        Map<String, String> parms = ParseParams(params);
        // Have we been asked for a size?
        int size = 1000;

        if (parms.containsKey("size")) {
            try {
                size = Integer.parseInt((String) params.get("size"));
            } catch (NumberFormatException nfe) {
                size = 1000;
            }
        }

        String vessel = "";
        if (parms.containsKey("vessel")) {
            vessel = (String) parms.get("vessel");
        }

      //Read CSV files and make a JSON Response

        //Assume live for now until we can build a UI to deal with this
        boolean live = true;

        //If Live - read newest set
        String directory = null;
        if (live) {

            long newest = 0;
            File file = new File("graph-data/");
            if (file.isDirectory()) {
                File[] contents = file.listFiles();
                for (File content : contents) {
                    String name = content.getName();
                    try {
                        long current = Long.parseLong(name);
                        if (current > newest) {
                            newest = current;
                        }
                    } catch (NumberFormatException nfe) {
                        //Skip
                    }
                }
            }
            directory = String.valueOf(newest);
        }

        //Else Read based on time


        //Get files from directory
        File directoryFile = new File("graph-data/" + directory);

        File[] contents = directoryFile.listFiles();
        StringBuilder json = new StringBuilder("[");

        for (File content : contents) {
            if (content.getName().endsWith(".csv")
                    && content.getName().toLowerCase().startsWith(
                            vessel.toLowerCase())) {
                if (json.length() > 1) {
                    json.append(',');
                }
                json.append("{\"label\":\"");

                String name = content.getName();

                //Strip off .csv
                name = name.substring(0, name.length() - 4);
                name = name.replace('-', ' ');
                json.append(name + "\"");

                if (name.contains(" duty")) {
                    json.append(", \"yaxis\": 2");
                } else {
                    json.append(", \"yaxis\": 1");
                }

                json.append(",\"data\":[");

                String lastLine = null;
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(content));
                    String line;

                    List<String> textArray = new ArrayList<String>();

                    while ((line = reader.readLine()) != null) {

                        textArray.add('[' + line + ']');
                        lastLine = line;
                    }

                    if (size > 0 && textArray.size() > size) {
                        textArray = textArray.subList(
                                textArray.size() - size, textArray.size());
                    }

                    StringBuffer sb = new StringBuffer();
                    String s = null;
                    for (int i = 0; i < textArray.size(); i++) {
                        s = textArray.get(i);
                        if (!s.matches(" *")) {
                            //empty string are ""; " "; "  "; and so on
                            sb.append(s);
                            if (i < textArray.size() - 1) {
                                sb.append(",");
                            }
                        }
                    }
                    json.append(sb.toString());

                } catch (Exception e) {
                    // Do nothing
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {

                        }
                    }
                }

                if (live && lastLine != null) {
                    // Repeat the last point since the series only prints data
                    // when it changes.  Otherwise this won't look 'live'
                    json.append(",[" + System.currentTimeMillis() + ","
                        + lastLine.split(",")[1] + "]");
                }

                json.append("]}");
            }

        }
        json.append(']');

        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
            json.toString());
    }
}
