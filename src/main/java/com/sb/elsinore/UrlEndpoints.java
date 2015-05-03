package com.sb.elsinore;

import ca.strangebrew.recipe.Recipe;
import com.sb.common.SBStringUtils;
import com.sb.elsinore.html.*;
import com.sb.elsinore.notificiations.Notifications;
import jGPIO.InvalidGPIOException;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.tools.PrettyWriter;

import com.sb.elsinore.NanoHTTPD.Response;
import com.sb.elsinore.NanoHTTPD.Response.Status;
import com.sb.elsinore.annotations.UrlEndpoint;
import com.sb.elsinore.inputs.PhSensor;
import com.sb.elsinore.recipes.BeerXMLReader;
import com.sb.elsinore.triggers.TriggerInterface;

import javax.xml.xpath.XPathException;

@SuppressWarnings("unchecked")
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
            put("axml", "application/xml");
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
        Entry<String, String> param;
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
     * @return True if the profile is updated successfully.
     */
    @UrlEndpoint(url = "/triggerprofile")
    public final Response updateTriggerProfile() {
        String tempProbe = parameters.get("tempprobe");
        if (tempProbe == null) {
            LaunchControl.setMessage("Could not trigger profile,"
                    + " no tempProbe provided");
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "FAILED");
        }
        Temp tProbe = LaunchControl.findTemp(tempProbe);
        TriggerControl triggerControl = tProbe.getTriggerControl();
        if (triggerControl.triggerCount() > 0) {
            triggerControl.sortTriggerSteps();
            triggerControl.activateTrigger(0);
        }

        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "Updated MashProfile");
    }

    /**
     * Reorder the triggers steps.
     * @return The Response object.
     */
    @UrlEndpoint(url = "/reordertriggers")
    public final Response reorderMashProfile() {
        Map<String, String> params = this.parameters;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the order for the mash profile.");
        usage.put("tempprobe",
                "The name of the TempProbe to change the mash profile order on.");
        usage.put(":old=:new", "The old position and the new position");
        // Resort the mash profile for the PID, then sort the rest
        String tempProbe = params.get("tempprobe");
        // Do we have a PID coming in?
        if (tempProbe == null) {
            LaunchControl.setMessage(
                    "No Temp Probe supplied for trigger profile order sort");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }
        // Prevent any errors from the PID not being a number.
        params.remove("tempprobe");
        // Do we have a mash control for the PID?
        TriggerControl mControl = LaunchControl.findTriggerControl(tempProbe);
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
                LaunchControl.setMessage(
                    "Failed to parse Trigger reorder value,"
                    + " things may get weird: "
                    + mEntry.getKey() + ": " + mEntry.getValue());
            } catch (IndexOutOfBoundsException ie) {
                LaunchControl.setMessage(
                        "Invalid trigger position, things may get weird");
            }
        }
        mControl.sortTriggerSteps();
        return new Response(Status.OK, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Delete the specified trigger step.
     * @return The Response.
     */
    @UrlEndpoint(url = "/deltriggerstep")
    public Response delTriggerStep() {
        Map<String, String> params = this.parameters;
        // Parse the response
        // Temp unit, PID, duration, temp, method, type, step number
        // Default to the existing temperature scale
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new mashstep to the specified PID");
        usage.put("tempprobe", "The PID to delete the mash step from");
        usage.put("position", "The mash step to delete");
        Status status = Status.OK;

        try {
            String tempProbe = params.get("tempprobe");
            if (tempProbe == null) {
                BrewServer.LOG.warning("No tempprobe parameter supplied to delete trigger.");
            }
            int position = Integer.parseInt(params.get("position"));
            TriggerControl mControl = LaunchControl
                    .findTemp(tempProbe).getTriggerControl();

            if (mControl != null) {
                mControl.delTriggerStep(position);
            } else {
                status = Status.BAD_REQUEST;
            }
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            LaunchControl.setMessage(
                    "Couldn't parse the position to delete: " + params);
            status = Status.BAD_REQUEST;
        } catch (NullPointerException ne) {
            ne.printStackTrace();
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
     * @return True if set, false if there's an error
     */
    @UrlEndpoint(url = "/toggleTrigger")
    public Response toggleTriggerProfile() {

        String tempProbe;
        if (parameters.containsKey("tempprobe")) {
            tempProbe = parameters.get("tempprobe");
        } else {
            BrewServer.LOG.warning("No Temp provided to toggle Trigger Profile");
            return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                    "Failed to toggle mash profile");
        }

        boolean activate = false;
        if (parameters.containsKey("status")) {
            if (parameters.get("status").equalsIgnoreCase("activate")) {
                activate = true;
            }
        } else {
            BrewServer.LOG.warning("No Status provided to toggle Profile");
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

        TriggerControl mObj = LaunchControl
                .findTemp(tempProbe).getTriggerControl();


        TriggerInterface triggerEntry = mObj.getCurrentTrigger();

        int stepToUse;
        // We have a mash step position
        if (position >= 0) {
            BrewServer.LOG.warning("Using mash step for " + tempProbe
                    + " at position " + position);
        }

        if (!activate) {
            // We're de-activating everything
            stepToUse = -1;
        } else if (triggerEntry == null && mObj.triggerCount() > 0) {
            stepToUse = 0;
        } else if (triggerEntry != null) {
            stepToUse = triggerEntry.getPosition();
        } else {
            stepToUse = -1;
        }

        if (stepToUse >= 0 && activate) {
            mObj.activateTrigger(stepToUse);
            BrewServer.LOG.warning(
                    "Activated " + tempProbe + " step at " + stepToUse);
        } else {
            mObj.deactivateTrigger(stepToUse);
            BrewServer.LOG.warning("Deactivated " + tempProbe + " step at "
                    + stepToUse);
        }

        LaunchControl.startMashControl(tempProbe);

        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Toggled profile");
    }

    /**
     * Read the incoming parameters and edit the vessel as appropriate.
     * @return True is success, false if failure.
     */
    @SuppressWarnings("ConstantConditions")
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
        Entry<String, String> param;
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

        if (tPID == null) {
            // No PID already, create one.
            tPID = new PID(tProbe, newName);
        }
        // HEATING GPIO
        if (heatgpio == null || heatgpio.equals("")) {
            tPID.delHeatGPIO();
        }
        if (coolgpio == null || coolgpio.equals("")) {
            tPID.delCoolGPIO();
        }
        tPID.setAux(auxpin);

        if (!heatgpio.equals("") || !coolgpio.equals("")) {
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
            LaunchControl.addPID(tPID);
        }
        if (heatgpio.equals("") && coolgpio.equals("")) {
            LaunchControl.deletePID(tPID);
        }


        LaunchControl.saveConfigFile();
        return new Response(Status.OK, MIME_TYPES.get("txt"),
                "PID Updated");
    }

    /**
     * Add a new timer to the brewery.
     *
     * @return True if added ok
     */
    @UrlEndpoint(url = "/addtimer")
    public final Response addTimer() {
        String newName = "";
        String inputUnit = "";

        Set<Entry<String, String>> incomingParams = parameters.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param;
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
            parms = parameters;
        }

        // Fall back to the old style
        if (parms.containsKey("new_name")) {
            newName = parms.get("new_name");
        }

        String target = parms.get("target");

        if (LaunchControl.addTimer(newName, target)) {
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Timer Added");
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new switch to the system.");
        usage.put("new_name", "The name of the timer to add");
        usage.put("mode",
                "The mode for the timer, increment, or decrement (optional)");
        usage.put("target", "The target time for the timer");
        usage.put("Error", "Invalid parameters passed " + parameters.toString());

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Add a new switch to the brewery.
     * 
     * @return True if added ok
     */
    @UrlEndpoint(url = "/addswitch")
    public Response addSwitch() {
        String newName = "", gpio = "";
        String inputUnit = "";
        boolean invert = false;

        Set<Entry<String, String>> incomingParams = this.parameters.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param;
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

        if (LaunchControl.addSwitch(newName, gpio)) {
            LaunchControl.findSwitch(newName).setInverted(invert);
            LaunchControl.saveSettings();
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Switch Added");
        } else {
            LaunchControl.setMessage(
                    "Could not add switch " + newName + ": " + gpio);
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new switch to the system");
        usage.put("new_name", "The name of the switch to add");
        usage.put("new_gpio", "The GPIO for the switch to work on");
        usage.put("Error", "Invalid parameters passed "
                + this.parameters.toString());

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Read the incoming parameters and update the PID as appropriate.
     *
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/updatepid")
    public Response updatePID() {
        String temp, mode = "off";
        BigDecimal dTemp, duty = new BigDecimal(0),
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
    }

    /**
     * Add a new data point for the volume reading.
     * @return A NanoHTTPD response
     */
    @UrlEndpoint(url = "/addvolpoint")
    public Response addVolumePoint() {
        JSONObject usage = new JSONObject();
        Map<String, String> parms = ParseParams(this.parameters);
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

        String name = parms.get("name");
        String volume = parms.get("volume");
        String units = parms.get("units");

        String onewire_add = parms.get("onewire_address");
        String onewire_offset = parms.get("onewire_offset");
        String adc_pin = parms.get("adc_pin");

        String error_msg;

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
                        "{'Result':\"OK\"}");
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

        Map<String, String> parms = ParseParams(parameters);
        return LaunchControl.getRecorder().getData(parms);
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

        if (newName != null && !newName.equals("") && newName.length() > 0) {
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
                        if (targetFile.exists() && !targetFile.delete()) {
                            BrewServer.LOG.warning("Failed to delete " + targetFile.getCanonicalPath());
                        }
                        LaunchControl.copyFile(uploadedFile, targetFile);

                        if (!targetFile.setReadable(true, false) || targetFile.setWritable(true, false)) {
                            BrewServer.LOG.warning("Failed to set read write permissions on " + targetFile.getCanonicalPath());
                        }

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
     * Update the switch order.
     * @return A HTTP Response
     */
    @UrlEndpoint(url = "/updateswitchorder")
    public Response updateSwitchOrder() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Re-order the switches");
        usage.put(":name=:position", "The new orders, starting at 0");

        Status status = Response.Status.BAD_REQUEST;

        for (Map.Entry<String, String> entry: params.entrySet()) {
            if (entry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }
            Switch tSwitch = LaunchControl.findSwitch(entry.getKey());
            // Make Sure we're aware of this switch
            if (tSwitch == null) {
                LaunchControl.setMessage(
                        "Couldn't find Switch: " + entry.getKey());
                continue;
            }

            try {
                tSwitch.setPosition(Integer.parseInt(entry.getValue()));
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage(
                        "Couldn't parse " + entry.getValue()
                        + " as an integer");
            }
            Collections.sort(LaunchControl.switchList);
            status = Response.Status.OK;
        }
        return new Response(status,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    /**
     * Delete a switch.
     * @return a reponse
     */
    @UrlEndpoint(url = "/deleteswitch")
    public Response deleteSwitch() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Delete the specified switch");
        usage.put("name=:switch", "The switch to delete");
        Status status = Response.Status.OK;

        // find the switch
        String switchName = params.get("name");
        if (switchName != null) {
            LaunchControl.deleteSwitch(switchName);
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
            // Make Sure we're aware of this switch
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
        LaunchControl.sortTimers();
        return new Response(Response.Status.OK,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    /**
     * Delete a timer.
     * @return a response
     */
    @UrlEndpoint(url = "/deletetimer")
    public Response deleteTimer() {
        Map<String, String> params = ParseParams(this.parameters);
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Delete the specified timer");
        usage.put("name=:timer", "The timer to delete");
        Status status = Response.Status.OK;

        // find the switch
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
            if (recorderOn) {
                LaunchControl.enableRecorder();
            } else {
                LaunchControl.disableRecorder();
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
        String result;
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
                "{status: \"locked\"}");
    }

    @UrlEndpoint(url = "/unlockpage")
    public Response unlockPage() {
        LaunchControl.listOneWireSys(false);
        LaunchControl.pageLock = false;
        return new NanoHTTPD.Response(Status.OK,
                BrewServer.MIME_TYPES.get("json"),
                "{status: \"unlocked\"");
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
                "{image: \"unavailable\"}");
    }

    @UrlEndpoint(url = "/updateswitch")
    public Response updateSwitch() {

        if (parameters.containsKey("toggle")) {
            String switchname = parameters.get("toggle");
            Switch tempSwitch =
                    LaunchControl.findSwitch(switchname.replaceAll("_", " "));
            if (tempSwitch != null) {
                if (tempSwitch.getStatus()) {
                    tempSwitch.turnOff();
                } else {
                    tempSwitch.turnOn();
                }

                return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                        "Updated Switch");
            } else {
                JSONObject usage = new JSONObject();
                usage.put("Error", "Invalid name supplied: " + switchname);
                usage.put("toggle", "The name of the Switch to toggle on/off");
                return new Response(Status.BAD_REQUEST,
                        MIME_TYPES.get("json"), usage.toJSONString());
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
        Entry<String, String> e;

        while (it.hasNext()) {
            e = it.next();

            if (e.getKey().endsWith("Start")) {
                int trimEnd = e.getKey().length() - "Start".length();
                String name = e.getKey().substring(0, trimEnd);
                brewDay.startTimer(name, e.getValue());
            } else if (e.getKey().endsWith("End")) {
                int trimEnd = e.getKey().length() - "End".length();
                String name = e.getKey().substring(0, trimEnd);
                if (e.getValue().equals("null")) {
                    brewDay.stopTimer(name, new Date());
                } else {
                    brewDay.stopTimer(name, e.getValue());
                }
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
        String result;
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
        PhSensor phSensor;

        if (!parameters.containsKey("sensor")) {
            phSensor = new PhSensor();
        } else {
            phSensor = LaunchControl.findPhSensor(parameters.get("sensor"));
        }

        // Render away
        PhSensorForm phSensorForm = new PhSensorForm(phSensor);
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        String result;
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
        String sensorName = parameters.get("name");
        if (sensorName != null
                && LaunchControl.deletePhSensor(sensorName)) {
            LaunchControl.setMessage("pH Sensor '" + sensorName
                    + "' deleted.");
        } else {
            LaunchControl.setMessage("Could not find pH Sensor with name: "
                    + sensorName);
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
        HtmlCanvas htmlCanvas;
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
            htmlCanvas = new HtmlCanvas();
        }

        return new Response(status, MIME_HTML, htmlCanvas.toHtml());
    }

    /**
     * Get the trigger edit form for the specified parameters.
     * @return The Edit form.
     */
    @UrlEndpoint(url = "/gettriggeredit")
    public final Response getTriggerEditForm() {
        Status status = Status.OK;
        int position = Integer.parseInt(parameters.get("position"));
        String tempProbeName = parameters.get("tempprobe");
        Temp tempProbe = LaunchControl.findTemp(tempProbeName);
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        HtmlCanvas canvas = triggerControl.getEditTriggerForm(
                position, new JSONObject(parameters));
        if (canvas != null) {
            return new Response(status, MIME_HTML, canvas.toHtml());
        }
        return new Response(Status.BAD_REQUEST, MIME_HTML, "BAD");
    }

    /**
     * Add a new Trigger to the incoming tempProbe object.
     * @return A response for the user.
     */
    @UrlEndpoint(url = "/addtriggertotemp")
    public final Response addNewTrigger() {
        Status status = Status.OK;
        int position = Integer.parseInt(parameters.get("position"));
        String type = parameters.get("type");
        String tempProbeName = parameters.get("tempprobe");
        Temp tempProbe = LaunchControl.findTemp(tempProbeName);
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        triggerControl.addTrigger(position, type, new JSONObject(parameters));
        return new Response(status, MIME_HTML, "OK");
    }

    /**
     * Update the trigger.
     * @return The Edit form.
     */
    @UrlEndpoint(url = "/updatetrigger")
    public final Response updateTrigger() {
        Status status = Status.OK;
        int position = Integer.parseInt(parameters.get("position"));
        String tempProbeName = parameters.get("tempprobe");
        Temp tempProbe = LaunchControl.findTemp(tempProbeName);
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        triggerControl.updateTrigger(position, new JSONObject(parameters));
        return new Response(status, MIME_HTML, "OK");
    }

    /**
     * Reorder the devices in the UI.
     * @return A response object.
     */
    @UrlEndpoint(url = "/reorderprobes")
    public final Response reorderProbes() {
        Map<String, String> params = this.parameters;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the order for the probes.");
        usage.put(":name=:position", "The name and the new position");

        // Should be good to go, iterate and update!
        for (Map.Entry<String, String> mEntry: params.entrySet()) {
            if (mEntry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }
            try {
                String tName = mEntry.getKey();
                int newPos = Integer.parseInt(mEntry.getValue());
                Temp temp = LaunchControl.findTemp(tName);
                if (temp != null) {
                    temp.setPosition(newPos);
                }
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage(
                    "Failed to parse device reorder value,"
                    + " things may get weird: "
                    + mEntry.getKey() + ": " + mEntry.getValue());
            }
        }
        LaunchControl.sortDevices();
        return new Response(Status.OK, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/uploadbeerxml")
    public Response uploadBeerXML() {

        final Map<String, String> files = this.files;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the beerXML file");
        usage.put("files", "The new beerXML file");
        Status status = Status.ACCEPTED;
        if (files.size() == 1) {
            for (Map.Entry<String, String> entry : files.entrySet()) {

                try {
                    File uploadedFile = new File(entry.getValue());
                    String fileType = Files.probeContentType(
                            uploadedFile.toPath());

                    if (fileType.endsWith("/xml"))
                    {
                        BeerXMLReader.getInstance().readFile(uploadedFile);
                        ArrayList<String> recipeList = BeerXMLReader.getInstance().getListOfRecipes();
                        BrewServer.setRecipeList(recipeList);
                        if (recipeList.size() == 1) {
                            BrewServer.setCurrentRecipe(BeerXMLReader.getInstance().readRecipe(recipeList.get(0)));
                            LaunchControl.setMessage("A single recipe has been read in and set: " + BrewServer.getCurrentRecipe().getName());
                        } else {
                            LaunchControl.setMessage(recipeList.size() + " recipes read in.");
                        }
                    }
                } catch (IOException e) {
                    usage.put("error", "Bad file");
                    status = Status.BAD_REQUEST;
                } catch (XPathException e) {
                    e.printStackTrace();
                }
            }
        }
        return new Response(status,
                MIME_TYPES.get("json"), usage.toJSONString());
    }

    @UrlEndpoint(url = "/getrecipelist")
    public Response getRecipeList() {
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        try {
            new RecipeListForm().renderOn(html);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Response(Status.OK, MIME_HTML, html.toHtml());
    }

    @UrlEndpoint(url = "/showrecipe")
    public Response showRecipe(){
        String recipeName = this.parameters.get("recipeName");
        if (recipeName == null && BrewServer.getCurrentRecipe() == null) {
            return new Response(Status.BAD_REQUEST, MIME_HTML, "No recipe name provided");
        }

        Recipe recipe = null;
        if (recipeName != null && !recipeName.equals("")) {
            try {
                recipe = BeerXMLReader.getInstance().readRecipe(recipeName);
            } catch (XPathException e) {
                e.printStackTrace();
            }
        } else {
            recipe = BrewServer.getCurrentRecipe();
        }

        if (recipe == null) {
            return new Response(Status.BAD_REQUEST, MIME_HTML, "Could not find recipe: " + recipeName);
        }
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        try {
            new RecipeViewForm(recipe).renderOn(html);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Response(Status.OK, MIME_HTML, html.toHtml());
    }

    @UrlEndpoint(url="/setprofile")
    public Response setProfile() {
        String profile = parameters.get("profile");
        if (profile == null || profile.equals("")) {
            LaunchControl.setMessage("No profile provided");
            return new Response(Status.BAD_REQUEST, MIME_HTML, "No profile provided");
        }

        String tempProbe = parameters.get("tempprobe");
        if (tempProbe == null || tempProbe.equals("")) {
            LaunchControl.setMessage("No temperature probe provided");
            return new Response(Status.BAD_REQUEST, MIME_HTML, "No temperature probe provided");
        }

        Temp temp = LaunchControl.findTemp(tempProbe);
        if (temp == null) {
            LaunchControl.setMessage("Couldn't find temp probe: " + tempProbe);
            return new Response(Status.BAD_REQUEST, MIME_HTML, "Couldn't find temp probe: " + tempProbe);
        }

        if (BrewServer.getCurrentRecipe() == null) {
            LaunchControl.setMessage("No recipe selected");
            return new Response(Status.BAD_REQUEST, MIME_HTML, "No recipe selected?!");
        }

        if (profile.equalsIgnoreCase("mash")) {
            BrewServer.getCurrentRecipe().setMashProfile(temp);
        }

        if (profile.equalsIgnoreCase("boil")) {
            BrewServer.getCurrentRecipe().setBoilHops(temp);
        }

        if (profile.equalsIgnoreCase("ferm")) {
            BrewServer.getCurrentRecipe().setFermProfile(temp);
        }

        if (profile.equalsIgnoreCase("dry")) {
            BrewServer.getCurrentRecipe().setDryHops(temp);
        }
        LaunchControl.setMessage("Set " + profile + " profile for " + tempProbe);
        return new Response(Status.OK, MIME_HTML, "Set " + profile + " profile for " + tempProbe);
    }

    /**
     * Clear the notification.
     * @return A response object indicating whether the notification is cleared OK.
     */
    @UrlEndpoint(url="/clearnotification")
    public Response clearNotification() {
        String rInt = parameters.get("notification");
        if (rInt == null) {
            BrewServer.LOG.warning("No notification position supplied to clear");
            return new Response(Status.BAD_REQUEST, MIME_HTML, "No notification position supplied to clear");
        }

        try {
            int position = Integer.parseInt(rInt);
            if (!Notifications.getInstance().clearNotification(position)) {
                BrewServer.LOG.warning("Failed to clear notification: " + position);
                return new Response(Status.BAD_REQUEST, MIME_HTML, "Failed to clear notification: " + position);
            }
        } catch (NumberFormatException ne) {
            BrewServer.LOG.warning("No notification position supplied to clear: " + rInt);
            return new Response(Status.BAD_REQUEST, MIME_HTML, "Invalid notification position supplied to clear: " + rInt);
        }

        return new Response(Status.OK, MIME_HTML, "Cleared notification: " + rInt);
    }

    @UrlEndpoint(url="/resetrecorder")
    public Response resetRecorder() {
        LaunchControl.disableRecorder();
        LaunchControl.enableRecorder();
        return new Response(Status.OK, MIME_HTML, "Reset recorder.");
    }

    @UrlEndpoint(url="/deletegraphdata")
    public Response deleteGraphData() {
        if (LaunchControl.recorderEnabled) {
            StatusRecorder temp = LaunchControl.disableRecorder();
            Response response = temp.deleteAllData();
            LaunchControl.enableRecorder();
            return response;
        }
        return new Response("Recorder not enabled");
    }

    @UrlEndpoint(url="/deleteprobe")
    public Response deleteTempProbe() {
        String probeName = parameters.get("probe");
        Status status = Status.OK;
        JSONObject result = new JSONObject();
        if (probeName == null) {
            status = Status.BAD_REQUEST;
            result.put("failed", "No temp probe provided.");
        } else {
            Temp tempProbe = LaunchControl.findTemp(probeName);
            if (tempProbe == null) {
                status = Status.BAD_REQUEST;
                result.put("failed", "Could not find temp probe: " + tempProbe);
            } else {
                PID pid = LaunchControl.findPID(probeName);
                if (pid != null)
                {
                    LaunchControl.deletePID(pid);
                    result.put("PID", "Deleted");
                }
                LaunchControl.deleteTemp(tempProbe);
                result.put("Temp", "Deleted");
            }
        }
        Response response = new Response(result.toJSONString());
        response.setStatus(status);
        return response;
    }

    @UrlEndpoint(url="/shutdownSystem")
    public Response shutdownSystem() {
        try {
            boolean shutdownEverything = Boolean.parseBoolean(parameters.get("turnoff"));
            LaunchControl.saveSettings();
            LaunchControl.saveConfigFile();
            if (shutdownEverything) {
                // Shutdown the system using shutdown now
                Runtime runtime = Runtime.getRuntime();
                Process proc = runtime.exec("shutdown -h now");
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            BrewServer.LOG.warning("Failed to shutdown. " + e.getMessage());
        }

        return new Response("Shutdown called");
    }
}
