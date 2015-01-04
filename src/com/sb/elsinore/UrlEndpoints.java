package com.sb.elsinore;

import jGPIO.InvalidGPIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.tools.PrettyWriter;

import com.sb.elsinore.NanoHTTPD.Response;
import com.sb.elsinore.NanoHTTPD.Response.Status;
import com.sb.elsinore.annotations.UrlEndpoint;
import com.sb.elsinore.html.PhSensorForm;
import com.sb.elsinore.html.RenderHTML;
import com.sb.elsinore.html.VolumeEditForm;
import com.sb.elsinore.inputs.PhSensor;
import com.sb.elsinore.triggers.TriggerInterface;

public class UrlEndpoints {

    public File rootDir;
    public Map<String, String> parameters = null;
    public Map<String, String> files = null;
    public Map<String, String> header = null;
    public static final Map<String, String> MIME_TYPES
        = new HashMap<String, String>() {
        /**
         * The Serial UID.
         */
        public static final long serialVersionUID = 1L;
    
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
    public static final String MIME_HTML = "text/html";
    
    public Map<String, String> getParameters() {
        return this.parameters;
    }
    
    public Map<String, String> ParseParams() {
        return this.ParseParams(this.getParameters());
    }
    /**
     * Convert a JSON parameter set into a hashmap.
     * @param params The incoming parameter listing
     * @return The converted parameters
     */
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

        if (inputUnit != null) {
            parms.put("inputunit", inputUnit);
        }

        return parms;
    }

    /**
     * Parse the parameters to update the MashProfile.
     *
     * @param parms
     *            The parameters map from the original request
     * @return True if the profile is updated successfully.
     */
    @UrlEndpoint(url = "/mashprofile")
    public Response updateMashProfile() {
        String inputUnit = null;
        Set<Entry<String, String>> incomingParams = parameters.entrySet();
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
            BrewServer.LOG.info(incomingData.toJSONString());

            TriggerControl triggerControl = null;

            String temp = null;

            if (incomingData.containsKey("temp")) {
                temp = incomingData.get("temp").toString();
                triggerControl = LaunchControl.findTriggerControl(temp);

                if (triggerControl == null) {
                    // Add a new MashControl to the list
                    triggerControl = new TriggerControl();
                    LaunchControl.addMashControl(triggerControl);
                    triggerControl.setOutputControl(temp);
                }

                incomingData.remove("pid");
            } else {
                BrewServer.LOG.warning("Couldn't find the PID for this update");

                return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                        "Failed to update Mash Profile");
            }

