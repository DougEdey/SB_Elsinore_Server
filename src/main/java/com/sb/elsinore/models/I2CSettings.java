package com.sb.elsinore.models;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"address", "deviceNumber"}))
public class I2CSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Min(0)
    private Integer address = -1;
    @NotNull
    @Min(0)
    private Integer deviceNumber = -1;

    @NotNull
    @Size(min = 1)
    private String deviceType = "";

    public Long getId() {
        return this.id;
    }

    public int getAddress() {
        return this.address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getDeviceNumber() {
        return this.deviceNumber;
    }

    public void setDeviceNumber(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public String getDeviceType() {
        return this.deviceType;
    }

    public void setDeviceType(@NotNull String deviceType) {
        this.deviceType = deviceType;
    }
}
