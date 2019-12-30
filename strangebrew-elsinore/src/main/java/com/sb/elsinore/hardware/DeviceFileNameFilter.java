package com.sb.elsinore.hardware;

import java.io.File;
import java.io.FilenameFilter;

public class DeviceFileNameFilter implements FilenameFilter {

    private final DeviceType deviceType;

    public DeviceFileNameFilter(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.startsWith(this.deviceType.getPrefix());
    }
}
