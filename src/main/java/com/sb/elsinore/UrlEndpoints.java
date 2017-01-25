package com.sb.elsinore;

import ca.strangebrew.recipe.Recipe;
import com.sb.common.CollectionsUtil;
import com.sb.elsinore.NanoHTTPD.Response;
import com.sb.elsinore.NanoHTTPD.Response.Status;
import com.sb.elsinore.annotations.Parameter;
import com.sb.elsinore.annotations.UrlEndpoint;
import com.sb.elsinore.html.PhSensorForm;
import com.sb.elsinore.html.RecipeListForm;
import com.sb.elsinore.html.RecipeViewForm;
import com.sb.elsinore.html.VolumeEditForm;
import com.sb.elsinore.inputs.PhSensor;
import com.sb.elsinore.notificiations.Notifications;
import com.sb.elsinore.recipes.BeerXMLReader;
import com.sb.elsinore.triggers.TriggerInterface;
import jGPIO.InvalidGPIOException;
import org.apache.tika.Tika;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.tools.PrettyWriter;

import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.elsinore.BrewServer.*;
import static com.sb.elsinore.NanoHTTPD.MIME_PLAINTEXT;
import static com.sb.elsinore.PID.HYSTERIA;
import static com.sb.elsinore.PID.MANUAL;
import static org.rendersnake.HtmlAttributesFactory.id;

@SuppressWarnings({"unchecked", "WeakerAccess"})
public class UrlEndpoints {


    public static final String HEAT = "heat";
    public static final String COOL = "cool";
    public static final String P = "p";
    public static final String I = "i";
    public static final String D = "d";
    public static final String CYCLETIME = "cycletime";

    public static final String NAME = "name";
    public static final String DURATION = "duration";
    public static final String INVERT = "invert";
    public static final String INPUTUNIT = "inputunit";
    public static final String FORM = "form";
    public static final String TOGGLE = "toggle";
    public static final String GPIO = "gpio";
    public static final String INVERTED = "inverted";
    public static final String VESSEL = "vessel";
    public static final String SENSOR = "sensor";
    public static final String DS_ADDRESS = "dsAddress";
    public static final String DS_OFFSET = "dsOffset";
    public static final String ADC_PIN = "adc_pin";
    public static final String I_2_C_MODEL = "i2c_model";
    public static final String I_2_C_DEVICE = "i2c_device";
    public static final String I_2_C_ADDRESS = "i2c_address";
    public static final String I_2_C_CHANNEL = "i2c_channel";
    public static final String VOLUME = "volume";
    public static final String UNITS = "units";
    public static final String ONEWIRE_ADDRESS = "onewire_address";
    public static final String ONEWIRE_OFFSET = "onewire_offset";
    public static final String I_2_C_TYPE = "i2c_type";
    public static final String PH_MODEL = "ph_model";
    public static final String CALIBRATION = "calibration";
    public static final String POSITION = "position";
    public static final String TYPE = "type";
    public static final String TEMP = "temp";
    public static final String TEMP_UNIT = "tempUnit";
    public static final String TEMPPROBE = "tempprobe";
    public static final String ORIGINALNAME = "originalname";
    public static final String ON_STRING = "on";
    public static final String OFF_STRING = "off";
    public static final String GRAVITY = "gravity";
    public static final String DUTYCYCLE = "dutycycle";
    public static final String ACTUAL_DUTY = "actualduty";
    public static final String SETPOINT = "setpoint";
    public static final String MANUAL_DUTY = "manual_duty";
    public static final String MANUAL_TIME = "manual_time";
    public static final String MODE = "mode";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String TIME = "time";
    public static final String COOLDELAY = "cooldelay";
    public static final String FILE = "file";
    public static final String SCALE = "scale";
    public static final String DEVICE = "device";
    public static final String RECORDER = "recorder";
    public static final String RECORDER_DIFF = "recorderDiff";
    public static final String RECORDER_TIME = "recorderTime";
    public static final String RESTORE = "restore";
    public static final String USE_OWFS = "use_owfs";
    public static final String OWFS_SERVER = "owfs_server";
    public static final String OWFS_PORT = "owfs_port";
    public static final String CELSIUS = "Celsius";
    public static final String C = "C";
    public static final String FAHRENHEIT = "Fahrenheit";
    public static final String F = "F";
    public static final String RECIPE_NAME = "recipeName";
    public static final String SUBSET = "subset";
    public static final String MASH = "mash";
    public static final String HOPS = "hops";
    public static final String FERMENTATION = "fermentation";
    public static final String DRY = "dry";
    public static final String PROFILE = "profile";
    public static final String BOIL = "boil";
    public static final String FERM = "ferm";
    public static final String NOTIFICATION = "notification";
    public static final String PROBE = "probe";
    public static final String TURNOFF = "turnoff";
    public static final String START = "start";
    public static final String RESET = "reset";
    public static final String TIMER = "timer";
    public static final String NEW_NAME = "new_name";
    public static final String NEW_HEAT_GPIO = "new_heat_gpio";
    public static final String NEW_COOL_GPIO = "new_cool_gpio";
    public static final String HEAT_INVERT = "heat_invert";
    public static final String COOL_INVERT = "cool_invert";
    public static final String AUX_GPIO = "aux_gpio";
    public static final String AUX_INVERT = "aux_invert";
    public static final String CUTOFF = "cutoff";
    public static final String CUTOFF_ENABLED = "cutoff_enabled";
    public static final String SIZE = "size";
    public static final String ADDRESS = "address";

    File rootDir;
    public Map<String, String> parameters = null;
    public Map<String, String> files = null;
    public Map<String, String> header = null;

    public static final String MIME_HTML = "text/html";

    public Map<String, String> getParameters() {
        return this.parameters;
    }

    private Map<String, String> ParseParams() {
        return RestEndpoints.ParseParams(this.getParameters());
    }


    @UrlEndpoint(url = "/addsystem", help = "Enable the system temperature probe",
            parameters = {})
    public final Response addSystemTempProbe() {
        LaunchControl.getInstance().addSystemTemp();
        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Added system temperature");
    }

    @UrlEndpoint(url = "/clearStatus", help = "Clear the current status message",
            parameters = {})
    public final Response clearStatus() {
        LaunchControl.setMessage("");
        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Status Cleared");
    }


    @UrlEndpoint(url = "/delsystem", help = "Disable the system temperature probe",
            parameters = {})
    public final Response delSystemStempProbe() {
        LaunchControl.getInstance().delSystemTemp();
        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Deleted system temperature");
    }

