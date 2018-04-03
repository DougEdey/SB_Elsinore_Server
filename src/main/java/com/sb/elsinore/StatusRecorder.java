package com.sb.elsinore;

import com.google.gson.annotations.Expose;
import com.sb.common.SBStringUtils;
import com.sb.elsinore.wrappers.TempRunner;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 *
 *
 */
public class StatusRecorder implements Runnable {

    @Expose
    double diff = .15d;
    @Expose
    long sleep = 1000 * 5; // 5 seconds - is this too fast?

    private JSONObject lastStatus = null;
    private String logFile = null;
    private Thread thread;
    @Expose
    String recorderDirectory = StatusRecorder.defaultDirectory;
    private HashMap<String, Status> temperatureMap;
    private HashMap<String, Status> dutyMap;
    static String defaultDirectory = "graph-data/";
    static String DIRECTORY_PROPERTY = "recorder_directory";
    static String RECORDER_ENABLED = "recorder_enabled";
    private String currentDirectory = null;

    StatusRecorder(String recorderDirectory) {
        this.recorderDirectory = recorderDirectory;
    }

    /**
     * Start the thread.
     */
    public final void start() {
        if (this.thread == null || !this.thread.isAlive()) {
            this.temperatureMap = new HashMap<>();
            this.dutyMap = new HashMap<>();
            this.thread = new Thread(this);
            this.thread.setName("Status recorder");
            this.thread.start();
        }
    }

