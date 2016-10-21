package com.sb.elsinore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.math.BigDecimal;

import static com.sb.elsinore.UrlEndpoints.*;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Created by Douglas on 2016-10-20.
 */
public class PIDRunnerTest {

    @Mock
    private PIDRunner pidRunner;
    @Mock
    private PID pid;

    @Spy
    private PIDSettings heatSettings;
    @Spy
    private PIDSettings coolSettings;


    @Before
    public void setup() throws Exception {
        this.pid = mock(PID.class);
        this.pidRunner = spy(new PIDRunner(this.pid));
        this.heatSettings = spy(new PIDSettings());
        this.coolSettings = spy(new PIDSettings());

        when(this.pidRunner.getTemp()).thenReturn(this.pid);
        when(this.pid.getSettings(HEAT)).thenReturn(this.heatSettings);
        when(this.pid.getSettings(COOL)).thenReturn(this.coolSettings);
    }

    @Test
    public void calculatePidFahrenheitReturnsZeroWithNoSettingsF() throws Exception {
        this.pid.setScale(F);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));

        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(0), result);
    }

    @Test
    public void calculatePidFahrenheitReturnsZeroWithNoSettingsC() throws Exception {
        this.pid.setScale(C);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));

        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(0), result);
    }

    @Test
    public void calculatePidFahrenheitReturnsZeroNoHeatGPIO() throws Exception {
        this.pid.setScale(C);
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.heatSettings.getProportional()).thenReturn(new BigDecimal(60));
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));

        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(0), result);
    }

    @Test
    public void calculatePidFahrenheitReturns100WithHeatGPIO() throws Exception {
        this.pid.setScale(C);
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));
        when(this.heatSettings.getProportional()).thenReturn(new BigDecimal(60));
        when(this.heatSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(100), result);
    }

    @Test
    public void calculatePidFahrenheitReturnsNeg100WithCoolGPIO() throws Exception {
        this.pid.setScale(C);
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(-100));
        when(this.coolSettings.getProportional()).thenReturn(new BigDecimal(60));
        when(this.coolSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(-100), result);
    }

    @Test
    public void calculatePidWith1CReturnsCloseWithHeatGPIO() throws Exception {
        BigDecimal avgTemp = new BigDecimal(99);
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));
        when(this.heatSettings.getProportional()).thenReturn(new BigDecimal(10));
        when(this.heatSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals("Result was: " + result.toPlainString(), 0, new BigDecimal(10.0).compareTo(result));
    }

    @Test
    public void calculatePidWithNeg1CReturnsCloseWithCoolGPIO() throws Exception {
        BigDecimal avgTemp = new BigDecimal(100);
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(99));
        when(this.coolSettings.getProportional()).thenReturn(new BigDecimal(10));
        when(this.coolSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals("Result was: " + result.toPlainString(), 0, new BigDecimal(-10.0).compareTo(result));
    }

    @Test
    public void calculatePidWith10CReturns100WithHeatGPIO() throws Exception {
        BigDecimal avgTemp = new BigDecimal(90);
        BigDecimal timeDiff = new BigDecimal(10);

        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));
        when(this.heatSettings.getProportional()).thenReturn(new BigDecimal(10));
        when(this.heatSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);

        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals("Result was: " + result.toPlainString(), 0, new BigDecimal(100.0).compareTo(result));
    }

    @Test
    public void calculatePidWithNeg10CReturns100WithCoolGPIO() throws Exception {
        BigDecimal avgTemp = new BigDecimal(100);
        BigDecimal timeDiff = new BigDecimal(10);

        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(90));
        when(this.coolSettings.getProportional()).thenReturn(new BigDecimal(10));
        when(this.coolSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);

        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals("Result was: " + result.toPlainString(), 0, new BigDecimal(-100.0).compareTo(result));
    }
}