    @UrlEndpoint(url = "/getstatus", help = "Get the current status JSON",
            parameters = {})
    public final Response getStatus() {
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                LaunchControl.getInstance().getJSONStatus());
    }

    @UrlEndpoint(url = "/getsystemsettings", help = "Get the current system settings",
            parameters = {})
    public final Response getSystemSettings() {
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                LaunchControl.getInstance().getSystemStatus());
    }

    @UrlEndpoint(url = "/graph", help = "Get the current Graph file",
            parameters = {})
    public final Response getGraph() {
        return serveFile("/templates/static/graph/graph.html", this.header,
                this.rootDir);
    }

    @UrlEndpoint(url = "/checkgit", help = "Check for updates from GIT",
            parameters = {})
    public final Response checkForUpdates() {
        LaunchControl.getInstance().checkForUpdates();
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                "{Status:'OK'}");
    }

    @UrlEndpoint(url = "/restartupdate", help = "Update from GIT and restart",
            parameters = {})
    public final Response updateFromGit() {
        LaunchControl.updateFromGit();
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                "{Status:'OK'}");
    }

    @UrlEndpoint(url = "/settheme", help = "Set the current theme name",
            parameters = {@Parameter(name = "name", value = "The name of the theme to set")})
    public final Response setTheme() {
        String newTheme = this.parameters.get("name");

        if (newTheme == null) {
            return new NanoHTTPD.Response(Status.BAD_REQUEST,
                    MIME_TYPES.get("json"),
                    "{Status:'No name provided'}");
        }

        String fileName = "/logos/" + newTheme + ".ico";
        if (!(new File(this.rootDir, fileName).exists())) {
            // It doesn't exist
            LaunchControl.setMessage("Favicon for the new theme: "
                    + newTheme + ", doesn't exist."
                    + " Please add: " + fileName + " and try again");
            return new NanoHTTPD.Response(Status.BAD_REQUEST,
                    MIME_TYPES.get("json"),
                    "{Status:'Favicon doesn\'t exist'}");
        }

        fileName = "/logos/" + newTheme + ".gif";
        if (!(new File(this.rootDir, fileName).exists())) {
            // It doesn't exist
            LaunchControl.setMessage("Brewry image for the new theme: "
                    + newTheme + ", doesn't exist."
                    + " Please add: " + fileName + " and try again");
            return new NanoHTTPD.Response(Status.BAD_REQUEST,
                    MIME_TYPES.get("json"),
                    "{Status:'Brewery Image doesn\'t exist'}");
        }

        LaunchControl.getInstance().theme = newTheme;
        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("json"),
                "{Status:'OK'}");

    }

    /**
     * Parse the parameters to update the MashProfile.
     *
     * @return True if the profile is updated successfully.
     */
    @UrlEndpoint(url = "/triggerprofile", help = "Get the trigger profile for a temperature probe",
            parameters = {@Parameter(name = TEMPPROBE, value = "The Temperature probe to trigger the profile for.")})
    public final Response updateTriggerProfile() {
        String tempProbe = this.parameters.get(TEMPPROBE);
        if (tempProbe == null) {
            LaunchControl.setMessage("Could not trigger profile,"
                    + " no tempProbe provided");
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "FAILED");
        }
        Temp tProbe = LaunchControl.getInstance().findTemp(tempProbe);
        if (tProbe == null) {
            LaunchControl.setMessage("Could not trigger profile,"
                    + " no tempProbe provided");
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                    "FAILED");
        }
        TriggerControl triggerControl = tProbe.getTriggerControl();
        if (triggerControl.triggerCount() > 0) {
            triggerControl.activateTrigger(0);
            if (triggerControl.isActive()) {
                triggerControl.deactivate();
            }
        }

        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Updated MashProfile");
    }

    /**
     * Reorder the triggers steps.
     *
     * @return The Response object.
     */
    @UrlEndpoint(url = "/reordertriggers", help = "Re-order the triggers",
            parameters = {@Parameter(name = TEMPPROBE, value = "The temperature probe to re-order the triggers for"),
                    @Parameter(name = "<old position>", value = "<new position>")}
    )
    public final Response reorderMashProfile() {
        Map<String, String> params = this.parameters;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the order for the mash profile.");
        usage.put(TEMPPROBE,
                "The name of the TempProbe to change the mash profile order on.");
        usage.put(":old=:new", "The old position and the new position");
        // Resort the mash profile for the PID, then sort the rest
        String tempProbe = params.get(TEMPPROBE);
        // Do we have a PID coming in?
        if (tempProbe == null) {
            LaunchControl.setMessage(
                    "No Temp Probe supplied for trigger profile order sort");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }
        // Prevent any errors from the PID not being a number.
        params.remove(TEMPPROBE);
        // Do we have a mash control for the PID?
        TriggerControl mControl = LaunchControl.getInstance().findTriggerControl(tempProbe);
        if (mControl == null) {
            return null;
        }
        // Should be good to go, iterate and update!
        for (Map.Entry<String, String> mEntry : params.entrySet()) {
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
        LaunchControl.getInstance().saveEverything();
        return new Response(Status.OK, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    /**
     * Delete the specified trigger step.
     *
     * @return The Response.
     */
    @UrlEndpoint(url = "/deltriggerstep", help = "Delete the trigger step at a specific position",
            parameters = {@Parameter(name = DEVICE, value = "The name of the temperature probe to delete the trigger step from"),
                    @Parameter(name = POSITION, value = "The integer position of the trigger to delete from the profile")})
    public Response delTriggerStep() {
        Map<String, String> params = this.parameters;
        // Parse the response
        // Temp unit, PID, duration, temp, method, type, step number
        // Default to the existing temperature scale
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new mashstep to the specified PID");
        usage.put(DEVICE, "The PID to delete the mash step from");
        usage.put(POSITION, "The mash step to delete");
        Status status = Status.OK;

        try {
            String tempProbe = params.get(DEVICE);
            if (tempProbe == null) {
                LOG.warning("No device parameter supplied to delete trigger.");
            }
            int position = Integer.parseInt(params.get(POSITION));
            Temp temp = LaunchControl.getInstance().findTemp(tempProbe);
            if (temp == null) {
                return null;
            }
            TriggerControl mControl = temp.getTriggerControl();

            if (mControl != null) {
                mControl.delTriggerStep(position);
            } else {
                return null;
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
     *
     * @return True if set, false if there's an error
     */
    @UrlEndpoint(url = "/toggleTrigger", help = "Toggle the trigger profile on or off for a temp probe",
            parameters = {@Parameter(name = TEMPPROBE, value = "The temperature probe to toggle the trigger profile for"),
                    @Parameter(name = "status", value = "\"activate\" or \"deactivate\" to enable or disable the profile"),
                    @Parameter(name = POSITION, value = "The integer position of the step to activate or deactivate (starting from 0)")})
    public Response toggleTriggerProfile() {

        String tempProbe;
        if (this.parameters.containsKey(TEMPPROBE)) {
            tempProbe = this.parameters.get(TEMPPROBE);
        } else {
            LOG.warning("No Temp provided to toggle Trigger Profile");
            return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                    "Failed to toggle mash profile");
        }

        boolean activate = false;
        if (this.parameters.containsKey("status")) {
            if (this.parameters.get("status").equalsIgnoreCase("activate")) {
                activate = true;
            }
        } else {
            LOG.warning("No Status provided to toggle Profile");
            return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                    "Failed to toggle mash profile");
        }

        int position = -1;
        if (this.parameters.containsKey(POSITION)) {
            try {
                position = Integer.parseInt(this.parameters.get(POSITION));
            } catch (NumberFormatException e) {
                LOG.warning("Couldn't parse positional argument: "
                        + this.parameters.get(POSITION));
                return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                        "Failed to toggle mash profile");
            }
        }

        Temp temp = LaunchControl.getInstance().findTemp(tempProbe);
        if (temp == null) {
            return null;
        }
        TriggerControl mObj = temp.getTriggerControl();

        TriggerInterface triggerEntry = mObj.getCurrentTrigger();

        int stepToUse;
        // We have a mash step position
        if (position >= 0) {
            LOG.warning("Using mash step for " + tempProbe
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

        if (stepToUse >= 0) {
            mObj.activateTrigger(stepToUse);
            LOG.warning(
                    "Activated " + tempProbe + " step at " + stepToUse);
        } else {
            mObj.deactivateTrigger(stepToUse, true);
            LOG.warning("Deactivated " + tempProbe + " step at "
                    + stepToUse);
        }

        LaunchControl.getInstance().startMashControl(tempProbe);

        return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                "Toggled profile");
    }

    /**
     * Read the incoming parameters and edit the vessel as appropriate.
     *
     * @return True is success, false if failure.
     */
    @SuppressWarnings("ConstantConditions")
    @UrlEndpoint(url = "/editdevice", help = "Edit the main settings for a device",
            parameters = {@Parameter(name = "form", value = "The Address of the temperature probe to update"),
                    @Parameter(name = NEW_NAME, value = "The new name of the probe"),
                    @Parameter(name = NEW_HEAT_GPIO, value = "The Heat GPIO"),
                    @Parameter(name = NEW_COOL_GPIO, value = "The Cool GPIO"),
                    @Parameter(name = HEAT_INVERT, value = "\"on\" to enable inverted outputs for the heating pin"),
                    @Parameter(name = COOL_INVERT, value = "\"on\" to enable inverted outputs for the cooling pin"),
                    @Parameter(name = AUX_GPIO, value = "A pin to use for auxilliary output"),
                    @Parameter(name = CUTOFF, value = "A temperature value at which to shutdown Elsinore as a safety measure"),
                    @Parameter(name = CUTOFF_ENABLED, value = "On to use the cutoff temperature"),
                    @Parameter(name = CALIBRATION, value = "An offset for calibration")})
    public Response editVessel() {
        final Map<String, String> params = this.parameters;
        String auxpin, newName, heatgpio;
        String inputUnit = null, cutoff, coolgpio;
        String calibration;
        boolean heatInvert, coolInvert, auxInvert;
        int size = -1;

        Set<Entry<String, String>> incomingParams = params.entrySet();
        Map<String, String> parms;
        Iterator<Entry<String, String>> it = incomingParams.iterator();
        Entry<String, String> param;
        JSONObject incomingData = null;
        JSONParser parser = new JSONParser();

        // Try to Parse JSON Data
        while (it.hasNext()) {
            param = it.next();
            LOG.info("Key: " + param.getKey());
            LOG.info("Entry: " + param.getValue());
            try {
                Object parsedData = parser.parse(param.getValue());
                if (parsedData instanceof JSONArray) {
                    incomingData = (JSONObject) ((JSONArray) parsedData).get(0);
                } else {
                    incomingData = (JSONObject) parsedData;
                    inputUnit = param.getKey();
                }
            } catch (Exception e) {
                LOG.info("couldn't read " + param.getValue()
                        + " as a JSON Value " + e.getMessage());
            }
        }

        if (incomingData != null && inputUnit != null) {
            // Use the JSON Data
            LOG.info("Found valid data for " + inputUnit);
            parms = (Map<String, String>) incomingData;
        } else {
            inputUnit = params.get("form");
            parms = params;
        }

        // Fall back to the old style
        newName = parms.get(NEW_NAME);
        heatgpio = parms.get(NEW_HEAT_GPIO);
        coolgpio = parms.get(NEW_COOL_GPIO);
        heatInvert = parms.get(HEAT_INVERT) != null && Boolean.parseBoolean(params.get(HEAT_INVERT));
        coolInvert = parms.get(COOL_INVERT) != null && Boolean.parseBoolean(params.get(COOL_INVERT));
        auxpin = parms.get(AUX_GPIO);
        auxInvert = parms.get(AUX_INVERT) != null && Boolean.parseBoolean(params.get(AUX_INVERT));
        cutoff = parms.get(CUTOFF);
        boolean cutoffEnabled = parms.get(CUTOFF_ENABLED) != null && Boolean.parseBoolean(parms.get(CUTOFF_ENABLED));
        calibration = parms.get(CALIBRATION);

        if (parms.containsKey(SIZE)) {
            try {
                size = Integer.parseInt(parms.get(SIZE));
            } catch (NumberFormatException nfe) {
                LOG.warning("Couldn't parse: " + parms.get(SIZE) + " as an int.");
            }
        }

        if (isNullOrEmpty(inputUnit)) {
            inputUnit = parms.get(ADDRESS);
            if (inputUnit == null) {
                LOG.warning("No Valid input unit");
            }
        }
        inputUnit = inputUnit.replaceAll("_", "\\.");
        Temp tProbe = LaunchControl.getInstance().findTemp(inputUnit);
        PID tPID = LaunchControl.getInstance().findPID(inputUnit);

        if (tProbe == null) {
            inputUnit = inputUnit.replace("_", " ");
            tProbe = LaunchControl.getInstance().findTemp(inputUnit);
            tPID = LaunchControl.getInstance().findPID(inputUnit);
        }

        if (tProbe == null) {
            LaunchControl.setMessage("Couldn't find PID: " + inputUnit);
            return null;
        }

        if (tProbe != null && !isNullOrEmpty(newName)) {
            tProbe.setName(newName);
            LOG.warning("Updated temp name " + newName);
        }

        tProbe.cutoffEnabled = cutoffEnabled;
        tProbe.setCutoffTemp(cutoff);

        if (calibration != null) {
            tProbe.setCalibration(calibration);
        }
        tProbe.setSize(size);

        if (isNullOrEmpty(newName)) {
            newName = inputUnit;
        }

        if (tPID != null && !newName.equals("")) {
            tPID.setName(newName);
            LOG.warning("Updated PID Name" + newName);
        }

        if (tPID == null) {
            // No PID already, create one.
            tPID = new PID(tProbe.getName(), tProbe.getDevice(), heatgpio);
        }
        // HEATING GPIO
        if (isNullOrEmpty(heatgpio)) {
            tPID.getSettings(HEAT).setGPIO(null);
        }
        if (isNullOrEmpty(coolgpio)) {
            tPID.getSettings(COOL).setGPIO(null);
        }

        tPID.setAux(auxpin, auxInvert);

        LaunchControl lc = LaunchControl.getInstance();

        if (!isNullOrEmpty(heatgpio) || !isNullOrEmpty(coolgpio)) {
            PIDSettings heatSettings = tPID.getSettings(HEAT);
            if (!heatgpio.equals(tPID.getHeatGPIO())) {
                // We have a PID, set it to the new value
                heatSettings.setGPIO(heatgpio);

            }
            heatSettings.setInverted(heatInvert);

            PIDSettings coolSettings = tPID.getSettings(COOL);
            if (!coolgpio.equals(tPID.getCoolGPIO())) {
                // We have a PID, set it to the new value
                coolSettings.setGPIO(coolgpio);
            }
            coolSettings.setInverted(coolInvert);
            lc.addTemp(tPID);
        }
        if (heatgpio.equals("") && coolgpio.equals("")) {
            lc.deleteTemp(tPID);
            // Downgrade to a temp probe
            Temp temp = new Temp(tPID.getName(), tPID.getProbe());
            lc.addTemp(temp);
        }

        // Update the heat settings
        parseAndUpdatePidSettings(tPID, params, HEAT);

        // update the cool settings
        parseAndUpdatePidSettings(tPID, params, COOL);

        lc.saveConfigFile();
        return new Response(Status.OK, MIME_TYPES.get("txt"),
                "PID Updated");
    }

    private void parseAndUpdatePidSettings(PID pid, Map<String, String> params, String type) {
        String temp = params.get(type + "[" + P + "]");
        BigDecimal newValue = stringToBigDecimal(temp);
        if (newValue != null) {
            pid.getSettings(type).setProportional(newValue);
        } else {
            LOG.warning(String.format("Failed to parse %s Derivative as a number: %s", type, temp));
        }

        temp = params.get(type + "[" + I + "]");
        newValue = stringToBigDecimal(temp);
        if (newValue != null) {
            pid.getSettings(type).setIntegral(newValue);
        } else {
            LOG.warning(String.format("Failed to parse %s Integral as a number: %s", type, temp));
        }

        temp = params.get(type + "[" + D + "]");
        newValue = stringToBigDecimal(temp);
        if (newValue != null) {
            pid.getSettings(type).setDerivative(newValue);
        } else {
            LOG.warning(String.format("Failed to parse %s Derivative as a number: %s", type, temp));
        }

        temp = params.get(type + "[" + CYCLETIME + "}");
        newValue = stringToBigDecimal(temp);
        if (newValue != null) {
            pid.getSettings(type).setCycleTime(newValue);
        } else {
            LOG.warning(String.format("Failed to parse %s Cycle Time as a number: %s", type, temp));
        }

    }

    /**
     * Add a new timer to the brewery.
     *
     * @return True if added ok
     */
    @UrlEndpoint(url = "/addtimer", help = "Add a new timer to the system",
            parameters = {@Parameter(name = "name", value = "The Name of the timer to create"),
                    @Parameter(name = DURATION, value = "The duration of the timer"),
                    @Parameter(name = INVERT, value = "True to invert the timer (count down), false to count up")})
    public final Response addTimer() {
        String newName;

        Map<String, String> parms = RestEndpoints.ParseParams(this.parameters);

        // Fall back to the old style
        newName = parms.get(NAME);
        if (newName == null || newName.trim().length() == 0) {
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("txt"), "No name provided for the timer");
        }
        String duration = parms.get(DURATION);
        boolean inverted = Boolean.parseBoolean(parms.get(INVERT));
        LaunchControl lc = LaunchControl.getInstance();
        Timer timer = lc.findTimer(newName);

        if (timer == null) {
            if (!lc.addTimer(newName, duration)) {
                return new Response(Status.BAD_REQUEST, MIME_TYPES.get("txt"), "Failed to create timer");
            }
            timer = lc.findTimer(newName);
            if (timer == null) {
                return new Response(Status.BAD_REQUEST, MIME_TYPES.get("txt"), "Failed to find timer");
            }
            timer.setInverted(inverted);
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Timer created");
        } else {
            timer.setInverted(inverted);
            timer.setTarget(duration);
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Timer Updated");
        }

    }

    /**
     * Add a new switch to the brewery.
     *
     * @return True if added ok
     */
    @UrlEndpoint(url = "/addswitch", help = "Add a new switch to the brewery",
            parameters = {@Parameter(name = NAME, value = "The name of the switch to add"),
                    @Parameter(name = GPIO, value = "The GPIO to control with this switch"),
                    @Parameter(name = INVERT, value = "\"on\" to enable inversion of the outputs, anything else for normal output.")})
    public Response addSwitch() {
        String newName = "", gpio = "", originalName = "";
        boolean invert = false;

        Map<String, String> parms = RestEndpoints.ParseParams(this.parameters);

        // Fall back to the old style
        if (parms.containsKey(ORIGINALNAME)) {
            originalName = parms.get(ORIGINALNAME);
        }

        if (parms.containsKey(NAME)) {
            newName = parms.get(NAME);
        }

        if (parms.containsKey(GPIO)) {
            gpio = parms.get(GPIO);
        }

        if (parms.containsKey(INVERT)) {
            String i = parms.get(INVERT);
            invert = i.equalsIgnoreCase(ON_STRING) || !i.equalsIgnoreCase(OFF_STRING) && Boolean.parseBoolean(parms.get(INVERT));
        }

        Switch aSwitch;
        LaunchControl lc = LaunchControl.getInstance();
        if (originalName != null && originalName.length() > 0) {
            aSwitch = LaunchControl.getInstance().findSwitch(originalName);
            if (aSwitch != null) {
                aSwitch.setName(newName);
                try {
                    aSwitch.setGPIO(gpio);
                } catch (InvalidGPIOException ignored) {
                }
            } else {
                aSwitch = lc.addSwitch(newName, gpio);
            }
        } else {
            aSwitch = lc.addSwitch(newName, gpio);
        }
        if (aSwitch != null) {

            aSwitch.setInverted(invert);
            lc.saveEverything();
            return new Response(Status.OK, MIME_TYPES.get("txt"), "Switch Added");
        } else {
            LaunchControl.setMessage(
                    "Could not add switch " + newName + ": " + gpio);
        }

        JSONObject usage = new JSONObject();
        usage.put("Usage", "Add a new switch to the system");
        usage.put(NAME, "The name of the switch to add");
        usage.put(GPIO, "The GPIO for the switch to work on");
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
    @UrlEndpoint(url = "/updatepid", help = "The big one, update the PID parameters",
            parameters = {@Parameter(name = INPUTUNIT, value = "The PID to change settings for"),
                    @Parameter(name = DUTYCYCLE, value = "The Duty cycle to set (between -100 and +100)"),
                    @Parameter(name = CYCLETIME, value = "The manual cycle time to use (in seconds)"),
                    @Parameter(name = SETPOINT, value = "The target temperature for automatic mode."),
                    @Parameter(name = HEAT, value = "The New settings for the heating PID"),
                    @Parameter(name = COOL, value = "The new settings for the cooling PID"),
                    @Parameter(name = MODE, value = "The mode to use \"off\" \"auto\" \"manual\" \"hysteria\""),
                    @Parameter(name = MIN, value = "The minimum temperature to turn on the heating output, or turn off the cooling output in hysteria mode"),
                    @Parameter(name = MAX, value = "The maximum temperature to turn on the cooling output, or turn off the heating output in hysteria mode"),
                    @Parameter(name = TIME, value = "The minimum amount of time to turn the cooling/heating output on or off for")})
    public Response updatePID() {
        String temp, mode = OFF_STRING;
        BigDecimal duty = null, setpoint = null,
                min = null, max = null, time = null, cycle = null;

        JSONObject sub_usage = new JSONObject();
        Map<String, String> parms = RestEndpoints.ParseParams(this.parameters);
        String inputUnit = parms.get(INPUTUNIT);
        boolean errorValue = false;
        PID tPID = LaunchControl.getInstance().findPID(inputUnit);
        if (tPID == null) {
            LOG.warning("Couldn't find PID: " + inputUnit);
            LaunchControl.setMessage("Could not find PID: " + inputUnit);
            LaunchControl.setMessage("Bad inputs when updating. Please check the system log");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"), "");
        }

        // Fall back to the old style
        sub_usage.put(DUTYCYCLE, "The new duty cycle % to set");
        if (parms.containsKey(DUTYCYCLE)) {
            temp = parms.get(DUTYCYCLE);
            duty = stringToBigDecimal(temp);
            if (duty == null) {
                LOG.warning(String.format("Failed to parse duty: %s", temp));
                errorValue = true;
            } else {
                LOG.info(String.format("New Duty Cycle: %s", duty));
            }
        }

        sub_usage.put(SETPOINT, "The new target temperature to set");
        if (parms.containsKey(SETPOINT)) {
            temp = parms.get(SETPOINT);
            setpoint = stringToBigDecimal(temp);
            if (setpoint == null) {
                LOG.warning(String.format("Failed to parse Setpoint: %s", temp));
                errorValue = true;
            } else {
                LOG.info(String.format("New Setpoint: %s", setpoint));
            }
        }


        sub_usage.put(CYCLETIME, "The new manual cycle time in seconds to set");
        if (parms.containsKey(CYCLETIME)) {
            temp = parms.get(CYCLETIME);
            cycle = stringToBigDecimal(temp);
            if (cycle == null) {
                LOG.warning(String.format("Failed to read Cycle Time: %s", temp));
                errorValue = true;
            }
        }

        sub_usage.put(MODE, "The new mode to set");
        if (parms.containsKey(MODE)) {
            mode = parms.get(MODE);
            LOG.info("Mode: " + mode);
        }

        sub_usage.put(MIN, "The minimum temperature to enable the output (HYSTERIA)");
        if (parms.containsKey(MIN)) {
            temp = parms.get(MIN);
            min = stringToBigDecimal(temp);
            if (min == null) {
                LOG.warning(String.format("Failed to parse new minimum value: %s", temp));
                errorValue = true;
            } else {
                LOG.info(String.format("New minimum hysteria value: %s", min));
            }
        }

        sub_usage.put(MAX, "The maximum temperature to disable the output (HYSTERIA)");
        if (parms.containsKey(MAX)) {
            temp = parms.get(MAX);
            max = stringToBigDecimal(temp);

            if (max == null) {
                LOG.warning(String.format("Failed to parse Maximum Temperature: %s", temp));
                errorValue = true;
            } else {
                LOG.info(String.format("New maximum hysteria value: %s", max));
            }
        }

        sub_usage.put(TIME, "The minimum time when enabling the output (HYSTERIA)");
        if (parms.containsKey(TIME)) {
            temp = parms.get(TIME);
            time = stringToBigDecimal(temp);
            if (time == null) {
                LOG.warning(String.format("Failed to parse minimum hysteria Time: %s", temp));
                errorValue = true;
            } else {
                LOG.info(String.format("New minimum hysteria Time: %s", time));
            }
        }

        LOG.info("Form: " + inputUnit);

        JSONObject usage = new JSONObject();
        usage.put(":PIDname", sub_usage);

        if (errorValue) {
            LaunchControl.setMessage("Bad inputs when updating. Please check the system log");
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"),
                    usage.toJSONString());
        }

        if (mode.equalsIgnoreCase(HYSTERIA)) {
            tPID.setHysteria(min, max, time);
            tPID.useHysteria();
        } else {
            LOG.info(mode + ":" + duty + ":" + setpoint);
            tPID.setPidMode(mode);
            tPID.setDuty(duty);
            tPID.setTemp(setpoint);
            if (parms.containsKey(HEAT)) {
                parseAndUpdatePidSettings(tPID, parms, HEAT);
            }
            if (parms.containsKey(COOL)) {
                parseAndUpdatePidSettings(tPID, parms, COOL);
            }

            if (mode.equalsIgnoreCase(MANUAL)) {
                tPID.setManualDuty(duty);
                tPID.setManualTime(cycle);
            }
        }
        return new Response(Status.OK, MIME_HTML, "PID " + inputUnit
                + " updated");
    }

    private BigDecimal stringToBigDecimal(String temp) {
        if (isNullOrEmpty(temp)) {
            return null;
        }
        try {
            return new BigDecimal(temp.replace(",", "."));
        } catch (NumberFormatException nfe) {
            LOG.warning(String.format("Couldn't parse %s as a number", temp));
        }
        return null;
    }

    /**
     * Add a new data point for the volume reading.
     *
     * @return A NanoHTTPD response
     */
    @UrlEndpoint(url = "/addvolpoint", help = "Add a volume data point to the vessel",
            parameters = {
                    @Parameter(name = NAME, value = "The temperature probe to add a volume data point to"),
                    @Parameter(name = UNITS, value = "The volume units to use for setup (Only required when setting up"),
                    @Parameter(name = VOLUME, value = "The volume that is in the vessel at the time of reading"),
                    @Parameter(name = ONEWIRE_ADDRESS, value = "The OneWire DS2450 Address to use for the readings"),
                    @Parameter(name = ONEWIRE_OFFSET, value = "The OneWire DS2450 offset (A|B|C|D) to use for the readings"),
                    @Parameter(name = ADC_PIN, value = "The onboard AIN pin to use"),
                    @Parameter(name = I_2_C_ADDRESS, value = "The I2C Address to use for the Analogue readings"),
                    @Parameter(name = I_2_C_DEVICE, value = "The I2C Device (integer) to use for the i2c_address"),
                    @Parameter(name = I_2_C_CHANNEL, value = "The channel on the I2C ADC to use"),
                    @Parameter(name = I_2_C_TYPE, value = "The type of the I2C ADC chip."),
            })

    public Response addVolumePoint() {
        Map<String, String> parms = ParseParams();
        JSONObject usage = new JSONObject();
        usage.put(NAME, "Temperature probe name to add a volume point to");
        usage.put(VOLUME,
                "Volume that is in the vessel to add a datapoint for");
        usage.put(UNITS,
                "The Volume units to use (only required when setting up)");
        usage.put(ONEWIRE_ADDRESS,
                "The one wire address to be used for analogue reads");
        usage.put(ONEWIRE_OFFSET,
                "The one wire offset to be used for analogue reads");
        usage.put(ADC_PIN, "The ADC Pin to be used for analogue reads");

        String name = parms.get(NAME);
        String volume = parms.get(VOLUME);
        String units = parms.get(UNITS);

        String onewire_add = parms.get(ONEWIRE_ADDRESS);
        String onewire_offset = parms.get(ONEWIRE_OFFSET);
        String adc_pin = parms.get(ADC_PIN);
        String i2c_address = parms.get(I_2_C_ADDRESS);
        String i2c_device = parms.get(I_2_C_DEVICE);
        String i2c_channel = parms.get(I_2_C_CHANNEL);
        String i2c_type = parms.get(I_2_C_TYPE);
        String error_msg;

        if (name == null || volume == null) {
            error_msg = "No name or volume supplied";
            usage.put("Error", "Invalid parameters: " + error_msg);
            return new Response(Response.Status.BAD_REQUEST,
                    MIME_TYPES.get("json"), usage.toJSONString());
        }

        // We have a name and volume, lookup the temp probe
        Temp t = LaunchControl.getInstance().findTemp(name);
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
            if (!isNullOrEmpty(adc_pin)) {
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

            } else if (!isNullOrEmpty(onewire_add) && !isNullOrEmpty(onewire_offset)) {
                // Setup Onewire pin
                if (!t.setupVolumes(onewire_add, onewire_offset, units)) {
                    error_msg = "Could not setup volumes for " + onewire_add
                            + " offset: " + onewire_offset + " Units: " + units;
                    usage.put("Error", "Invalid parameters: " + error_msg);
                    return new Response(Response.Status.BAD_REQUEST,
                            MIME_TYPES.get("json"), usage.toJSONString());
                }
            } else if (!isNullOrEmpty(i2c_address) && !isNullOrEmpty(i2c_channel) && !isNullOrEmpty(i2c_device)) {
                if (t.setupVolumeI2C(i2c_device, i2c_address, i2c_channel, i2c_type, units)) {
                    error_msg = "Could not setup volumes for " + i2c_device
                            + " address: " + i2c_address + " Units: " + units;
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
     *
     * @return the JSON Response data
     */
    @UrlEndpoint(url = "/graph-data.zip", help = "Downloading data as a zip",
            parameters = {})
    public NanoHTTPD.Response getGraphDataZipWrapper() {
        return getGraphData();
    }

    @UrlEndpoint(url = "/graph-data", help = "Get the graph data as JSON", parameters = {})
    public NanoHTTPD.Response getGraphDataWrapper() {
        return getGraphData();
    }

    @UrlEndpoint(url = "/graph-data/", help = "Get the graph data as JSON", parameters = {})
    public NanoHTTPD.Response getGraphData() {
        LaunchControl lc = LaunchControl.getInstance();
        if (!lc.recorderEnabled()) {
            return new NanoHTTPD.Response("Recorder disabled");
        }

        Map<String, String> parms = ParseParams();
        return lc.getRecorder().getData(parms);
    }

    /**
     * Read the incoming parameters and update the name as appropriate.
     *
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/setbreweryname", help = "Set the brewery name",
            parameters = {@Parameter(name = NAME, value = "The name of the brewery to set")})
    public Response updateBreweryName() {
        Map<String, String> parms = ParseParams();
        String newName = parms.get(NAME);

        if (!isNullOrEmpty(newName)) {
            LaunchControl.getInstance().setName(newName);
            return new Response(Response.Status.OK, MIME_TYPES.get("text"), "Updated");
        }

        return null;

    }

    /**
     * Read the incoming parameters and update the name as appropriate.
     *
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/uploadimage", help = "Upload an image for the brewery logo",
            parameters = {@Parameter(name = FILE, value = "Push a file to this EndPoint to upload")})
    public Response updateBreweryImage() {

        final Map<String, String> files = this.files;

        if (files.size() == 1) {
            for (Map.Entry<String, String> entry : files.entrySet()) {

                try {
                    File uploadedFile = new File(entry.getValue());
                    String fileType = Files.probeContentType(
                            uploadedFile.toPath());

                    if (fileType.equalsIgnoreCase(MIME_TYPES.get("gif"))
                            || fileType.equalsIgnoreCase(MIME_TYPES.get("jpg"))
                            || fileType.equalsIgnoreCase(MIME_TYPES.get("jpeg"))
                            || fileType.equalsIgnoreCase(MIME_TYPES.get("png"))) {
                        File targetFile = new File(this.rootDir, "brewerImage.gif");
                        if (targetFile.exists() && !targetFile.delete()) {
                            LOG.warning("Failed to delete " + targetFile.getCanonicalPath());
                        }
                        LaunchControl.copyFile(uploadedFile, targetFile);

                        if (!targetFile.setReadable(true, false) || targetFile.setWritable(true, false)) {
                            LOG.warning("Failed to set read write permissions on " + targetFile.getCanonicalPath());
                        }

                        LaunchControl.getInstance().setFileOwner(targetFile);
                        return new Response(Response.Status.OK, MIME_TYPES.get("text"), "Updated brewery logo");
                    }
                } catch (IOException e) {
                }
            }
        }
        return null;

    }

    /**
     * Update the switch order.
     *
     * @return A HTTP Response
     */
    @UrlEndpoint(url = "/updateswitchorder", help = "Reorder the switches",
            parameters = {@Parameter(name = "<name of the switch>", value = "New integer position of the switch")})
    public Response updateSwitchOrder() {
        Map<String, String> params = ParseParams();
        Status status;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }
            Switch tSwitch = LaunchControl.getInstance().findSwitch(entry.getKey());
            // Make Sure we're aware of this switch
            if (tSwitch == null) {
                LaunchControl.setMessage(
                        "Couldn't find Switch: " + entry.getKey());
                return null;
            }

            try {
                LaunchControl.getInstance().switchList.remove(tSwitch);
                tSwitch.setPosition(Integer.parseInt(entry.getValue()));
                CollectionsUtil.addInOrder(LaunchControl.getInstance().switchList, tSwitch);
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage("Couldn't parse " + entry.getValue() + " as an integer");
                return null;
            }
            status = Response.Status.OK;
            return new Response(status,
                    MIME_TYPES.get("text"), "Reordered");
        }
        return null;
    }

    /**
     * Delete a switch.
     *
     * @return a reponse
     */
    @UrlEndpoint(url = "/deleteswitch", help = "Delete a switch",
            parameters = {@Parameter(name = NAME, value = "The name of the switch to delete")})
    public Response deleteSwitch() {
        Map<String, String> params = ParseParams();
        Status status = Response.Status.OK;

        // find the switch
        String switchName = params.get(NAME);
        if (switchName != null) {
            LaunchControl.getInstance().deleteSwitch(switchName);
            return new Response(status, MIME_TYPES.get("text"),
                    "Deleted");
        }
        return null;
    }

    /**
     * Update the timer order.
     *
     * @return A HTTP Response
     */
    @UrlEndpoint(url = "/updatetimerorder", help = "Re-order the timers",
            parameters = {@Parameter(name = "<name of timer>", value = "<Integer position of the timer>")})
    public Response updateTimerOrder() {
        Map<String, String> params = ParseParams();

        if (params.size() == 0) {
            return null;
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }

            Timer tTimer = LaunchControl.getInstance().findTimer(entry.getKey());
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
                return null;
            }
        }
        LaunchControl.getInstance().sortTimers();
        return new Response(Response.Status.OK,
                MIME_TYPES.get("text"), "Updated timer order");
    }

    /**
     * Delete a timer.
     *
     * @return a response
     */
    @UrlEndpoint(url = "/deletetimer", help = "Delete a timer",
            parameters = {@Parameter(name = "name", value = "The name of the timer to delete")})
    public Response deleteTimer() {
        Map<String, String> params = ParseParams();
        Status status = Response.Status.OK;

        // find the switch
        String timerName = params.get("name");
        if (timerName != null) {
            LaunchControl.getInstance().deleteTimer(timerName);
        } else {
            return null;
        }
        return new Response(status, MIME_TYPES.get("text"),
                "Deleted timer " + timerName);
    }

    @UrlEndpoint(url = "/setscale", help = "Set the temperature scale",
            parameters = {@Parameter(name = SCALE, value = "The new scale \"F\" or \"C\"")})
    public Response setScale() {
        Map<String, String> params = ParseParams();

        Status status = Response.Status.OK;

        // Iterate all the temperature probes and change the scale
        if (!LaunchControl.getInstance().setTempScales(params.get(SCALE))) {
            return null;
        }

        return new Response(status, MIME_TYPES.get("text"),
                "Scale set to " + params.get(SCALE));
    }

    @UrlEndpoint(url = "/toggledevice", help = "Toggle the visibility of a device",
            parameters = {@Parameter(name = DEVICE, value = "The name of the device to toggle the visibility of")})
    public Response toggleDevice() {
        Map<String, String> params = ParseParams();

        Status status = Response.Status.OK;
        Temp temp = LaunchControl.getInstance().findTemp(params.get(DEVICE));

        if (temp != null) {
            temp.toggleVisibility();
        } else {
            return null;
        }

        return new Response(status, MIME_TYPES.get("text"),
                "Device " + temp.getName() + " is toggled");
    }

    @UrlEndpoint(url = "/updatesystemsettings", help = "Update the system settings",
            parameters = {
                    @Parameter(name = RECORDER, value = "\"on\" to enable the data recorder, \"off\" to disable it"),
                    @Parameter(name = RECORDER_DIFF, value = "Decimal that represents the tolerance to use when recording data, when the temperature varies by this amount from the previous point, record it"),
                    @Parameter(name = RECORDER_TIME, value = "The sample rate in milliseconds"),
                    @Parameter(name = RESTORE, value = "\"on\" to restore the previous state of Elsinore on startup."),
                    @Parameter(name = USE_OWFS, value = "Enable or disable OWFS"),
                    @Parameter(name = OWFS_SERVER, value = "OWFS server IP"),
                    @Parameter(name = OWFS_PORT, value = "OWFS port"),
                    @Parameter(name = SCALE, value = "Scale in Celsius or Fahrenheit"),
            })
    public final Response updateSystemSettings() {
        Map<String, String> params = ParseParams();
        LaunchControl lc = LaunchControl.getInstance();

        if (params.containsKey(SCALE)) {
            String scale = params.get(SCALE);
            if (scale.equalsIgnoreCase(CELSIUS) || scale.equalsIgnoreCase(C)) {
                lc.setTempScales(C);
            } else if (scale.equalsIgnoreCase(FAHRENHEIT) || scale.equalsIgnoreCase(F)) {
                lc.setTempScales(F);
            }
        }

        if (params.containsKey(RECORDER)) {
            boolean recorderOn = params.get(RECORDER).equals(ON_STRING);
            // If we're on, disable the recorder
            if (recorderOn) {
                lc.enableRecorder();
            } else {
                lc.disableRecorder();
            }
        } else {
            lc.disableRecorder();
        }

        if (params.containsKey(RECORDER_DIFF)) {
            try {
                lc.getRecorder().setDiff(Double.parseDouble(params.get(RECORDER_DIFF)));
            } catch (Exception e) {
                LaunchControl.setMessage(
                        "Failed to parse Recorder diff as a double\n"
                                + e.getMessage()
                                + LaunchControl.getMessage());
            }
        }

        if (params.containsKey(RECORDER_TIME)) {
            try {
                lc.getRecorder().setTime(Long.parseLong(params.get(RECORDER_TIME)));
            } catch (Exception e) {
                LaunchControl.setMessage(
                        "Failed to parse Recorder time as a long\n" + e.getMessage()
                                + LaunchControl.getMessage());
            }
        }

        if (params.containsKey(RESTORE)) {
            lc.setRestore(params.get(RESTORE).equals(ON_STRING));
        }

        if (params.containsKey(USE_OWFS)) {
            lc.systemSettings.useOWFS = params.get(USE_OWFS).equals(ON_STRING);
        }

        if (params.containsKey(OWFS_SERVER)) {
            lc.systemSettings.owfsServer = params.get(OWFS_SERVER);
        }
        if (params.containsKey(OWFS_PORT)) {
            try {
                lc.systemSettings.owfsPort = Integer.parseInt(params.get(OWFS_PORT));
            } catch (NumberFormatException nfe) {
                LOG.log(Level.WARNING, "Failed to parse " + params.get(OWFS_PORT), nfe);
            }
        }

        lc.setupOWFS();
        lc.listOWFSTemps();

        return new NanoHTTPD.Response(Status.OK, MIME_TYPES.get("text"),
                "Updated system");
    }

    /**
     * Set the gravity for the specified device.
     *
     * @return A response
     */
    @UrlEndpoint(url = "/setgravity", help = "Set the gravity reading for a device to improve the volume calculation",
            parameters = {
                    @Parameter(name = DEVICE, value = "The Device to set the gravity for"),
                    @Parameter(name = GRAVITY, value = "The  new specific gravity value")
            })
    public final Response setGravity() {
        Map<String, String> params = ParseParams();
        Status status = Status.OK;

        Temp temp = null;

        if (params.containsKey(INPUTUNIT)) {
            temp = LaunchControl.getInstance().findTemp(params.get(INPUTUNIT));
            if (temp == null) {
                LaunchControl.addMessage("Could not find the temperature probe for: " + params.get(INPUTUNIT));
                status = Status.BAD_REQUEST;
            }
        } else {
            LaunchControl.addMessage("No device provided when setting the gravity");
            status = Status.BAD_REQUEST;
        }

        if (params.containsKey(GRAVITY)) {
            try {
                BigDecimal gravity = new BigDecimal(params.get(GRAVITY));
                if (temp != null) {
                    temp.setGravity(gravity);
                }
            } catch (NumberFormatException nfe) {
                LaunchControl.addMessage("Could not parse gravity as a decimal: " + params.get(GRAVITY));
            }
        }

        if (status == Status.BAD_REQUEST) {
            return null;
        }
        return new Response(status, MIME_TYPES.get("text"),
                "Updated");
    }

    @UrlEndpoint(url = "/controller", help = "Get the main controller page HTML",
            parameters = {})
    public Response renderController() {
        return serveFile("html/index.html", this.header, this.rootDir);
    }


    @UrlEndpoint(url = "/brewerImage.gif", help = "Get the current brewery logo",
            parameters = {})
    public Response getBrewerImage() {
        String uri = "/brewerImage.gif";
        LaunchControl lc = LaunchControl.getInstance();
        // Has the user uploaded a file?
        if (new File(this.rootDir, uri).exists()) {
            return serveFile(uri, this.header, this.rootDir);
        }
        // Check to see if there's a theme set.
        if (lc.theme != null
                && !lc.theme.equals("")) {
            if (new File(this.rootDir,
                    "/logos/" + lc.theme + ".gif").exists()) {
                return serveFile("/logos/" + lc.theme + ".gif",
                        this.header, this.rootDir);
            }
        }

        return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_TYPES.get("json"), "{image: \"unavailable\"}");
    }

    @UrlEndpoint(url = "/updateswitch", help = "Toggle a switch on or off",
            parameters = {@Parameter(name = TOGGLE, value = "The name of the switch to toggle")})
    public Response updateSwitch() {

        if (this.parameters.containsKey(TOGGLE)) {
            String switchName = this.parameters.get(TOGGLE);
            Switch tempSwitch =
                    LaunchControl.getInstance().findSwitch(switchName);
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
                usage.put("Error", "Invalid name supplied: " + switchName);
                usage.put(TOGGLE, "The name of the Switch to toggle on/off");
                return new Response(Status.BAD_REQUEST, MIME_TYPES.get("json"), usage.toJSONString());
            }
        }

        return new Response(Status.BAD_REQUEST, MIME_TYPES.get("txt"), "Invalid data" + " provided.");
    }

    @UrlEndpoint(url = "/getswitchsettings", help = "Get the settings for a switch",
            parameters = {@Parameter(name = NAME, value = "The name of the switch to get the details for")})
    public Response switchSettings() {
        Status status = Status.BAD_REQUEST;
        String data = "";
        if (this.parameters.containsKey(NAME)) {
            String switchName = this.parameters.get(NAME);
            Switch tempSwitch =
                    LaunchControl.getInstance().findSwitch(switchName.replaceAll("_", " "));
            if (tempSwitch != null) {
                JSONObject values = new JSONObject();
                values.put(NAME, tempSwitch.getName());
                values.put(GPIO, tempSwitch.getGPIO());
                values.put(INVERTED, tempSwitch.getInverted());
                data = values.toJSONString();
                status = Status.OK;
            }
        }
        return new Response(status, MIME_TYPES.get("json"), data);
    }

    @UrlEndpoint(url = "/toggleaux", help = "Toggle the aux pin for a PID",
            parameters = {@Parameter(name = TOGGLE, value = "The name of the PID to toggle the pin for")})
    public Response toggleAux() {
        JSONObject usage = new JSONObject();
        if (this.parameters.containsKey(TOGGLE)) {
            String pidname = this.parameters.get(TOGGLE);
            PID tempPID = LaunchControl.getInstance().findPID(pidname);
            if (tempPID != null) {
                tempPID.toggleAux();
                return new NanoHTTPD.Response(Status.OK, MIME_HTML, "Updated Aux for " + pidname);
            } else {
                LOG.warning("Invalid PID: " + pidname + " provided.");
                usage.put("Error", "Invalid name supplied: " + pidname);
            }
        }

        usage.put("toggle", "The name of the PID to toggle the aux output for");
        return new Response(usage.toJSONString());
    }

    @UrlEndpoint(url = "/getvolumeform", help = "Get the volume update form for the specified vessel",
            parameters = {
                    @Parameter(name = VESSEL, value = "The name of the vessel to get the Volume form for")
            })
    public Response getVolumeForm() {
        if (!this.parameters.containsKey(VESSEL)) {
            LaunchControl.setMessage("No Vessel provided");
            return null;
        }

        // Check to make sure we have a valid vessel
        Temp temp = LaunchControl.getInstance().findTemp(this.parameters.get(VESSEL));
        if (temp == null) {
            return null;
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

    @UrlEndpoint(url = "/getphsensorform", help = "Get the HTML form for a pH Sensor",
            parameters = {
                    @Parameter(name = SENSOR, value = "The name of the sensor to get the pH Edit form for")
            })
    public Response getPhSensorForm() {
        PhSensor phSensor;

        if (!this.parameters.containsKey(SENSOR)) {
            phSensor = new PhSensor();
        } else {
            phSensor = LaunchControl.getInstance().findPhSensor(this.parameters.get(SENSOR));
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
            return null;
        }
        return new Response(Status.OK, MIME_HTML, result);
    }

    @UrlEndpoint(url = "/addphsensor", help = "Add or update a pH Sensor",
            parameters = {
                    @Parameter(name = NAME, value = "The name of the pH Sensor to edit"),
                    @Parameter(name = DS_ADDRESS, value = "The DS2450 Address to use for reading the pH Sensor Analogue value"),
                    @Parameter(name = DS_OFFSET, value = "The DS2450 Offset to use for reading the pH Sensor Analogue value"),
                    @Parameter(name = ADC_PIN, value = "The onboard ADC AIN pin to use for reading the pH Sensor Analogue value"),
                    @Parameter(name = I_2_C_MODEL, value = "The I2C ADC model that's used for reading the pH Sensor Analogue value"),
                    @Parameter(name = I_2_C_DEVICE, value = "The I2C device that's used for reading the pH Sensor Analogue value"),
                    @Parameter(name = I_2_C_ADDRESS, value = "The I2C ADC Address that's used for reading the pH Sensor Analogue value"),
                    @Parameter(name = I_2_C_CHANNEL, value = "The I2C ADC channel that's used for reading the pH Sensor Analogue value"),
                    @Parameter(name = PH_MODEL, value = "The pH sensor model that being used"),
                    @Parameter(name = CALIBRATION, value = "The offset for calibration")

            })
    public Response addPhSensor() {
        PhSensor phSensor;
        String result = "";
        Map<String, String> localParams = this.ParseParams();
        String sensorName = null;
        if (localParams.containsKey(NAME)) {
            sensorName = localParams.get(NAME);
        }
        if (sensorName == null) {
            return new Response(Status.BAD_REQUEST, MIME_PLAINTEXT, "Failed to find the Name parameter");
        }
        phSensor = LaunchControl.getInstance().findPhSensor(sensorName);

        if (phSensor == null) {
            phSensor = new PhSensor();
            phSensor.setName(sensorName);
            LaunchControl.getInstance().addPhSensor(phSensor);
        }

        // Update the pH Sensor
        String temp = localParams.get(DS_ADDRESS);
        if (temp != null) {
            phSensor.setDsAddress(temp);
        }
        temp = localParams.get(DS_OFFSET);
        if (temp != null) {
            phSensor.setDsOffset(temp);
        }
        temp = localParams.get(ADC_PIN);
        if (temp != null) {
            int newPin = -1;
            if (!isNullOrEmpty(temp)) {
                try {
                    newPin = Integer.parseInt(temp);
                } catch (NumberFormatException nfe) {
                    LaunchControl.setMessage("Couldn't parse analog pin " + temp + " as an integer.");
                }
            }
            phSensor.setAinPin(newPin);
        }
        LaunchControl lc = LaunchControl.getInstance();
        String i2cModel = localParams.get(I_2_C_MODEL);
        if (i2cModel != null && i2cModel.length() > 0) {
            String devNumber = localParams.get(I_2_C_DEVICE);
            String devAddress = localParams.get(I_2_C_ADDRESS);
            String devChannel = localParams.get(I_2_C_CHANNEL);

            phSensor.i2cDevice = lc.getI2CDevice(devNumber, devAddress, i2cModel);
            phSensor.i2cChannel = Integer.parseInt(devChannel);
        }

        temp = localParams.get(PH_MODEL);
        if (temp != null) {
            phSensor.setModel(temp);
        }

        // Check for a calibration
        temp = localParams.get(CALIBRATION);
        if (!isNullOrEmpty(temp)) {
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

    @UrlEndpoint(url = "/delphsensor", help = "Remove a pH Sensor",
            parameters = {
                    @Parameter(name = NAME, value = "The name of the pH Sensor to delete")
            })
    public Response delPhSensor() {
        String sensorName = this.parameters.get(NAME);
        if (sensorName != null
                && LaunchControl.getInstance().deletePhSensor(sensorName)) {
            LaunchControl.setMessage("pH Sensor '" + sensorName + "' deleted.");
        } else {
            LaunchControl.setMessage("Could not find pH Sensor with name: " + sensorName);
        }

        return new Response(Status.OK, MIME_HTML, "");
    }

    /**
     * Update the specified pH Sensor reading.
     *
     * @return a Response with the pH Sensor value.
     */
    @UrlEndpoint(url = "/readphsensor", help = "Get the current value of a pH Sensor",
            parameters = {@Parameter(name = NAME, value = "The name of the pH Sensor to get the value for")})
    public final Response readPhSensor() {
        PhSensor phSensor = LaunchControl.getInstance().findPhSensor(
                this.parameters.get(NAME).replace(" ", "_"));
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
     *
     * @return The HTML Representing the trigger form.
     */
    @UrlEndpoint(url = "/getTriggerForm", help = "Get the trigger edit form for the specified trigger type at the position",
            parameters = {
                    @Parameter(name = POSITION, value = "The integer position of the trigger to edit/add"),
                    @Parameter(name = TYPE, value = "The trigger type to get the edit/add form for.")
            })
    public final Response getTriggerForm() {
        int position = Integer.parseInt(this.parameters.get(POSITION));
        String type = this.parameters.get(TYPE);
        HtmlCanvas result = TriggerControl.getNewTriggerForm(position, type);
        if (result == null) {
            return null;
        }
        return new Response(Status.OK, MIME_HTML, result.toHtml());
    }

    /**
     * Get the form representing the new triggers.
     *
     * @return The Response with the HTML.
     */
    @UrlEndpoint(url = "/getNewTriggers", help = "Get a form representing the new trigger types available for the temperature probe",
            parameters = {@Parameter(name = TEMP, value = "The name of the temperature probe/device to get the new trigger form for")})
    public final Response getNewTriggersForm() {
        Status status = Status.OK;
        HtmlCanvas htmlCanvas;
        String probe = this.parameters.get(TEMP);
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
     *
     * @return The Edit form.
     */
    @UrlEndpoint(url = "/gettriggeredit", help = "Edit the trigger at the specified position for the temp probe",
            parameters = {
                    @Parameter(name = POSITION, value = "The position in the trigger control to edit"),
                    @Parameter(name = TEMPPROBE, value = "The temperature probe to edit.")
            })
    public final Response getTriggerEditForm() {
        Status status = Status.OK;
        int position = Integer.parseInt(this.parameters.get(POSITION));
        String tempProbeName = this.parameters.get(TEMPPROBE);
        Temp tempProbe = LaunchControl.getInstance().findTemp(tempProbeName);
        if (tempProbe == null) {
            return null;
        }
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        HtmlCanvas canvas = triggerControl.getEditTriggerForm(
                position, new JSONObject(this.parameters));
        if (canvas != null) {
            return new Response(status, MIME_HTML, canvas.toHtml());
        }
        return new Response(Status.BAD_REQUEST, MIME_HTML, "BAD");
    }

    /**
     * Add a new Trigger to the incoming tempProbe object.
     *
     * @return A response for the user.
     */
    @UrlEndpoint(url = "/addtriggertotemp", help = "Add a trigger to the temperature probe trigger control",
            parameters = {
                    @Parameter(name = POSITION, value = "The position to add the trigger in"),
                    @Parameter(name = TYPE, value = "The type of trigger to add"),
                    @Parameter(name = TEMPPROBE, value = "The name of the temperature probe to add the trigger control to."),
                    @Parameter(name = "<Other Values>", value = "The values specified by the trigger edit form.")
            })
    public final Response addNewTrigger() {
        Status status = Status.OK;
        int position = Integer.parseInt(this.parameters.get(POSITION));
        String type = this.parameters.get(TYPE);
        String tempProbeName = this.parameters.get(TEMPPROBE);
        Temp tempProbe = LaunchControl.getInstance().findTemp(tempProbeName);
        if (tempProbe == null) {
            return null;
        }
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        if (triggerControl == null) {
            return null;
        }
        triggerControl.addTrigger(position, type, new JSONObject(this.parameters));
        return new Response(status, MIME_HTML, "OK");
    }

    /**
     * Update the trigger.
     *
     * @return The Edit form.
     */
    @UrlEndpoint(url = "/updatetrigger", help = "Update the trigger at the specified position",
            parameters = {
                    @Parameter(name = POSITION, value = "The position to edit the trigger at."),
                    @Parameter(name = TEMPPROBE, value = "The temperature probe to edit the trigger on."),
                    @Parameter(name = "<Other Values>", value = "Other values requested by the trigger type.")
            })
    public final Response updateTrigger() {
        Status status = Status.OK;
        int position = Integer.parseInt(this.parameters.get(POSITION));
        String tempProbeName = this.parameters.get(TEMPPROBE);
        Temp tempProbe = LaunchControl.getInstance().findTemp(tempProbeName);
        if (tempProbe == null) {
            return null;
        }
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        if (triggerControl == null) {
            return null;
        }
        if (!triggerControl.updateTrigger(position, new JSONObject(this.parameters))) {
            return null;
        }

        return new Response(status, MIME_HTML, "OK");
    }

    /**
     * Reorder the devices in the UI.
     *
     * @return A response object.
     */
    @UrlEndpoint(url = "/reorderprobes", help = "Re-order the temperature probes",
            parameters = {@Parameter(name = "<name of the device>", value = "<New integer position of the device>")})
    public final Response reorderProbes() {
        Map<String, String> params = this.parameters;
        JSONObject usage = new JSONObject();
        usage.put("Usage", "Set the order for the probes.");
        usage.put(":name=:position", "The name and the new position");

        // Should be good to go, iterate and update!
        for (Map.Entry<String, String> mEntry : params.entrySet()) {
            if (mEntry.getKey().equals("NanoHttpd.QUERY_STRING")) {
                continue;
            }
            try {
                String tName = mEntry.getKey();
                int newPos = Integer.parseInt(mEntry.getValue());
                Temp temp = LaunchControl.getInstance().findTemp(tName);
                if (temp != null) {
                    temp.setPosition(newPos);
                }
            } catch (NumberFormatException nfe) {
                LaunchControl.setMessage(
                        "Failed to parse device reorder value, things may get weird: " + mEntry.getKey() + ": " + mEntry.getValue());
            }
        }
        LaunchControl.getInstance().sortDevices();
        return new Response(Status.OK, MIME_TYPES.get("json"),
                usage.toJSONString());
    }

    @SuppressWarnings("unchecked")
    @UrlEndpoint(url = "/uploadbeerxml", help = "Upload beerXML for parsing",
            parameters = {@Parameter(name = "<files>", value = "Files to upload")})
    public Response uploadBeerXML() {

        final Map<String, String> files = this.files;
        Status status = Status.ACCEPTED;
        if (files.size() == 1) {
            for (Map.Entry<String, String> entry : files.entrySet()) {

                try {
                    File uploadedFile = new File(entry.getValue());
                    String fileType = new Tika().detect(
                            uploadedFile);

                    if (fileType.endsWith("/xml")) {
                        BeerXMLReader.getInstance().readFile(uploadedFile);
                        ArrayList<String> recipeList = BeerXMLReader.getInstance().getListOfRecipes();
                        setRecipeList(recipeList);
                        if (recipeList == null) {
                            LaunchControl.setMessage("Couldn't read recipe file.");
                        } else if (recipeList.size() == 1) {
                            setCurrentRecipe(BeerXMLReader.getInstance().readRecipe(recipeList.get(0)));
                            LaunchControl.setMessage("A single recipe has been read in and set: " + getCurrentRecipe().getName());
                        } else {
                            LaunchControl.setMessage(recipeList.size() + " recipes read in.");
                        }
                    }
                } catch (IOException e) {
                    return null;
                } catch (XPathException e) {
                    e.printStackTrace();
                }
            }
        }
        return new Response(status, MIME_TYPES.get("json"), "");
    }

    @UrlEndpoint(url = "/getrecipelist", help = "Get the list of recipes that Elsinore has read in",
            parameters = {})
    public Response getRecipeList() {
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        try {
            new RecipeListForm().renderOn(html);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Response(Status.OK, MIME_HTML, html.toHtml());
    }

    @UrlEndpoint(url = "/showrecipe", help = "Show the recipe HTML for a specified recipe",
            parameters = {@Parameter(name = RECIPE_NAME, value = "The name of the recipe to show")})
    public Response showRecipe() {
        String recipeName = this.parameters.get(RECIPE_NAME);
        String renderSection = this.parameters.get(SUBSET);
        if (recipeName == null && getCurrentRecipe() == null) {
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
            recipe = getCurrentRecipe();
        }

        if (recipe == null) {
            return new Response(Status.BAD_REQUEST, MIME_HTML, "Could not find recipe: " + recipeName);
        }
        HtmlCanvas html = new HtmlCanvas(new PrettyWriter());
        try {
            RecipeViewForm recipeForm = new RecipeViewForm(recipe);
            if (renderSection != null) {
                html.macros().stylesheet("/bootstrap-v4/css/bootstrap.min.css");
                html.div(id("recipeView").class_("text-center"));
                switch (renderSection) {
                    case MASH:
                        recipeForm.renderMash(html);
                        break;
                    case HOPS:
                        recipeForm.renderHops(html);
                        break;
                    case FERMENTATION:
                        recipeForm.renderFermentation(html);
                        break;
                    case DRY:
                        recipeForm.renderDryHops(html);
                        break;
                }
                html._div();
            } else {
                recipeForm.renderOn(html);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Response(Status.OK, MIME_HTML, html.toHtml());
    }

    @UrlEndpoint(url = "/setprofile", help = "Set the profile for a PID from the recipe, make sure the current recipe is set first.",
            parameters = {
                    @Parameter(name = PROFILE, value = "The name of the profile to set from the recipe."),
                    @Parameter(name = TEMPPROBE, value = "The name of the temperature probe to set the profile for.")
            }
    )
    public Response setProfile() {
        String profile = this.parameters.get(PROFILE);
        if (isNullOrEmpty(profile)) {
            LaunchControl.setMessage("No profile provided");
            return null;
        }

        String tempProbe = this.parameters.get(TEMPPROBE);
        if (isNullOrEmpty(tempProbe)) {
            LaunchControl.setMessage("No temperature probe provided");
            return null;
        }

        Temp temp = LaunchControl.getInstance().findTemp(tempProbe);
        if (temp == null) {
            LaunchControl.setMessage("Couldn't find temp probe: " + tempProbe);
            return null;
        }

        if (getCurrentRecipe() == null) {
            LaunchControl.setMessage("No recipe selected");
            return null;
        }

        if (profile.equalsIgnoreCase(MASH)) {
            getCurrentRecipe().setMashProfile(temp);
        }

        if (profile.equalsIgnoreCase(BOIL)) {
            getCurrentRecipe().setBoilHops(temp);
        }

        if (profile.equalsIgnoreCase(FERM)) {
            getCurrentRecipe().setFermProfile(temp);
        }

        if (profile.equalsIgnoreCase(DRY)) {
            getCurrentRecipe().setDryHops(temp);
        }
        LaunchControl.setMessage("Set " + profile + " profile for " + tempProbe);
        return new Response(Status.OK, MIME_HTML, "Set " + profile + " profile for " + tempProbe);
    }

    /**
     * Clear the notification.
     *
     * @return A response object indicating whether the notification is cleared OK.
     */
    @UrlEndpoint(url = "/clearnotification", help = "Clear the current notification",
            parameters = {@Parameter(name = NOTIFICATION, value = "The notification to clear.")})
    public Response clearNotification() {
        String rInt = this.parameters.get(NOTIFICATION);
        if (isNullOrEmpty(rInt)) {
            LOG.warning("No notification position supplied to clear");
            return null;
        }

        try {
            int position = Integer.parseInt(rInt);
            if (!Notifications.getInstance().clearNotification(position)) {
                LOG.warning("Failed to clear notification: " + position);
                return null;
            }
        } catch (NumberFormatException ne) {
            LOG.warning("No notification position supplied to clear: " + rInt);
            return null;
        }

        return new Response(Status.OK, MIME_HTML, "Cleared notification: " + rInt);
    }

    @UrlEndpoint(url = "/resetrecorder", help = "Reset the recorder data, store the old data, and create a new start point.",
            parameters = {})
    public Response resetRecorder() {
        LaunchControl.getInstance().disableRecorder();
        LaunchControl.getInstance().enableRecorder();
        return new Response(Status.OK, MIME_HTML, "Reset recorder.");
    }

    @UrlEndpoint(url = "/deletegraphdata", help = "Delete the current recorder data.",
            parameters = {})
    public Response deleteGraphData() {
        LaunchControl lc = LaunchControl.getInstance();
        if (lc.recorderEnabled) {
            StatusRecorder temp = lc.disableRecorder();
            if (temp == null) {
                return null;
            }
            Response response = temp.deleteAllData();
            lc.enableRecorder();
            return response;
        }
        return new Response("Recorder not enabled");
    }

    @UrlEndpoint(url = "/deleteprobe", help = "Delete the PID/Temp probe specified",
            parameters = {@Parameter(name = PROBE, value = "The name of the device to delete")})
    public Response deleteTempProbe() {
        LaunchControl lc = LaunchControl.getInstance();
        String probeName = this.parameters.get(PROBE);
        Status status = Status.OK;
        if (isNullOrEmpty(probeName)) {
            return null;
        } else {
            Temp tempProbe = lc.findTemp(probeName);
            if (tempProbe == null) {
                return null;
            } else {
                PID pid = lc.findPID(probeName);
                if (pid != null) {
                    lc.deleteTemp(pid);
                }
                lc.deleteTemp(tempProbe);
            }
        }
        Response response = new Response("");
        response.setStatus(status);
        return response;
    }

    @UrlEndpoint(url = "/shutdownSystem", help = "Shutdown Elsinore or turn off the system",
            parameters = {@Parameter(name = TURNOFF, value = "true: turn off the entire system, false: shutdown Elsinore only")})
    public Response shutdownSystem() {
        try {
            LaunchControl.getInstance().saveEverything();
            boolean shutdownEverything = Boolean.parseBoolean(this.parameters.get(TURNOFF));
            if (shutdownEverything) {
                // Shutdown the system using shutdown now
                Runtime runtime = Runtime.getRuntime();
                runtime.exec("shutdown -h now");
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.warning("Failed to shutdown. " + e.getMessage());
        }

        return new Response("Shutdown called");
    }

    @UrlEndpoint(url = "/toggletimer", help = "Start, stop or reset a timer",
            parameters = {
                    @Parameter(name = RESET, value = "name of the timer to reset"),
                    @Parameter(name = TOGGLE, value = "name of the timer to toggle on/off")})
    public Response toggleTimer() {
        LaunchControl lc = LaunchControl.getInstance();
        Status status = Status.BAD_REQUEST;
        String message = "Bad request";
        if (this.parameters.containsKey(TOGGLE)) {
            String name = this.parameters.get(TOGGLE);
            Timer timer = lc.findTimer(name);
            if (timer != null) {
                timer.startTimer();
                status = Status.OK;
                message = "Timer started/paused";
            } else {
                status = Status.BAD_REQUEST;
                message = "Couldn't find the timer " + name;
            }

        }
        if (this.parameters.containsKey(RESET)) {
            String name = this.parameters.get(RESET);
            Timer timer = lc.findTimer(name);
            if (timer != null) {
                timer.resetTimer();
                status = Status.OK;
                message = "Timer reset";
            } else {
                status = Status.BAD_REQUEST;
                message = "Couldn't find the timer " + name;
            }
        }
        return new Response(status, MIME_TYPES.get("txt"), message);
    }

    @UrlEndpoint(url = "/getTimerSettings", help = "Get the settings for the current timer.",
            parameters = {@Parameter(name = TIMER, value = "The name of the timer to get the settings for")})
    public Response getTimerSettings() {
        String timerName = this.parameters.get(TIMER);
        if (isNullOrEmpty(timerName)) {
            return new Response(Status.BAD_REQUEST, MIME_TYPES.get("txt"), "No timer name provided.");
        }

        Timer timer = LaunchControl.getInstance().findTimer(timerName);

        JSONObject timerSettings = new JSONObject();
        if (timer != null) {
            timerSettings.put(NAME, timer.getName());
            timerSettings.put(DURATION, timer.getTarget());
            timerSettings.put(INVERTED, timer.getInverted());
        }
        return new Response(Status.OK, MIME_TYPES.get("json"), timerSettings.toJSONString());
    }

    @UrlEndpoint(url = "/clearbeerxml", help = "Clear the currently loaded BeerXML", parameters = {})
    public Response clearBeerXML() {
        BrewServer.getRecipeList().clear();
        setCurrentRecipe(null);
        return new Response(Status.OK, MIME_TYPES.get("txt"), "Cleared BeerXML");
    }
}