            // Iterate through the JSON
            for (Object key : incomingData.keySet()) {
                String cKey = key.toString();

                try {

                    JSONObject valueObj = (JSONObject) incomingData.get(key);
                    int stepCount = -1;
                    try {
                        stepCount = Integer.parseInt(cKey);
                    } catch (NumberFormatException nfe) {
                        stepCount = (Integer) valueObj.get("position");
                    }

                    if (stepCount < 0) {
                        continue;
                    }

                    // We have a good step count, parse the value
                    String triggerType = (String) valueObj.get("Trigger");

                    // Add the trigger
                    TriggerInterface newTrigger = triggerControl.addTrigger(
                            stepCount, triggerType, valueObj);

                    BrewServer.LOG.info(newTrigger.toString());
                } catch (NumberFormatException e) {
                    // Couldn't work out what this is
                    BrewServer.LOG.warning("Couldn't parse " + cKey);
                    BrewServer.LOG.warning(incomingData.get(key).toString());
                    e.printStackTrace();
                }
            }
            triggerControl.sortMashSteps();
            LaunchControl.startMashControl(temp);
        }

        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "Updated MashProfile");
    }

    /**
     * Add a mash step (and mash control if needed) to a PID.
     * @param params The incoming parameters.
     * @return The NanoHTTPD response to return to the browser.
     */
    @UrlEndpoint(url = "/addmashstep")
    public Response addMashStep() {
        // Parse the response
        // Temp unit, PID, duration, temp, method, type, step number
        // Default to the existing temperature scale
        Map<String, String> params = this.parameters;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new mashstep to the specified PID");
        usage.put("type", "The mash step type");
        usage.put("position", "The step number");
        params = ParseParams(params);

        Status status = Response.Status.OK;
        String pid = params.get("temp");
        String type = params.get("type");

        try {
            BigDecimal duration = new BigDecimal(
                    params.get("duration").replace(",", "."));
            BigDecimal temp = new BigDecimal(
                    params.get("temp").replace(",", "."));
            int stepNumber = Integer.parseInt(params.get("step"));

            // Double check for any issues.
            if (pid == null) {
                throw new IllegalStateException(
                        "Couldn't add Trigger: No temp probe provided");
            }
            if (type == null) {
                throw new IllegalStateException(
                        "Couldn't add Trigger: No type provided");
            }

            if (LaunchControl.findPID(pid) == null) {
                throw new IllegalStateException(
                        "Couldn't add mashstep: Couldn't find PID: " + pid);
            }

            TriggerControl mControl = LaunchControl.findTriggerControl(pid);

            if (mControl == null) {
                // Add a new MashControl to the list
                mControl = new TriggerControl();
                LaunchControl.addMashControl(mControl);
                mControl.setOutputControl(pid);
                LaunchControl.startMashControl(pid);
            }

            TriggerInterface newStep = mControl.addTrigger(
                    stepNumber, type, new JSONObject(parameters));
            mControl.sortMashSteps();
        } catch (NullPointerException nfe) {
            LaunchControl.setMessage(
                    "Couldn't add mashstep, problem parsing values: "
                        + params.toString() + nfe.getMessage());
            status = Response.Status.BAD_REQUEST;
        } catch (NumberFormatException nfe) {
            LaunchControl.setMessage(
                "Couldn't add mashstep, problem parsing values: "
                        + params.toString());
            status = Response.Status.BAD_REQUEST;
        } catch (IllegalStateException ise) {
            LaunchControl.setMessage(ise.getMessage());
            status = Response.Status.BAD_REQUEST;
        }

        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Reorder the mash steps.
     * @param params The incoming params, check the usage.
     * @return The Response object.
     */
    @UrlEndpoint(url = "/reordermashprofile")
    public Response reorderMashProfile() {
        Map<String, String> params = this.parameters;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the order for the mash profile.");
        usage.put("pid", "The name of the PID to change the mash profile order on.");
        usage.put(":old=:new", "The old position and the new position");
        // Resort the mash profile for the PID, then sort the rest
        String pid = params.get("pid");
        // Do we have a PID coming in?
        if (pid == null) {
            LaunchControl.setMessage("No PID supplied for mash profile order sort");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }
        // Prevent any errors from the PID not being a number.
        params.remove("pid");
        // Do we have a mash control for the PID?
        TriggerControl mControl = LaunchControl.findTriggerControl(pid);
        if (mControl == null) {
            LaunchControl.setMessage("No Mash specified for the PID supplied " + pid);
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }

        // Should be good to go, iterate and update!
        for (Map.Entry<String, String> mEntry: params.entrySet()) {
            if (mEntry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }
            try {
                int oldPos = Integer.parseInt(mEntry.getKey());
                int newPos = Integer.parseInt(mEntry.getValue());
                mControl.getTrigger(oldPos).setPosition(newPos);
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage("Failed to parse Mash reorder value, things may get weird" + mEntry.getKey() + ": " + mEntry.getValue());
            } catch (IndexOutOfBoundsException ie) {
                LaunchControl.setMessage("Invalid mash position, things may get weird");
            }
        }
        mControl.sortMashSteps();
        return new Response(Status.OK, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Delete the specified mash step.
     * @param params The details.
     * @return The Response.
     */
    @UrlEndpoint(url = "/delmashstep")
    public Response delMashStep() {
        Map<String, String> params = this.parameters;
        // Parse the response
        // Temp unit, PID, duration, temp, method, type, step number
        // Default to the existing temperature scale
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new mashstep to the specified PID");
        usage.put("pid", "The PID to delete the mash step from");
        usage.put("position", "The mash step to delete");
        Status status = Status.OK;

        try {
            String pid = params.get("pid");
            int position = Integer.parseInt(params.get("position"));
            TriggerControl mControl = LaunchControl.findTriggerControl(pid);

            if (mControl == null) {
                // Add a mash control
                if (LaunchControl.findPID(pid) == null) {
                    LaunchControl.setMessage(
                            "Could not find the specified PID: " + pid);
                } else {
                    mControl = new TriggerControl();
                    mControl.setOutputControl(pid);
                }
            }

            if (mControl != null) {
                mControl.delMashStep(position);
            } else {
                status = Status.BAD_REQUEST;
            }
        } catch (NumberFormatException nfe) {
            LaunchControl.setMessage(
                    "Couldn't parse the position to delete: " + params);
            status = Status.BAD_REQUEST;
        } catch (NullPointerException ne) {
            LaunchControl.setMessage(
                    "Couldn't parse the mash data to delete: " + params);
            status = Status.BAD_REQUEST;
        } catch (IndexOutOfBoundsException ie) {
            LaunchControl.setMessage(
                    "Invalid Mash step position to delete: " + ie.getMessage());
            status = Status.BAD_REQUEST;
        }

        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Toggle the state of the mash profile on/off.
     * @param parameters
     *            The parameters from the original request.
     * @return True if set, false if there's an error
     */
    @UrlEndpoint(url = "/togglemash")
    public Response toggleMashProfile() {

        String pid = null;
        if (parameters.containsKey("pid")) {
            pid = parameters.get("pid");
        } else {
            BrewServer.LOG.warning("No PID provided to toggle Mash Profile");
            return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                    "Failed to toggle mash profile");
        }

        boolean activate = false;
        if (parameters.containsKey("status")) {
            if (parameters.get("status").equalsIgnoreCase("activate")) {
                activate = true;
            }
        } else {
            BrewServer.LOG.warning("No Status provided to toggle Mash Profile");
            return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                    "Failed to toggle mash profile");
        }

        int position = -1;
        if (parameters.containsKey("position")) {
            try {
                position = Integer.parseInt(parameters.get("position"));
            } catch (NumberFormatException e) {
                BrewServer.LOG.warning("Couldn't parse positional argument: "
                        + parameters.get("position"));
                return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                        "Failed to toggle mash profile");
            }
        }

        TriggerControl mObj = LaunchControl.findTriggerControl(pid);

        if (mObj == null) {
            return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                    "Failed to toggle mash profile");
        }

        TriggerInterface triggerEntry = mObj.getCurrentTrigger();

        // No active mash step
        int stepToUse = -1;
        if (triggerEntry == null) {
            if (position >= 0) {
                stepToUse = position;
                BrewServer.LOG.warning("Using initial mash step for " + pid
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
                BrewServer.LOG.warning("A mash is in progress for " + pid
                        + " but no positional argument is set");
                return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                        "Failed to toggle mash profile");
            }
        }

        if (stepToUse >= 0 && activate) {
            mObj.activateTrigger(stepToUse);
            BrewServer.LOG
                    .warning("Activated " + pid + " step at " + stepToUse);
        } else {
            mObj.deactivateTrigger(stepToUse);
            BrewServer.LOG.warning("Deactivated " + pid + " step at "
                    + stepToUse);
        }


        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Toggled mash profile");
    }

    /**
     * Read the incoming parameters and edit the vessel as appropriate.
     *
     * @param params
     *            The parameters from the client.
     * @return True is success, false if failure.
     */
    @UrlEndpoint(url = "/editdevice")
    public Response editVessel() {
        final Map<String, String> params = this.parameters;
        String auxpin = "", newName = "", heatgpio = "";
        String inputUnit = "", cutoff = "", coolgpio = "";
        String calibration = null;
        boolean heatInvert = false, coolInvert = false;

        Set<Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param = null;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();
        String errorMsg = "No Changes Made";

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add or update a Temperature probe to the system,"
                + " incoming object"
                + " should be a JSON Literal of \nold_name: {details}");
        usage.put("new_name", "The name of the Temperature Probe to add");
        usage.put("new_gpio", "The GPIO for the PID to work on");
        usage.put("aux_gpio", "The Auxilliary GPIO for the PID to work on");
        usage.put("Error", "Invalid parameters passed " + errorMsg + " = "
                + params.toString());

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

        if (parms.containsKey("new_heat_gpio")) {
            heatgpio = parms.get("new_heat_gpio");
        }

        if (parms.containsKey("new_cool_gpio")) {
            coolgpio = parms.get("new_cool_gpio");
        }

        if (parms.containsKey("heat_invert")) {
            heatInvert = parms.get("heat_invert").equals("on");
        }

        if (parms.containsKey("cool_invert")) {
            coolInvert = parms.get("cool_invert").equals("on");
        }

        if (parms.containsKey("auxpin")) {
            auxpin = parms.get("auxpin");
        }

        if (parms.containsKey("cutoff")) {
            cutoff = parms.get("cutoff");
        }

        if (parms.containsKey("calibration")) {
            calibration = parms.get("calibration");
        }

        if (inputUnit.equals("")) {
            BrewServer.LOG.warning("No Valid input unit");
            errorMsg = "No Valid Input Unit";
        }

        Temp tProbe = LaunchControl.findTemp(inputUnit);
        PID tPID = LaunchControl.findPID(inputUnit);

        if (tProbe == null) {
            inputUnit = inputUnit.replace("_", " ");
            tProbe = LaunchControl.findTemp(inputUnit);
            tPID = LaunchControl.findPID(inputUnit);
        }

        if (tProbe == null) {
            LaunchControl.setMessage("Couldn't find PID: " + inputUnit);

            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }

        if (tProbe != null && !newName.equals("")) {
            tProbe.setName(newName);
            BrewServer.LOG.warning("Updated temp name " + newName);
        }

        if (!cutoff.equals("")) {
            tProbe.setCutoffTemp(cutoff);
        }

        if (calibration != null) {
            tProbe.setCalibration(calibration);
        }

        if (tPID != null && !newName.equals("")) {
            tPID.getTemp().setName(newName);
            BrewServer.LOG.warning("Updated PID Name" + newName);
        }

        if (newName.equals("")) {
            newName = inputUnit;
        }

        // HEATING GPIO
        if (!heatgpio.equals("") || !coolgpio.equals("")) {
            // The GPIO is Set.
            if (tPID == null) {
                // No PID already, create one.
                tPID = new PID(tProbe, newName);
                // Setup the heating output
                if (!heatgpio.equals("")) {
                    tPID.setHeatGPIO(heatgpio);
                    tPID.setHeatInverted(heatInvert);
                }
                // Setup the cooling output
                if (!coolgpio.equals("")) {
                    tPID.setCoolGPIO(coolgpio);
                    tPID.setCoolInverted(coolInvert);
                }
                tPID.setAux(auxpin);
                LaunchControl.addPID(tPID);
                BrewServer.LOG.warning("Create PID");
                return new Response(Status.OK, MIME_TYPES.get("txt"),
                        "PID Added");
            } else {
                if (!heatgpio.equals(tPID.getHeatGPIO())) {
                    // We have a PID, set it to the new value
                    tPID.setHeatGPIO(heatgpio);
                }
                tPID.setHeatInverted(heatInvert);
                if (!coolgpio.equals(tPID.getCoolGPIO())) {
                    // We have a PID, set it to the new value
                    tPID.setCoolGPIO(coolgpio);
                }
                tPID.setCoolInverted(coolInvert);
                return new Response(Status.OK, MIME_TYPES.get("txt"),
                        "PID Updated");
            }
        } else {
            if (tPID != null) {
                LaunchControl.deletePID(tPID);
                tPID.setHeatGPIO("");
            }
        }

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Add a new timer to the brewery.
     *
     * @return True if added ok
     */
    @UrlEndpoint(url = "/addtimer")
    public final Response addTimer() {
        String oldName = "", newName = "", gpio = "";
        String inputUnit = "";

        Set<Entry<String, String>> incomingParams = parameters.entrySet();
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
            inputUnit = parameters.get("form");
            parms = parameters;
        }

        // Fall back to the old style
        if (parms.containsKey("new_name")) {
            newName = parms.get("new_name");
        }

        if (LaunchControl.addTimer(newName, "")) {
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Timer Added");
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new pump to the system.");
        usage.put("new_name", "The name of the timer to add");
        usage.put("mode",
                "The mode for the timer, increment, or decrement (optional)");
        usage.put("Error", "Invalid parameters passed " + parameters.toString());

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Add a new pump to the brewery.
     * 
     * @param params
     *            The parameter list
     * @return True if added ok
     */
    @UrlEndpoint(url = "/addpump")
    public Response addPump() {
        String newName = "", gpio = "";
        String inputUnit = "";
        boolean invert = false;

        Set<Entry<String, String>> incomingParams = this.parameters.entrySet();
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
            inputUnit = this.parameters.get("form");
            parms = this.parameters;
        }

        // Fall back to the old style
        if (parms.containsKey("new_name")) {
            newName = parms.get("new_name");
        }

        if (parms.containsKey("new_gpio")) {
            gpio = parms.get("new_gpio");
        }

        if (parms.containsKey("invert")) {
            invert = parms.get("invert").equals("on");
        }

        if (LaunchControl.addPump(newName, gpio)) {
            LaunchControl.findPump(newName).setInverted(invert);
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Pump Added");
        } else {
            LaunchControl.setMessage(
                    "Could not add pump " + newName + ": " + gpio);
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new pump to the system");
        usage.put("new_name", "The name of the pump to add");
        usage.put("new_gpio", "The GPIO for the pump to work on");
        usage.put("Error", "Invalid parameters passed "
                + this.parameters.toString());

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Read the incoming parameters and update the PID as appropriate.
     * 
     * @param parameters
     *            The parameters from the client
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/updatepid")
    public Response updatePID() {
        String temp, mode = "off";
        BigDecimal dTemp = new BigDecimal(0), duty = new BigDecimal(0),
                heatcycle = new BigDecimal(0), setpoint = new BigDecimal(0),
                heatp = new BigDecimal(0), heati = new BigDecimal(0),
                heatd = new BigDecimal(0), min = new BigDecimal(0),
                max = new BigDecimal(0), time = new BigDecimal(0),
                coolcycle = new BigDecimal(0), coolp= new BigDecimal(0),
                cooli = new BigDecimal(0), coold = new BigDecimal(0),
                cooldelay = new BigDecimal(0), cycle = new BigDecimal(0);

        JSONObject sub_usage = new JSONObject();
        Map<String, String> parms = ParseParams(parameters);
        String inputUnit = parms.get("inputunit");
        boolean errorValue = false;
        PID tPID = LaunchControl.findPID(inputUnit);
        if (tPID == null) {
            BrewServer.LOG.warning("Couldn't find PID: " + inputUnit);
            LaunchControl.setMessage("Could not find PID: " + inputUnit);
            LaunchControl.setMessage("Bad inputs when updating."
                    + " Please check the system log");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    "");
        }

        // Fall back to the old style
        sub_usage.put("dutycycle", "The new duty cycle % to set");
        if (parms.containsKey("dutycycle")) {
            temp = parms.get("dutycycle");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                duty = dTemp;
                BrewServer.LOG.info("Duty cycle: " + duty);
            } catch (NumberFormatException nfe) {
                System.out.print("Bad duty");
                errorValue = true;
            }
        }

        sub_usage.put("setpoint", "The new target temperature to set");
        if (parms.containsKey("setpoint")) {
            temp = parms.get("setpoint");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                setpoint = dTemp;
                BrewServer.LOG.info("Set Point: " + setpoint);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.warning("Bad setpoint");
                errorValue = true;
            }
        }

        sub_usage.put("heatcycletime", "The new heat cycle time in seconds to set");
        if (parms.containsKey("heatcycletime")) {
            temp = parms.get("heatcycletime");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                heatcycle = dTemp;
                BrewServer.LOG.info("Cycle time: " + heatcycle);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidHeater()) {
                    BrewServer.LOG.warning("Bad cycle");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("heatp", "The new proportional value to set");
        if (parms.containsKey("heatp")) {
            temp = parms.get("heatp");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                heatp = dTemp;
                BrewServer.LOG.info("heat P: " + heatp);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidHeater()) {
                    BrewServer.LOG.warning("Bad heat p");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("heati", "The new integral value to set");
        if (parms.containsKey("heati")) {
            temp = parms.get("heati");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                heati = dTemp;
                BrewServer.LOG.info("Heat I: " + heati);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidHeater()) {
                    BrewServer.LOG.warning("Bad heat i");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("heatd", "The new head differential value to set");
        if (parms.containsKey("heatd")) {
            temp = parms.get("heatd");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                heatd = dTemp;
                BrewServer.LOG.info("Heat D: " + heatd);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidHeater()) {
                    BrewServer.LOG.warning("Bad heat d");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("coolcycletime", "The new cool cycle time in seconds to set");
        if (parms.containsKey("coolcycletime")) {
            temp = parms.get("coolcycletime");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                coolcycle = dTemp;
                BrewServer.LOG.info("Cycle time: " + coolcycle);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidCooler()) {
                    BrewServer.LOG.warning("Bad cycle");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("cycletime", "The new manual cycle time in seconds to set");
        if (parms.containsKey("cycletime")) {
            temp = parms.get("cycletime");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                cycle = dTemp;
                BrewServer.LOG.info("Cycle time: " + cycle);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidHeater()) {
                    BrewServer.LOG.warning("Bad cycle");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("cooldelay", "The new cool cycle delay in minutes to set");
        if (parms.containsKey("cooldelay")) {
            temp = parms.get("cooldelay");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                cooldelay = dTemp;
                BrewServer.LOG.info("Delay time: " + cooldelay);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidHeater()) {
                    BrewServer.LOG.warning("Bad Cool delay");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("coolp", "The new proportional value to set");
        if (parms.containsKey("coolp")) {
            temp = parms.get("coolp");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                coolp = dTemp;
                BrewServer.LOG.info("cool P: " + coolp);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidCooler()) {
                    BrewServer.LOG.warning("Bad cool p");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("cooli", "The new integral value to set");
        if (parms.containsKey("cooli")) {
            temp = parms.get("cooli");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                cooli = dTemp;
                BrewServer.LOG.info("Heat I: " + cooli);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidCooler()) {
                    BrewServer.LOG.warning("Bad cool i");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("coold", "The new head differential value to set");
        if (parms.containsKey("coold")) {
            temp = parms.get("coold");
            try {
                dTemp = new BigDecimal(temp.replace(",", "."));
                coold = dTemp;
                BrewServer.LOG.info("Heat D: " + coold);
            } catch (NumberFormatException nfe) {
                if (tPID.hasValidCooler()) {
                    BrewServer.LOG.warning("Bad cool d");
                    errorValue = true;
                }
            }
        }

        sub_usage.put("mode", "The new mode to set");
        if (parms.containsKey("mode")) {
            mode = parms.get("mode");
            BrewServer.LOG.info("Mode: " + mode);
        }

        sub_usage.put("min",
                "The minimum temperature to enable the output (HYSTERIA)");
        if (parms.containsKey("min")) {
            try {
                dTemp = new BigDecimal(parms.get("min").replace(",", "."));
                min = dTemp;
                BrewServer.LOG.info("Min: " + mode);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.warning("Bad minimum");
                errorValue = true;
            }
        }

        sub_usage.put("max",
                "The maximum temperature to disable the output (HYSTERIA)");
        if (parms.containsKey("max")) {
            try {
                dTemp = new BigDecimal(parms.get("max").replace(",", "."));
                max = dTemp;
                BrewServer.LOG.info("Max: " + mode);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.warning("Bad maximum");
                errorValue = true;
            }
        }

        sub_usage.put("time",
                "The minimum time when enabling the output (HYSTERIA)");
        if (parms.containsKey("time")) {
            try {
                dTemp = new BigDecimal(parms.get("time").replace(",", "."));
                time = dTemp;
                BrewServer.LOG.info("Time: " + time);
            } catch (NumberFormatException nfe) {
                BrewServer.LOG.warning("Bad time");
                errorValue = true;
            }
        }

        BrewServer.LOG.info("Form: " + inputUnit);

        JSONObject usage = new JSONObject();
        usage.put(":PIDname", sub_usage);

        if (errorValue) {
            LaunchControl.setMessage("Bad inputs when updating. Please check the system log");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString()); 
        }
        
        if (tPID != null) {
            if (mode.equalsIgnoreCase("hysteria")) {
                tPID.setHysteria(min, max, time);
                tPID.useHysteria();
            } else {
                BrewServer.LOG.info(mode + ":" + duty + ":" + heatcycle + ":"
                        + setpoint + ":" + heatp + ":" + heati + ":" + heatd);
                tPID.updateValues(mode, duty, heatcycle, setpoint, heatp, heati, heatd);
                tPID.setCoolCycle(coolcycle);
                tPID.setCoolP(coolp);
                tPID.setCoolI(cooli);
                tPID.setCoolD(coold);
                tPID.setCoolDelay(cooldelay);

                if (mode.equalsIgnoreCase("manual")) {
                    tPID.setManualDuty(duty);
                    tPID.setManualTime(cycle);
                }
            }
            return new Response(Status.OK, MIME_HTML, "PID " + inputUnit
                    + " updated");
        } else {
            BrewServer.LOG.warning("Attempted to update a non existent PID, "
                    + inputUnit + ". Please check your client");
            usage.put("Error", "Non existent PID specified: " + inputUnit);
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }

    }

    /**
     * Add a new data point for the volume reading.
     * @return A NanoHTTPD response
     */
    @UrlEndpoint(url = "/addvolpoint")
    public Response addVolumePoint() {
        JSONObject usage = new JSONObject();
        Map<String, String> parms = ParseParams(this.parameters);
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
        usage.put("adc_pin", "The ADC Pin to be used for analogue reads");

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
            error_msg = "Invalid temperature probe name supplied (" + name
                    + ")";
            usage.put("Error", "Invalid parameters: " + error_msg);
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }
        // We have a temp probe, check to see if there's a valid volume input
        if (!t.hasVolume()) {
            if (onewire_add == null && onewire_offset == null
                    && adc_pin == null) {
                error_msg = "No volume input setup, and no valid volume inputs provided";
                usage.put("Error", "Invalid parameters: " + error_msg);
                return new Response(Response.Status.BAD_REQUEST,
                        MIME_TYPES.get("json"), usage.toJSONString());
            }

            // Check for ADC Pin
            if (adc_pin != null && !adc_pin.equals("")) {
                int adcpin = Integer.parseInt(adc_pin);
                if (0 < adcpin || adcpin > 7) {
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
            BigDecimal actualVol = new BigDecimal(volume.replace(",", "."));
            if (t.addVolumeMeasurement(actualVol)) {
                return new Response(Response.Status.OK, MIME_TYPES.get("json"),
                        "{'Result': 'OK'}");
            }
        } catch (NumberFormatException nfe) {
            error_msg = "Could not setup volumes for " + volume + " Units: "
                    + units;
            LaunchControl.addMessage(error_msg);
            usage.put("Error", "Invalid parameters: " + error_msg);
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }

        return new Response(Response.Status.BAD_REQUEST,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    /**
     * Get the graph data.
     * @return the JSON Response data
     */
    @UrlEndpoint(url = "/graph-data")
    public NanoHTTPD.Response getGraphDataWrapper() {
        return getGraphData();
    }

    @UrlEndpoint(url = "/graph-data/")
    public NanoHTTPD.Response getGraphData() {
        if (!LaunchControl.recorderEnabled()) {
            return new NanoHTTPD.Response("Recorder disabled");
        }
        Map<String, String> params = this.parameters;
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

        // Read CSV files and make a JSON Response

        // Assume live for now until we can build a UI to deal with this
        boolean live = true;

        // If Live - read newest set
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
                        // Skip
                    }
                }
            }
            directory = String.valueOf(newest);
        }

        // Else Read based on time

        // Get files from directory
        File directoryFile = new File("graph-data/" + directory);

        File[] contents = directoryFile.listFiles();
        JSONObject xsData = new JSONObject();
        JSONObject axes = new JSONObject();
        JSONObject columns = new JSONObject();
        JSONArray dataBuffer = new JSONArray();
        long currentTime = System.currentTimeMillis();

        // Are we downloading the files?
        if (params.containsKey("download")
                && params.get("download").equalsIgnoreCase("true")) {
            String zipFileName = "graph-data/zipdownload-"
                + currentTime + ".zip";
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(zipFileName);
            } catch (FileNotFoundException ioe) {
                BrewServer.LOG.warning(
                        "Couldn't create zip file at: " + zipFileName);
                BrewServer.LOG.warning(ioe.getLocalizedMessage());
            }

            for (File content : contents) {
                try {
                    if (content.getName().endsWith(".csv")
                            && content.getName().toLowerCase()
                                    .startsWith(vessel.toLowerCase())) {
                        zipFile.addToZipFile(content.getAbsolutePath());
                    }
                } catch (IOException ioe) {
                    BrewServer.LOG.warning(
                            "Couldn't add " + content.getAbsolutePath()
                            + " to zipfile");
                }
            }
            try {
                zipFile.closeZip();
            } catch (IOException e) {
            }
            return BrewServer.serveFile(zipFileName, params, rootDir);
        }

        for (File content : contents) {
            if (content.getName().endsWith(".csv")
                    && content.getName().toLowerCase()
                            .startsWith(vessel.toLowerCase())) {
                String name = content.getName();

                // Strip off .csv
                name = name.substring(0, name.length() - 4);
                name = name.replace('-', ' ');

                if (parms.containsKey("bindto")
                        && ((String) parms.get("bindto"))
                        .endsWith("-graph_body")) {
                    name = name.substring(name.lastIndexOf(" ") + 1);
                }

                xsData.put(name, "x" + name);

                if (name.endsWith("duty")) {
                    axes.put(name, "y2");
                } else {
                    axes.put(name, "y");
                }

                JSONArray xArray = new JSONArray();
                JSONArray dataArray = new JSONArray();

                xArray.add("x" + name);
                dataArray.add(name);

                String lastLine = null;
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(content));
                    String line;
                    String[] lArray = null;

                    while ((line = reader.readLine()) != null) {
                        // Each line contains the timestamp and the value
                        lArray = line.split(",");
                        xArray.add(BrewDay.sFormat
                                .format(new Date(Long.parseLong(lArray[0])))
                                );
                        dataArray.add(lArray[1].trim());
                    }

                    if (lArray != null && Long.parseLong(lArray[0]) != currentTime) {
                        xArray.add(BrewDay.sFormat
                                .format(new Date(currentTime)));
                        dataArray.add(lArray[1].trim());
                    }

                    dataBuffer.add(xArray);
                    dataBuffer.add(dataArray);
                } catch (Exception e) {
                    // Do nothing
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            BrewServer.LOG.warning("Couldn't close file: "
                                    + content.getAbsolutePath());
                        }
                    }
                }

            }
        }

        JSONObject dataContent = new JSONObject();
        dataContent.put("columns", dataBuffer);
        if (parms.containsKey("updates")
                && Boolean.parseBoolean(parms.get("updates"))) {
            return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                    dataContent.toJSONString());
        }

        dataContent.put("xs", xsData);
        dataContent.put("axes", axes);
        dataContent.put("xFormat", "%H:%M:%S");

        JSONObject axisContent = new JSONObject();
        JSONObject y2Label = new JSONObject();
        y2Label.put("text", "Duty Cycle %");
        y2Label.put("position", "outer-middle");
        JSONObject y2 = new JSONObject();
        y2.put("show", "true");
        y2.put("label", y2Label);

        JSONObject y1Label = new JSONObject();
        y1Label.put("text", "Temperature");
        y1Label.put("position", "outer-middle");
        JSONObject y1 = new JSONObject();
        y1.put("show",  "true");
        y1.put("label", y1Label);

        JSONObject padding = new JSONObject();
        padding.put("top", 0);
        padding.put("bottom", 0);
        y1.put("padding", padding);

        JSONObject formatJSON = new JSONObject();
        formatJSON.put("format", "%H:%M:%S");
        formatJSON.put("culling", true);
        formatJSON.put("rotate", 90);
        JSONObject xContent = new JSONObject();
        xContent.put("type", "timeseries");
        xContent.put("tick", formatJSON);
        axisContent.put("x", xContent);
        axisContent.put("y", y1);
        axisContent.put("y2", y2);

        JSONObject finalJSON = new JSONObject();
        finalJSON.put("data", dataContent);
        finalJSON.put("axis", axisContent);

        if (parms.containsKey("bindto")) {
            finalJSON.put("bindto", "#" + parms.get("bindto"));
        } else {
            finalJSON.put("bindto", "#chart");
        }

        if (!((String) finalJSON.get("bindto")).endsWith("_body")) {
            JSONObject enabledJSON = new JSONObject();
            enabledJSON.put("enabled", true);
            finalJSON.put("zoom", enabledJSON);
        }

        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                finalJSON.toJSONString());

    }

    /**
     * Read the incoming parameters and update the name as appropriate.
     *
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/setbreweryname")
    public Response updateBreweryName() {

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the brewery name");
        usage.put("name", "The new brewery name");

        Map<String, String> parms = ParseParams(this.parameters);
        String newName = parms.get("name");

        if (newName != null && newName != "" && newName.length() > 0) {
            LaunchControl.setName(newName);
            return new Response(Response.Status.OK,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }

        return new Response(Response.Status.BAD_REQUEST,
                MIME_TYPES.get("json"), usage.toJSONString());

    }

    /**
     * Read the incoming parameters and update the name as appropriate.
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/uploadimage")
    public Response updateBreweryImage() {

        final Map<String, String> files = this.files;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the brewery image");
        usage.put("files", "The new image file");

        if (files.size() == 1) {
            for (Map.Entry<String, String> entry : files.entrySet()) {

                try {
                    File uploadedFile = new File(entry.getValue());
                    String fileType = Files.probeContentType(
                            uploadedFile.toPath());

                    if (fileType.equalsIgnoreCase(MIME_TYPES.get("gif"))
                        || fileType.equalsIgnoreCase(MIME_TYPES.get("jpg"))
                        || fileType.equalsIgnoreCase(MIME_TYPES.get("jpeg"))
                        || fileType.equalsIgnoreCase(MIME_TYPES.get("png")))
                    {
                        File targetFile = new File(rootDir, "brewerImage.gif");
                        if (targetFile.exists()) {
                            targetFile.delete();
                        }
                        LaunchControl.copyFile(uploadedFile, targetFile);

                        targetFile.setReadable(true, false);
                        targetFile.setWritable(true, false);

                        LaunchControl.setFileOwner(targetFile);
                    }
                } catch (IOException e) {
                    usage.put("error", "Bad file");
                }
            }
        }
        return new Response(Response.Status.BAD_REQUEST,
                MIME_TYPES.get("json"), usage.toJSONString());

    }

    /**
     * Update the pump order.
     * @return A HTTP Response
     */
    @UrlEndpoint(url = "/updatepumporder")
    public Response updatePumpOrder() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Re-order the pumps");
        usage.put(":name=:position", "The new orders, starting at 0");

        Status status = Response.Status.BAD_REQUEST;

        for (Map.Entry<String, String> entry: params.entrySet()) {
            if (entry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }
            Pump tPump = LaunchControl.findPump(entry.getKey());
            // Make Sure we're aware of this pump
            if (tPump == null) {
                LaunchControl.setMessage(
                        "Couldn't find Pump: " + entry.getKey());
                continue;
            }

            try {
                tPump.setPosition(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage(
                        "Couldn't parse " + entry.getValue()
                        + " as an integer");
            }
            status = Response.Status.OK;
        }
        return new Response(status,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    /**
     * Delete a pump.
     * @return a reponse
     */
    @UrlEndpoint(url = "/deletepump")
    public Response deletePump() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Delete the specified pump");
        usage.put("name=:pump", "The pump to delete");
        Status status = Response.Status.OK;

        // find the pump
        String pumpName = params.get("name");
        if (pumpName != null) {
            LaunchControl.deletePump(pumpName);
        }
        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Update the timer order.
     * @return A HTTP Response
     */
    @UrlEndpoint(url = "/updatetimerorder")
    public Response updateTimerOrder() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Re-order the timers");
        usage.put(":name=:position", "The new orders, starting at 0");

        if (params.size() == 0) {
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }

        for (Map.Entry<String, String> entry: params.entrySet()) {
            if (entry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }

            Timer tTimer = LaunchControl.findTimer(entry.getKey());
            // Make Sure we're aware of this pump
            if (tTimer == null) {
                LaunchControl.setMessage(
                        "Couldn't find Timer: " + entry.getKey());
                continue;
            }

            try {
                tTimer.setPosition(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage(
                        "Couldn't parse " + entry.getValue()
                        + " as an integer");
            }
        }
        return new Response(Response.Status.OK,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    /**
     * Delete a timer.
     * @param parms
     * @return a response
     */
    @UrlEndpoint(url = "/deletetimer")
    public Response deleteTimer() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Delete the specified timer");
        usage.put("name=:timer", "The timer to delete");
        Status status = Response.Status.OK;

        // find the pump
        String timerName = params.get("name");
        if (timerName != null) {
            LaunchControl.deleteTimer(timerName);
        }
        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    @UrlEndpoint(url = "/setscale")
    public Response setScale() {
        Map<String, String> parms = this.parameters;
        Map<String, String> params = ParseParams(parms);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Change the scale on all the temperature probes");
        usage.put("scale", "New scale 'F' or 'C'");

        Status status = Response.Status.OK;

        // Iterate all the temperature probes and change the scale
        if (!LaunchControl.setTempScales(params.get("scale"))) {
            status = Response.Status.BAD_REQUEST;
        }

        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    @UrlEndpoint(url = "/toggledevice")
    public Response toggleDevice() {
        Map<String, String> parms = this.parameters;
        Map<String, String> params = ParseParams(parms);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Hide a specific device from the UI");
        usage.put("device", "The device name to hide");

        Status status = Response.Status.OK;
        Temp temp = LaunchControl.findTemp(params.get("device"));

        if (temp != null) {
            temp.toggleVisibility();
        } else {
            status = Response.Status.BAD_REQUEST;
        }

        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    @UrlEndpoint(url = "/updatesystemsettings")
    public final Response updateSystemSettings() {
        Map<String, String> params = ParseParams(parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Update the system settings");
        usage.put("recorder", "True/false to enable/disable the recorder.");
        usage.put("recorderDiff", "The tolerance to record data changes.");
        usage.put("recorderTime",
                "The time between sampling the data for recording.");

        if (params.containsKey("recorder")) {
            boolean recorderOn = params.get("recorder").equals("on");
            // If we're on, disable the recorder
            if (!recorderOn) {
                LaunchControl.disableRecorder();
            } else if (recorderOn) {
                LaunchControl.enableRecorder();
            }
        } else {
            LaunchControl.disableRecorder();
        }

        if (params.containsKey("recorderTolerence")) {
            try {
                StatusRecorder.THRESHOLD =
                    Double.parseDouble(params.get("recorderTolerence"));
            } catch (Exception e) {
                LaunchControl.setMessage(
                    "Failed to parse Recorder diff as a double\n"
                            + e.getMessage()
                            + LaunchControl.getMessage());
            }
        }

        if (params.containsKey("recorderTime")) {
            try {
                StatusRecorder.SLEEP =
                    Long.parseLong(params.get("recorderTime"));
            } catch (Exception e) {
                LaunchControl.setMessage(
                    "Failed to parse Recorder time as a long\n" + e.getMessage()
                            + LaunchControl.getMessage());
            }
        }
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                usage.toString());
    }

    /**
     * Set the gravity for the specified device.
     * @param parms
     * @return A response
     */
    @UrlEndpoint(url = "/setgravity")
    public final Response setGravity() {
        Map<String, String> parms = this.parameters;
        Map<String, String> params = ParseParams(parms);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Update the specified volume reader gravity");
        usage.put("device", "The device name to set the gravity on.");
        usage.put("gravity", "The new specific gravity to set");
        Status status = Status.OK;

        Temp temp = null;

        if (params.containsKey("inputunit")) {
            temp = LaunchControl.findTemp(params.get("inputunit"));
            if (temp == null) {
                LaunchControl.addMessage(
                        "Could not find the temperature probe for: "
                                + params.get("inputunit"));
                status = Status.BAD_REQUEST;
            }
        } else {
            LaunchControl.addMessage(
                    "No device provided when setting the gravity");
            status = Status.BAD_REQUEST;
        }

        if (params.containsKey("gravity")) {
            try {
                BigDecimal gravity = new BigDecimal(params.get("gravity"));
                if (temp != null) {
                    temp.setGravity(gravity);
                }
            } catch (NumberFormatException nfe) {
                LaunchControl.addMessage(
                        "Could not parse gravity as a decimal: "
                                + params.get("gravity"));
            }
        }

        return new Response(status, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    @UrlEndpoint(url="/controller")
    public Response renderController() {
        RenderHTML renderController = new RenderHTML();
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        String result = "";
        try {
            renderController.renderOn(html);
            result = html.toHtml();
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                result);
    }

    @UrlEndpoint(url = "/lockpage")
    public Response lockPage() {
        LaunchControl.pageLock = true;
        return new NanoHTTPD.Response(Status.OK,
                BrewServer.MIME_TYPES.get("json"),
                "{status: 'locked'}");
    }

    @UrlEndpoint(url = "/unlockpage")
    public Response unlockPage() {
        LaunchControl.listOneWireSys(false);
        LaunchControl.pageLock = false;
        return new NanoHTTPD.Response(Status.OK,
                BrewServer.MIME_TYPES.get("json"),
                "{status: 'unlocked'}");
    }
    
    @UrlEndpoint(url = "/brewerImage.gif")
    public Response getBrewerImage() {
        String uri = "/brewerImage.gif";

        // Has the user uploaded a file?
        if (new File(rootDir, uri).exists()) {
            return BrewServer.serveFile(uri, this.header, rootDir);
        }
        // Check to see if there's a theme set.
        if (LaunchControl.theme != null
                && !LaunchControl.theme.equals("")) {
            if (new File(rootDir,
                    "/logos/" + LaunchControl.theme + ".gif").exists()) {
                return BrewServer.serveFile("/logos/" + LaunchControl.theme + ".gif",
                        this.header, rootDir);
            }
        }

        return new NanoHTTPD.Response(Status.BAD_REQUEST,
                BrewServer.MIME_TYPES.get("json"),
                "{image: 'unavailable'}");
    }

    @UrlEndpoint(url = "/updatepump")
    public Response updatePump() {

        if (parameters.containsKey("toggle")) {
            String pumpname = parameters.get("toggle");
            Pump tempPump =
                    LaunchControl.findPump(pumpname.replaceAll("_", " "));
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
                        MIME_TYPES.get("txt"), "Invalid pump: " + pumpname
                                + " provided.");
            }
        }

        return new Response(Status.BAD_REQUEST,
                MIME_TYPES.get("txt"), "Invalid data"
                        + " provided.");
    }

    @UrlEndpoint(url = "/toggleaux")
    public Response toggleAux() {
        JSONObject usage = new JSONObject();
        if (parameters.containsKey("toggle")) {
            String pidname = parameters.get("toggle");
            PID tempPID = LaunchControl.findPID(pidname);
            if (tempPID != null) {
                tempPID.toggleAux();
                return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                        "Updated Aux for " + pidname);
            } else {
                BrewServer.LOG.warning("Invalid PID: " + pidname + " provided.");
                usage.put("Error", "Invalid name supplied: " + pidname);
            }
        }

        usage.put("toggle",
                "The name of the PID to toggle the aux output for");
        return new Response(usage.toJSONString());
    }

    @UrlEndpoint(url = "/updateday")
    public Response updateDay() {
        // we're storing the data for the brew day
        String tempDateStamp;
        BrewDay brewDay = LaunchControl.getBrewDay();

        // updated date
        if (parameters.containsKey("updated")) {
            tempDateStamp = parameters.get("updated");
            brewDay.setUpdated(tempDateStamp);
        } else {
            // we don't have an updated datestamp
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "No update datestamp, not updating a thang! YA HOSER!");
        }

        Iterator<Entry<String, String>> it = parameters.entrySet().iterator();
        Entry<String, String> e = null;

        while (it.hasNext()) {
            e = it.next();

            if (e.getKey().endsWith("Start")) {
                int trimEnd = e.getKey().length() - "Start".length();
                String name = e.getKey().substring(0, trimEnd);
                brewDay.startTimer(name, e.getValue());
            } else if (e.getKey().endsWith("End")) {
                int trimEnd = e.getKey().length() - "End".length();
                String name = e.getKey().substring(0, trimEnd);
                brewDay.stopTimer(name, e.getValue());
            } else if (e.getKey().endsWith("Reset")) {
                int trimEnd = e.getKey().length() - "Reset".length();
                String name = e.getKey().substring(0, trimEnd);
                brewDay.resetTimer(name);
            }
        }

        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Updated Brewday");
    }

    @UrlEndpoint(url = "/getvolumeform")
    public Response getVolumeForm() {
        if (!parameters.containsKey("vessel")) {
            LaunchControl.setMessage("No Vessel provided");
            return new Response(Status.BAD_REQUEST, MIME_HTML,
                    "No vessel provided");
        }

        // Check to make sure we have a valid vessel
        Temp temp = LaunchControl.findTemp(parameters.get("vessel"));
        if (temp == null) {
            return new Response(Status.BAD_REQUEST, MIME_HTML,
                    "Could not find vessel: " + parameters.get("vessel"));
        }

        // Render away
        VolumeEditForm volEditForm = new VolumeEditForm(
                temp);
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        String result = "";
        try {
            volEditForm.renderOn(html);
            result = html.toHtml();
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
            LaunchControl.setMessage(result);
        }
        return new Response(Status.OK, MIME_HTML, result);
    }

    @UrlEndpoint(url = "/getphsensorform")
    public Response getPhSensorForm() {
        PhSensor phSensor = null;

        if (!parameters.containsKey("sensor")) {
            phSensor = new PhSensor();
        } else {
            phSensor = LaunchControl.findPhSensor(parameters.get("sensor"));
        }

        // Render away
        PhSensorForm phSensorForm = new PhSensorForm(phSensor);
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        String result = "";
        try {
            phSensorForm.renderOn(html);
            result = html.toHtml();
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
            LaunchControl.setMessage(result);
        }
        return new Response(Status.OK, MIME_HTML, result);
    }

    @UrlEndpoint(url = "/addphsensor")
    public Response addPhSensor() {
        PhSensor phSensor = null;
        String result = "";
        Map<String, String> localParams = this.ParseParams();
        if (localParams.containsKey("name")) {
            phSensor = LaunchControl.findPhSensor(localParams.get("name"));
        }

        if (phSensor == null) {
            phSensor = new PhSensor();
            phSensor.setName(localParams.get("name"));
            LaunchControl.phSensorList.add(phSensor);
        }

        // Update the pH Sensor
        String temp = localParams.get("dsAddress");
        if (temp != null) {
            phSensor.setDsAddress(temp);
        }
        temp = localParams.get("dsOffset");
        if (temp != null) {
            phSensor.setDsOffset(temp);
        }
        temp = localParams.get("adc_pin");
        if (temp != null) {
            int newPin = -1;
            if (!temp.equals("")) {
                try {
                    newPin = Integer.parseInt(temp);
                } catch (NumberFormatException nfe) {
                    LaunchControl.setMessage(
                            "Couldn't parse analog pin " + temp
                            + " as an integer.");
                }
            }
            phSensor.setAinPin(newPin);
        }
        temp = localParams.get("ph_model");
        if (temp != null) {
            phSensor.setModel(temp);
        }

        // Check for a calibration
        temp = localParams.get("calibration");
        if (temp != null && !temp.equals("")) {
            try {
                BigDecimal calibration = new BigDecimal(temp);
                phSensor.calibrate(calibration);
            } catch (Exception e) {
                LaunchControl.setMessage("Could not read calibration: " + temp
                        + ", " + e.getLocalizedMessage());
            }
        }

        return new Response(Status.OK, MIME_HTML, result);
    }

    @UrlEndpoint(url = "/delphsensor")
    public Response delPhSensor() {
        PhSensor phSensor = null;
        String result = "";
        if (parameters.containsKey("name")) {
            LaunchControl.deletePhSensor(parameters.get("name"));
            LaunchControl.setMessage("pH Sensor " + parameters.get("name")
                    + " deleted.");
        }

        return new Response(Status.OK, MIME_HTML, "");
    }

    /**
     * Update the specified pH Sensor reading.
     * @return a Response with the pH Sensor value.
     */
    @UrlEndpoint(url = "/readphsensor")
    public final Response readPhSensor() {
        PhSensor phSensor = LaunchControl.findPhSensor(
                parameters.get("name").replace(" ", "_"));
        // CHeck for errors
        String result;
        if (phSensor == null) {
            result = "Couldn't find phSensor";
        } else {
            result = phSensor.calcPhValue().toPlainString();
        }
        // return the value
        return new Response(Status.OK, MIME_HTML, result);
    }

    /**
     * Get the trigger input form for the trigger type specified.
     * @return The HTML Representing the trigger form.
     */
    @UrlEndpoint(url = "/getTriggerForm")
    public final Response getTriggerForm() {
        int position = Integer.parseInt(parameters.get("position"));
        String type = parameters.get("type");
        HtmlCanvas result = TriggerControl.getNewTriggerForm(position, type);
        return new Response(Status.OK, MIME_HTML, result.toHtml());
    }

    /**
     * Get the form representing the new triggers.
     * @return The Response with the HTML.
     */
    @UrlEndpoint(url = "/getNewTriggers")
    public final Response getNewTriggersForm() {
        Status status = Status.OK;
        HtmlCanvas htmlCanvas = null;
        String probe = parameters.get("temp");
        if (probe == null) {
            LaunchControl.setMessage("No Temperature probe set.");
        }
        try {
            htmlCanvas = TriggerControl.getNewTriggersForm(probe);
        } catch (IOException ioe) {
            LaunchControl.setMessage(
                    "Failed to get the new triggers form: "
                            + ioe.getLocalizedMessage());
            status = Status.BAD_REQUEST;
        }

        return new Response(status, MIME_HTML, htmlCanvas.toHtml());
    }
}
