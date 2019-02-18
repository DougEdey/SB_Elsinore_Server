package com.sb.elsinore;

import com.sb.elsinore.devices.PID;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.interfaces.PIDSettingsInterface;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import com.sb.elsinore.hardware.OneWireController;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by Douglas on 2016-10-20.
 */
public class PIDModelRunnerTest {

    private PIDRunner pidRunner;
    @Mock
    private PID pid;
    @Mock
    private TempProbe tempProbe;
    @Mock
    private OneWireController oneWireController;


    @Spy
    private PIDSettingsInterface heatSettings;
    @Spy
    private PIDSettingsInterface coolSettings;


    @Before
    public void setup() throws Exception {
        initMocks(this);
        this.pidRunner = spy(new PIDRunner(this.pid, this.oneWireController));

        when(this.pidRunner.getTempInterface()).thenReturn(this.tempProbe);
        when(this.pid.getHeatSetting()).thenReturn(this.heatSettings);
        when(this.pid.getCoolSetting()).thenReturn(this.coolSettings);
    }

    @Test
    public void calculatePidFahrenheitReturnsZeroWithNoSettingsF() throws Exception {
        this.tempProbe.setScale("F");
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));

        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(0), result);
    }

    @Test
    public void calculatePidFahrenheitReturnsZeroWithNoSettingsC() throws Exception {
        this.tempProbe.setScale("C");
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));

        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(0), result);
    }

    @Test
    public void calculatePidFahrenheitReturnsZeroNoHeatGPIO() throws Exception {
        this.tempProbe.setScale("C");
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
        this.tempProbe.setScale("C");
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(100));
        when(this.heatSettings.getProportional()).thenReturn(new BigDecimal(60));
        when(this.heatSettings.getIntegral()).thenReturn(new BigDecimal(0));
        when(this.heatSettings.getDerivative()).thenReturn(new BigDecimal(0));
        when(this.heatSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);
        BigDecimal avgTemp = new BigDecimal(10);
        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals(new BigDecimal(100), result);
    }

    @Test
    public void calculatePidFahrenheitReturnsNeg100WithCoolGPIO() throws Exception {
        this.tempProbe.setScale("C");
        BigDecimal timeDiff = new BigDecimal(10);
        when(this.pid.getSetPoint()).thenReturn(new BigDecimal(-100));
        when(this.coolSettings.getProportional()).thenReturn(new BigDecimal(60));
        when(this.coolSettings.getIntegral()).thenReturn(new BigDecimal(0));
        when(this.coolSettings.getDerivative()).thenReturn(new BigDecimal(0));
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
        when(this.heatSettings.getIntegral()).thenReturn(new BigDecimal(0));
        when(this.heatSettings.getDerivative()).thenReturn(new BigDecimal(0));
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
        when(this.coolSettings.getIntegral()).thenReturn(new BigDecimal(0));
        when(this.coolSettings.getDerivative()).thenReturn(new BigDecimal(0));
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
        when(this.heatSettings.getIntegral()).thenReturn(new BigDecimal(0));
        when(this.heatSettings.getDerivative()).thenReturn(new BigDecimal(0));
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
        when(this.coolSettings.getIntegral()).thenReturn(new BigDecimal(0));
        when(this.coolSettings.getDerivative()).thenReturn(new BigDecimal(0));
        when(this.coolSettings.getGPIO()).thenReturn("GPIO_7");
        when(this.pidRunner.calculateTimeDiff()).thenReturn(timeDiff);

        BigDecimal result = this.pidRunner.calculate(avgTemp);
        assertEquals("Result was: " + result.toPlainString(), 0, new BigDecimal(-100.0).compareTo(result));
    }
}