    /**
     * Stop the thread.
     */
    public final void stop() {
        if (this.thread != null) {
            this.thread.interrupt();
            while (this.thread.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.thread = null;
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

            this.currentDirectory = this.recorderDirectory + "/" + startTime + "/";
            File directoryFile = new File(this.currentDirectory);
            if (!directoryFile.mkdirs()) {
                BrewServer.LOG.warning("Could not create directory: " + this.currentDirectory);
                return;
            }
            LaunchControl.getInstance().setFileOwner(directoryFile.getParentFile());
            LaunchControl.getInstance().setFileOwner(directoryFile);

            //Generate a new log file under the current directory
            this.logFile = this.currentDirectory + "raw.log";

            File file = new File(this.logFile);
            LaunchControl.getInstance().setFileOwner(file);

            boolean continueRunning = true;
            while (continueRunning) {
                //Just going to record when something changes
                Thread.sleep(this.sleep);
            }
        } catch (InterruptedException ex) {
            BrewServer.LOG.warning("Status Recorder shutting down");
        }

    }

    private boolean checkInitialized() {
        return LaunchControl.getInstance().isInitialized();
    }

    private class Status {

        long timestamp;
        public String value;

        Status(String value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        boolean isDifferentEnough(String newValue) {
            boolean retVal = false;

            try {
                double oldVal = Double.valueOf(this.value);
                double newVal = Double.valueOf(newValue);
                retVal = Math.abs(oldVal - newVal) > StatusRecorder.this.diff;
            } catch (Throwable ignored) {
            }

            return retVal;

        }
    }

    public double getDiff() {
        return this.diff;
    }

    public double getTime() {
        return this.sleep;
    }

    public void setDiff(double threshold) {
        this.diff = threshold;
    }

    public void setTime(long time) {
        this.sleep = time;
    }

    private String getCurrentDir() {
        return this.currentDirectory;
    }

    @SuppressWarnings("unchecked")
    NanoHTTPD.Response getData(Map<String, String> params) {
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
        TempRunner tempProbe = LaunchControl.getInstance().findTemp(vessel);
        if (tempProbe != null) {
            vesselName = tempProbe.getTempProbe().getProbe();
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
                String localName;

                // Strip off .csv
                name = name.substring(0, name.length() - 4);
                localName = name.substring(0, name.lastIndexOf("-"));
                String type = name.substring(name.lastIndexOf("-") + 1);
                TempRunner localTempProbe = LaunchControl.getInstance().findTemp(localName);
                if (localTempProbe != null) {
                    localName = localTempProbe.getName();
                } else {
                    localName = name;
                }
                localName = org.apache.commons.lang3.StringEscapeUtils.unescapeJson(localName);
                if (params.containsKey("bindto")
                        && (params.get("bindto"))
                        .endsWith("-graph_body")) {
                    localName = type;
                }

                // Work out the real axis name and reuse it.
                String axisName = type.toUpperCase();
                if (!localName.equals(type)) {
                    axisName = localName + " " + type;
                }

                xsData.put(axisName, "x" + axisName);
                if (type.equalsIgnoreCase("duty")) {

                    axes.put(axisName, "y");
                    dutyVisible = true;
                } else {
                    axes.put(axisName, "y2");
                }

                JSONArray xArray = new JSONArray();
                JSONArray dataArray = new JSONArray();

                xArray.add("x" + axisName);
                dataArray.add(axisName);

                ReversedLinesFileReader reader = null;
                try {
                    reader = new ReversedLinesFileReader(content);
                    String line;
                    String[] lArray;

                    int count = 0;
                    try {
                        while ((line = reader.readLine()) != null && count < size) {
                            // Each line contains the timestamp and the value
                            lArray = line.split(",");
                            BrewServer.LOG.info("Line: " + line + ". Split: " + lArray.length);
                            if (lArray.length != 2) {
                                continue;
                            }
                            long timestamp = Long.parseLong(lArray[0]);
                            // If this is the first element, add an extra one on.
                            if (count == 0 && timestamp != currentTime) {
                                xArray.add(BrewDay.mFormat
                                        .format(new Date(currentTime)));
                                dataArray.add(lArray[1].trim());
                            }
                            xArray.add(BrewDay.mFormat
                                    .format(new Date(timestamp))
                            );
                            dataArray.add(lArray[1].trim());
                            count++;
                        }
                    } catch (Exception e) {
                        // Do nothing. File doesn't have any data.
                        BrewServer.LOG.info("Error when reading for temperature: " + content.getAbsolutePath());
                    }


                    dataBuffer.add(xArray);
                    dataBuffer.add(dataArray);
                    BrewServer.LOG.info("Read: " + count);
                } catch (Exception e) {
                    e.printStackTrace();
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
                if (series.equals("tempProbe")) {
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
        y1.put("show", "true");
        y1.put("label", y1Label);

        JSONObject padding = new JSONObject();
        padding.put("top", 0);
        padding.put("bottom", 0);
        y1.put("padding", padding);

        JSONObject formatJSON = new JSONObject();
        formatJSON.put("format", "%H:%M:%S");
        formatJSON.put("culling", "{max: 4}");
        formatJSON.put("rotate", 90);
        JSONObject xContent = new JSONObject();
        xContent.put("type", "timeseries");
        xContent.put("tick", formatJSON);
        axisContent.put("x", xContent);
        axisContent.put("y2", y1);
        if (dutyVisible) {
            JSONObject y2Label = new JSONObject();
            y2Label.put("text", "Duty Cycle %");
            y2Label.put("position", "outer-middle");
            JSONObject y2 = new JSONObject();
            y2.put("show", "true");
            y2.put("label", y2Label);
            axisContent.put("y", y2);
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

    NanoHTTPD.Response deleteAllData() {
        File graphDir = new File(this.recorderDirectory);
        File[] fileList = graphDir.listFiles();

        if (fileList != null) {
            for (File directory : fileList) {
                if (!deleteDir(directory)) {
                    BrewServer.LOG.warning("Failed to delete: " + directory.getAbsolutePath());
                }
            }
        }
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, BrewServer.MIME_TYPES.get("json"),
                "{Complete}");
    }

    private boolean deleteDir(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children == null) {
                return false;
            }
            for (String childDir : children) {
                boolean success = deleteDir(new File(file, childDir));
                if (!success) {
                    return false;
                }
            }
        }
        return file.delete();
    }

}
