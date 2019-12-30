package com.sb.elsinore;

import com.google.gson.annotations.Expose;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.HashMap;


/**
 *
 */
public class StatusRecorder implements Runnable {

    static String defaultDirectory = "graph-data/";
    static String DIRECTORY_PROPERTY = "recorder_directory";
    static String RECORDER_ENABLED = "recorder_enabled";
    @Expose
    double diff = .15d;
    @Expose
    long sleep = 1000 * 5; // 5 seconds - is this too fast?
    @Expose
    String recorderDirectory = StatusRecorder.defaultDirectory;
    @Autowired
    private LaunchControl launchControl;
    private Logger logger = LoggerFactory.getLogger(StatusRecorder.class);
    private JSONObject lastStatus = null;
    private String logFile = null;
    private Thread thread;
    private HashMap<String, Status> temperatureMap;
    private HashMap<String, Status> dutyMap;
    private String currentDirectory = null;

    StatusRecorder(String recorderDirectory) {
        this.recorderDirectory = recorderDirectory;
    }

    @Autowired
    public void setLaunchControl(LaunchControl launchControl) {
        this.launchControl = launchControl;
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
            long startTime = System.currentTimeMillis();

            this.currentDirectory = this.recorderDirectory + "/" + startTime + "/";
            File directoryFile = new File(this.currentDirectory);
            if (!directoryFile.mkdirs()) {
                this.logger.warn("Could not create directory: " + this.currentDirectory);
                return;
            }
            this.launchControl.setFileOwner(directoryFile.getParentFile());
            this.launchControl.setFileOwner(directoryFile);

            //Generate a new log file under the current directory
            this.logFile = this.currentDirectory + "raw.log";

            File file = new File(this.logFile);
            this.launchControl.setFileOwner(file);

            boolean continueRunning = true;
            while (continueRunning) {
                //Just going to record when something changes
                Thread.sleep(this.sleep);
                // TODO: Flesh this out
            }
        } catch (InterruptedException ex) {
            this.logger.warn("Status Recorder shutting down");
        }

    }

    public double getDiff() {
        return this.diff;
    }

    public void setDiff(double threshold) {
        this.diff = threshold;
    }

    public double getTime() {
        return this.sleep;
    }

    public void setTime(long time) {
        this.sleep = time;
    }

    private String getCurrentDir() {
        return this.currentDirectory;
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

    private class Status {

        public String value;
        long timestamp;

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

}
