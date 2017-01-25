package com.sb.elsinore;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.persistence.*;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * This is the base class for devices.
 * Created by Douglas on 2016-10-17.
 */
@Entity
@Inheritance
@DiscriminatorColumn(name = "DEVICE_TYPE")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Expose
    @SerializedName("type")
    private String type = null;

    public String getType() {
        return this.type;
    }

    public Device() {

    }

    public Device(String type) {
        if (isNullOrEmpty(type)) {
            throw new IllegalArgumentException("Type cannot be null/empty");
        }
        this.type = type;
    }
}
