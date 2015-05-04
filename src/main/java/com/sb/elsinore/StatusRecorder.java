package com.sb.elsinore;

import com.sb.common.SBStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 */
public class StatusRecorder implements Runnable {

    public static double THRESHOLD = .15d;
    public static long SLEEP = 1000 * 5; // 5 seconds - is this too fast?
    private JSONObject lastStatus = null;
    private String logFile = null;
    private Thread thread;
    private String recorderDirectory = StatusRecorder.defaultDirectory;
    private HashMap<String, Status> temperatureMap;
    private HashMap<String, Status> dutyMap;
    boolean writeRawLog = false;
    public static String defaultDirectory = "graph-data/";
    public static String DIRECTORY_PROPERTY = "recorder_directory";
    public static String RECORDER_ENABLED = "recorder_enabled";
    private String currentDirectory = null;

    public StatusRecorder(String recorderDirectory) {
        this.recorderDirectory = recorderDirectory;
    }

    /**
     * Start the thread.
     */
    public final void start() {
        if (thread == null || !thread.isAlive()) {
            temperatureMap = new HashMap();
            dutyMap = new HashMap();
            thread = new Thread(this);
            thread.setName("Status recorder");
            thread.start();
        }
    }

    /**
     * Stop the thread.
     */
    public final void stop() {
        if (thread != null) {
            thread.interrupt();
            while (thread.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            thread = null;
        }
    }

    /**
     * Main runnable, updates the files every five seconds.
     */
    @Override
    public final void run() {
        //This will store multiple logs - one for raw data,
        // one for each series (duty & temperature per vessel)
        // For now - we'll store Duty, temperature vs time
        //Assume new logs on each run
        
        //Keep checking the status until all the temperature sensors are initialized
        try {
            
            while (!checkInitialized()) {
                Thread.sleep(1000);
            }

            long startTime = System.currentTimeMillis();

            currentDirectory = recorderDirectory + "/" + startTime + "/";
            File directoryFile = new File(currentDirectory);
            if (!directoryFile.mkdirs()) {
                BrewServer.LOG.warning("Could not create directory: " + currentDirectory);
                return;
            }
            LaunchControl.setFileOwner(directoryFile.getParentFile());
            LaunchControl.setFileOwner(directoryFile);

            //Generate a new log file under the current directory
            logFile = currentDirectory + "raw.log";

            File file = new File(this.logFile);
            boolean fileExists = file.exists();
            LaunchControl.setFileOwner(file);

            boolean continueRunning = true;
            while (continueRunning) {
                //Just going to record when something changes
                try {
                    String status = LaunchControl.getJSONStatus();
                    JSONObject newStatus = (JSONObject) JSONValue.parse(status);
                    if (lastStatus == null || isDifferent(lastStatus, newStatus)) {
                        //For now just log the whole status
                        //Eventually we may want multiple logs, etc.
                        if (writeRawLog) {
                            writeToLog(newStatus, fileExists);
                        }

                        Date now = new Date();
                        printJsonToCsv(now, newStatus, currentDirectory);
                        lastStatus = newStatus;
                        fileExists = true;
                    }
                } catch (Exception ioe) {
                    if (ioe instanceof InterruptedException) {
                        continueRunning = false;
                    }
                    continue;
                }
                Thread.sleep(SLEEP);
            }
        } catch (InterruptedException ex) {
            BrewServer.LOG.warning("Status Recorder shutting down");
        }

    }
    
    protected boolean checkInitialized()
    {
        return LaunchControl.isInitialized();
    }

    /**
     * Save the status to the directory.
     *
     * @param nowDate The current date to save the datapoint for.
     * @param newStatus The JSON Status object to dump
     * @param directory The graph data directory.
     */
    protected final void printJsonToCsv(final Date nowDate,
            final JSONObject newStatus, final String directory) {
        //Now look for differences in the temperature and duty
        long now = nowDate.getTime();
        JSONArray vessels = (JSONArray) newStatus.get("vessels");
        for (Object vessel1 : vessels) {
            JSONObject vessel = (JSONObject) vessel1;
            if (vessel.containsKey("name")) {
                String name = vessel.get("name").toString();
                if (LaunchControl.findTemp(name) != null) {
                    name = LaunchControl.findTemp(name).getProbe();
                }
                if (vessel.containsKey("tempprobe")) {
                    String temp = ((JSONObject) vessel.get("tempprobe"))
                            .get("temp").toString();


                    Status lastStatus = temperatureMap.get(name);
                    if (lastStatus == null) {
                        lastStatus = new Status("-999", now);
                    }

                    if (lastStatus.isDifferentEnough(temp)) {
                        File tempFile = new File(directory + name + "-temp.csv");
                        if (now - lastStatus.timestamp > SLEEP * 1.5) {
                            appendToLog(tempFile, now - SLEEP + "," + lastStatus.value + "\r\n");
                        }
                        appendToLog(tempFile, now + "," + temp + "\r\n");

                        temperatureMap.put(name, new Status(temp, now));
                    }
                }

                if (vessel.containsKey("pidstatus")) {
                    JSONObject pid = (JSONObject) vessel.get("pidstatus");
                    String duty = "0";
                    if (pid.containsKey("actualduty")) {
                        duty = pid.get("actualduty").toString();
                    } else if (!pid.get("mode").equals("off")) {
                        duty = pid.get("duty").toString();
                    }

                    Status lastStatus = dutyMap.get(name);
                    if (lastStatus == null) {
                        lastStatus = new Status("-999", now);
                    }

                    if (!duty.equals(lastStatus.value)) {
                        File dutyFile = new File(directory + name + "-duty.csv");
                        if (now - lastStatus.timestamp > SLEEP * 1.5) {
                            appendToLog(dutyFile, now - SLEEP + "," + lastStatus.value + "\r\n");
                        }
                        appendToLog(dutyFile, now + "," + duty + "\r\n");
                        dutyMap.put(name, new Status(duty, now));
                    }
                }

            }
        }
    }

    /**
     * Save the string to the log file.
     *
     * @param file The file object to save to
     * @param toAppend The string to add to the file
     */
    protected final void appendToLog(final File file, final String toAppend) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file, true);
            fileWriter.write(toAppend);
        } catch (IOException ex) {
            BrewServer.LOG.warning("Could not save to file: "
                    + file.getAbsolutePath());
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException ex) {
                BrewServer.LOG.warning("Could not close filewriter: "
                        + file.getAbsolutePath());
            }
        }
    }

    /**
     * Write a JSON object to the log file.
     *
     * @param status The JSON Object to log
     * @param fileExists If the file exists, prepend a "," otherwise an open
     * brace "["
     */
    protected final void writeToLog(final JSONObject status,
            final boolean fileExists) {
        String append = fileExists ? "," : "[" + status.toJSONString();
        appendToLog(new File(this.logFile), append);
    }

    /**
     * Check to see if the objects are different.
     *
     * @param previous The first object to check.
     * @param current The second object to check
     * @return True if the objects are different
     */
    protected final boolean isDifferent(final JSONObject previous,
            final JSONObject current) {
        if (previous.size() != current.size()) {
            return true;
        }

        for (Object key : previous.keySet()) {
            if (!"elapsed".equals(key)) {
                Object previousValue = previous.get(key);
                Object currentValue = current.get(key);

                if (compare(previousValue, currentValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check to see if the JSONArrays are different.
     *
     * @param previous The first JSONArray to check
     * @param current The second JSONArray to check.
     * @return True if the JSONArrays are different
     */
    protected final boolean isDifferent(final JSONArray previous,
            final JSONArray current) {

        if (previous.size() != current.size()) {
            return true;
        }

        for (int x = 0; x < previous.size(); x++) {
            Object previousValue = previous.get(x);
            Object currentValue = current.get(x);

            if (compare(previousValue, currentValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compare two generic objects.
     *
     * @param previousValue First object to check
     * @param currentValue Second object to check
     * @return True if the objects are different, false if the same.
     */
    protected final boolean compare(final Object previousValue,
            final Object currentValue) {
        if (previousValue == null && currentValue == null) {
            return true;
        }

        if (previousValue == null || currentValue == null) {
            return false;
        }

        if (previousValue instanceof JSONObject
                && currentValue instanceof JSONObject) {
            if (isDifferent((JSONObject) previousValue,
                    (JSONObject) currentValue)) {
                return true;
            }
        } else if (previousValue instanceof JSONArray
                && currentValue instanceof JSONArray) {
            if (isDifferent((JSONArray) previousValue,
                    (JSONArray) currentValue)) {
                return true;
            }
        } else {
            if (!previousValue.equals(currentValue)) {
                return true;
            }
        }

        return false;
    }

    private class Status {

        public long timestamp;
        public String value;
        public int count = 0;

        public Status(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public boolean isDifferentEnough(String newValue) {
            boolean retVal = false;

            try {
                double oldVal = Double.valueOf(value);
                double newVal = Double.valueOf(newValue);
                retVal = Math.abs(oldVal - newVal) > THRESHOLD;
            } catch (Throwable ignored) {
            }

            return retVal;

        }
    }

    /**
     * Set the Threshold for the status recorder.
     * @param recorderDiff The threshold to use.
     */
    public void setThreshold(double recorderDiff) {
        THRESHOLD = recorderDiff;
    }
    
    public double getDiff() {
        return StatusRecorder.THRESHOLD;
    }
    
    public double getTime() {
        return StatusRecorder.SLEEP;
    }

    public void setDiff(double threshold) {
        StatusRecorder.THRESHOLD = threshold;
    }
    
    public void setTime(long time) {
        StatusRecorder.SLEEP = time;
    }

    public String getCurrentDir() {
        return this.currentDirectory;
    }

    public NanoHTTPD.Response getData(Map<String, String> params) {
        String rootPath;
        try {
            rootPath = SBStringUtils.getAppPath("");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new NanoHTTPD.Response("No app path");
        }

        int size = 1000;
        if (params.containsKey("size")) {
            try {
                size = Integer.parseInt(params.get("size"));
            } catch (NumberFormatException nfe) {
                size = 1000;
            }
        }

        String vessel = "";
        if (params.containsKey("vessel")) {
            vessel = params.get("vessel");
        }
        String vesselName = vessel;
        Temp temp = LaunchControl.findTemp(vessel);
        if (temp != null) {
            vesselName = temp.getProbe();
        }

        File[] contents = new File(getCurrentDir()).listFiles();
        JSONObject xsData = new JSONObject();
        JSONObject axes = new JSONObject();
        JSONArray dataBuffer = new JSONArray();
        long currentTime = System.currentTimeMillis();

        // Are we downloading the files?
        if (params.containsKey("download")
                && params.get("download").equalsIgnoreCase("true")) {

            String zipFileName = rootPath + "/graph-data/zipdownload-" + currentTime + ".zip";
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(zipFileName);
            } catch (FileNotFoundException ioe) {
                BrewServer.LOG.warning(
                        "Couldn't create zip file at: " + zipFileName);
                BrewServer.LOG.warning(ioe.getLocalizedMessage());
            }

            if (contents == null || zipFile == null) {
                return new NanoHTTPD.Response(NanoHTTPD.Response.Status.BAD_REQUEST, BrewServer.MIME_TYPES.get("json"),
                        "No files.");
            }

            for (File content : contents) {
                try {

                    if (content.getName().endsWith(".csv")
                            && content.getName().toLowerCase()
                            .startsWith(vesselName.toLowerCase())) {
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
                e.printStackTrace();
            }
            return BrewServer.serveFile("graph-data/zipdownload-" + currentTime + ".zip",
                    params, new File(rootPath));
        }

        if (contents == null) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.BAD_REQUEST, BrewServer.MIME_TYPES.get("json"),
                    "{Bad: Request}");
        }
        boolean dutyVisible = false;
        for (File content : contents) {
            if (content.getName().endsWith(".csv")
                    && content.getName().toLowerCase()
                    .startsWith(vesselName.toLowerCase())) {
                String name = content.getName();
                String localName = null;

                // Strip off .csv
                name = name.substring(0, name.length() - 4);
                localName = name.substring(0, name.lastIndexOf("-"));
                String type = name.substring(name.lastIndexOf("-") + 1);
                Temp localTemp = LaunchControl.findTemp(localName);
                if (localTemp != null) {
                    localName = localTemp.getName();
                } else {
                    localName = name;
                }

                if (params.containsKey("bindto")
                        && (params.get("bindto"))
                        .endsWith("-graph_body")) {
                    localName = type;
                }


                xsData.put(localName, "x" + localName + " " + type);


                if (type.equalsIgnoreCase("duty")) {
                    axes.put(localName, "y2");
                    dutyVisible = true;
                } else {
                    axes.put(localName, "y");
                }

                JSONArray xArray = new JSONArray();
                JSONArray dataArray = new JSONArray();

                xArray.add("x" + localName + " " + type);
                dataArray.add(localName);

                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(content));
                    String line;
                    String[] lArray = null;

                    int count = 0;
                    while ((line = reader.readLine()) != null && count < size) {
                        // Each line contains the timestamp and the value
                        lArray = line.split(",");
                        xArray.add(BrewDay.mFormat
                                        .format(new Date(Long.parseLong(lArray[0])))
                        );
                        dataArray.add(lArray[1].trim());
                        count++;
                    }

                    if (lArray != null && Long.parseLong(lArray[0]) != currentTime) {
                        xArray.add(BrewDay.mFormat
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
        if (params.containsKey("updates")
                && Boolean.parseBoolean(params.get("updates"))) {
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, BrewServer.MIME_TYPES.get("json"),
                    dataContent.toJSONString());
        }

        dataContent.put("xs", xsData);
        dataContent.put("axes", axes);
        dataContent.put("xFormat", "%Y/%m/%d %H:%M:%S%Z");

        // DO the colours manually
        JSONObject colorContent = new JSONObject();
        if (!vessel.equals("")) {
            JSONArray tJson;
            for (Object aDataBuffer : dataBuffer) {
                tJson = (JSONArray) aDataBuffer;
                String series = (String) tJson.get(0);
                String color = "";
                if (series.equals("temp")) {
                    color = "#00ff00";
                } else if (series.equals("duty")) {
                    color = "#0000ff";
                }
                colorContent.put(series, color);
            }
            dataContent.put("colors", colorContent);
        }

        JSONObject axisContent = new JSONObject();


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
        formatJSON.put("count", 4);
        JSONObject xContent = new JSONObject();
        xContent.put("type", "timeseries");
        xContent.put("tick", formatJSON);
        axisContent.put("x", xContent);
        axisContent.put("y", y1);
        if (dutyVisible) {
            JSONObject y2Label = new JSONObject();
            y2Label.put("text", "Duty Cycle %");
            y2Label.put("position", "outer-middle");
            JSONObject y2 = new JSONObject();
            y2.put("show", "true");
            y2.put("label", y2Label);
            axisContent.put("y2", y2);
        }

        JSONObject finalJSON = new JSONObject();
        finalJSON.put("data", dataContent);
        finalJSON.put("axis", axisContent);

        if (params.containsKey("bindto")) {
            finalJSON.put("bindto", "[id='" + params.get("bindto") + "']");
        } else {
            finalJSON.put("bindto", "#chart");
        }

        if (!((String) finalJSON.get("bindto")).endsWith("_body")) {
            JSONObject enabledJSON = new JSONObject();
            enabledJSON.put("enabled", true);
            finalJSON.put("zoom", enabledJSON);
        }

        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, BrewServer.MIME_TYPES.get("json"),
                finalJSON.toJSONString());
    }

    public NanoHTTPD.Response deleteAllData() {
        File graphDir = new File(this.recorderDirectory);
        for (File directory: graphDir.listFiles()) {
            if (!deleteDir(directory)) {
                BrewServer.LOG.warning("Failed to delete: " + directory.getAbsolutePath());
            }
        }
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, BrewServer.MIME_TYPES.get("json"),
                "{Complete}");
    }

    public boolean deleteDir(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(file, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return file.delete();
    }

}
