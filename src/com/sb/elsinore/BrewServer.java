package com.sb.elsinore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
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
        }
    };


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
    private boolean editVessel(final Map<String, String> params) {
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

        if (parms.containsKey("new_gpio")) {
            gpio = parms.get("new_gpio");
        }

        if (inputUnit.equals("")) {
            BrewServer.LOG.warning("No Valid input unit");
            return false;
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
                LaunchControl.addPID(tPID);
                BrewServer.LOG.warning("Create PID");
            }
        }

        return true;
    }

    /**
     * Read the incoming parameters and update the PID as appropriate.
     * @param params The parameters from the client
     * @return True if success, false if failure
     */
    @SuppressWarnings("unchecked")
    private boolean updatePID(final Map<String, String> params) {
        String temp, mode = "off", inputUnit = null;
        BigDecimal dTemp = new BigDecimal(0),
            duty = new BigDecimal(0),
            cycle = new BigDecimal(0),
            setpoint = new BigDecimal(0),
            p = new BigDecimal(0),
            i = new BigDecimal(0),
            d = new BigDecimal(0);
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

        if (parms.containsKey("mode")) {
            mode = parms.get("mode");
            BrewServer.LOG.info("Mode: " + mode);
        }

        BrewServer.LOG.info("Form: " + inputUnit);

        PID tPID = LaunchControl.findPID(inputUnit);
        if (tPID != null) {
            BrewServer.LOG.info(mode + ":" + duty + ":" + cycle + ":" + setpoint
                + ":" + p + ":" + i + ":" + d);
            tPID.updateValues(mode, duty, cycle, setpoint, p, i, d);
        } else {
            BrewServer.LOG.warning("Attempted to update a non existent PID, "
                    + inputUnit + ". Please check your client");
            return false;
        }

        return true;
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
        if (method == Method.POST) {
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
                if (editVessel(parms)) {
                    return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                        "Editted Vessel");
                } else {
                    return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                            "Failed to Edit Vessel. Please check logs");
                }
            }
            if (uri.toLowerCase().equals("/updatepid")) {
                // parse the values if possible
                // TODO: Break this out into a function
                if (updatePID(parms)) {
                    return new NanoHTTPD.Response(Status.OK, MIME_HTML,
                        "Updated PID");
                } else {
                    return new NanoHTTPD.Response(Status.BAD_REQUEST, MIME_HTML,
                            "Failed to update PID. Please check logs");
                }
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
                    return new NanoHTTPD.Response( Status.OK, MIME_HTML,
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
                        LOG.warning("Invalid pump: " + pumpname + " provided.");
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
                    }
                }
            }
        }

        if (uri.toLowerCase().equals("/getstatus")) {
            return new NanoHTTPD.Response(Status.OK, MIME_HTML,
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

        if (new File(rootDir, uri).exists()) {
            return serveFile(uri, header, rootDir);
        }

        BrewServer.LOG.info("Invalid URI: " + uri);
        return new NanoHTTPD.Response(Status.OK, MIME_HTML, "Unrecognized URL");
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

}
