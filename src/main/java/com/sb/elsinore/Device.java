package com.sb.elsinore;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by Douglas on 2016-10-17.
 */
public class Device {
    @Expose
    @SerializedName("type")
    public String type = null;

    public String getType() {
        return this.type;
    }

    public Device(String type) {
        if (isNullOrEmpty(type)) {
            throw new IllegalArgumentException("Type cannot be null/empty");
        }
        this.type = type;
    }
}
