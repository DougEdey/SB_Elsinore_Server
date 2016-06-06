package com.sb.elsinore;

import com.google.gson.annotations.Expose;

/**
 * Holds the system configuration
 * Created by Douglas on 2016-05-15.
 */
public class SystemSettings {
    @Expose
    public StatusRecorder recorder;
    @Expose
    boolean restoreState;
    @Expose
    public String scale = "C";
    @Expose boolean useOWFS = false;
    @Expose String owfsServer = "localhost";
    @Expose int owfsPort = 4304;
    @Expose String breweryName = "Elsinore";
    @Expose int server_port = LaunchControl.DEFAULT_PORT;
}
