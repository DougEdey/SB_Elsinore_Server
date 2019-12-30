package com.sb.elsinore.models;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class TemperatureTest {

    private TemperatureModel temperature = null;

    @Before
    public void before() {
        this.temperature = new TemperatureModel();
    }

    @Test
    public void canReadAndWriteName() {
        this.temperature.setName("Test");

        assertEquals("Test", this.temperature.getName());
    }

    @Test
    public void canReadAndWriteDevice() {
        this.temperature.setDevice("device");

        assertEquals("device", this.temperature.getDevice());
    }

    @Test
    public void canReadAndWriteScale() {
        this.temperature.setScale("F");
        assertEquals("F", this.temperature.getScale());
    }

    @Test
    public void canReadAndWriteVolumeUnit() {
        this.temperature.setVolumeUnit("l");
        assertEquals("l", this.temperature.getVolumeUnit());
    }

    @Test
    public void canReadAndWriteVolumeAIN() {
        this.temperature.setVolumeAIN(4);
        assertEquals("4", this.temperature.getVolumeAIN());
    }

    @Test
    public void canReadAndWriteVolumeAINSetToNull() {
        this.temperature.setVolumeAIN(-1);
        assertEquals("", this.temperature.getVolumeAIN());
    }


    @Test
    public void canReadAndWriteVolumeAddress() {
        this.temperature.setVolumeAddress("Address");
        assertEquals("Address", this.temperature.getVolumeAddress());
    }


    @Test
    public void canReadAndWriteVolumeOffset() {
        this.temperature.setVolumeOffset("A");
        assertEquals("A", this.temperature.getVolumeOffset());
    }


    @Test
    public void canReadAndWriteCalibration() {
        this.temperature.setCalibration(new BigDecimal(5.4));
        assertEquals(new BigDecimal(5.4), this.temperature.getCalibration());
    }


    @Test
    public void canShow() {
        this.temperature.show();
        assertFalse(this.temperature.isHidden());
    }

    @Test
    public void canHide() {
        this.temperature.hide();
        assertTrue(this.temperature.isHidden());
    }

    @Test
    public void canReadAndWritePosition() {
        this.temperature.setPosition(23);
        assertEquals((Integer) 23, this.temperature.getPosition());
    }

    @Test
    public void canReadAndWriteI2CNumber() {
        this.temperature.setI2cNumber("1");
        assertEquals("1", this.temperature.getI2cNumber());
    }

    @Test
    public void canReadAndWriteI2CAddress() {
        this.temperature.setI2cAddress("I2C_Address");
        assertEquals("I2C_Address", this.temperature.getI2cAddress());
    }

    @Test
    public void canReadAndWriteI2CChannelToNull() {
        this.temperature.setI2cChannel("Channel");
        assertEquals("", this.temperature.getI2cChannel());
    }

    @Test
    public void canReadAndWriteI2CChannelToInteger() {
        this.temperature.setI2cChannel("12");
        assertEquals("12", this.temperature.getI2cChannel());
    }

    @Test
    public void canReadAndWriteI2CDevType() {
        this.temperature.setI2cType("Dev_Type");
        assertEquals("Dev_Type", this.temperature.getI2cType());
    }

    @Test
    public void canReadAndWriteCutoffTemp() {
        this.temperature.setCutoffTemp(new BigDecimal(103));
        assertEquals(new BigDecimal(103), this.temperature.getCutoffTemp());
    }

    @Test
    public void canEnableVolumeMeasurementEnabled() {
        this.temperature.setVolumeMeasurementEnabled(true);
        assertTrue(this.temperature.getVolumeMeasurementEnabled());
    }

    @Test
    public void canDisableVolumeMeasurementEnabled() {
        this.temperature.setVolumeMeasurementEnabled(false);
        assertFalse(this.temperature.getVolumeMeasurementEnabled());
    }
}
