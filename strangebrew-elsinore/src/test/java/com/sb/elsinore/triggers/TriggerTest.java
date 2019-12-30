package com.sb.elsinore.triggers;

import com.sb.common.TemperatureResult;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.hardware.OneWireController;
import com.sb.elsinore.wrappers.TempRunner;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Trigger creation tests
 * Created by doug on 25/09/15.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(LaunchControl.class)
@SpringBootTest
@PowerMockIgnore({ "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*",
"org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*" })
@ContextConfiguration(classes = {
    JpaDataConfiguration.class,
    TestJpaConfiguration.class,
    TestRestConfiguration.class})
public class TriggerTest {
    @MockBean
    private LaunchControl launchControl;
    @MockBean
    private OneWireController oneWireController;
    @InjectMocks
    private TriggerControl mTriggerControl;

    @Before
    public void setup() {
        this.launchControl = mock(LaunchControl.class);
        PowerMockito.mockStatic(LaunchControl.class);
        this.mTriggerControl = new TriggerControl();
    }

    @Test
    public void testTriggerInterface() {
        addTriggers();
        reorderTriggers();
        deleteSecondTrigger();
    }

    private void reorderTriggers() {
        this.mTriggerControl.getTrigger(1).setPosition(2);
        this.mTriggerControl.getTrigger(2).setPosition(1);
        this.mTriggerControl.sortTriggers();

        TriggerInterface triggerInterface = this.mTriggerControl.getTrigger(0);
        assertTrue(triggerInterface instanceof WaitTrigger);
        assertEquals("First", ((WaitTrigger) triggerInterface).getNote());

        triggerInterface = this.mTriggerControl.getTrigger(1);
        assertTrue(triggerInterface instanceof WaitTrigger);
        assertEquals("Third", ((WaitTrigger) triggerInterface).getNote());
    }

    private void deleteSecondTrigger() {
        this.mTriggerControl.delTriggerStep(1);
        assertEquals(2, this.mTriggerControl.getTriggersSize());

        TriggerInterface triggerInterface = this.mTriggerControl.getTrigger(0);
        assertTrue(triggerInterface instanceof WaitTrigger);
        assertEquals("First", ((WaitTrigger) triggerInterface).getNote());

        triggerInterface = this.mTriggerControl.getTrigger(1);
        assertTrue(triggerInterface instanceof WaitTrigger);
        assertEquals("Second", ((WaitTrigger) triggerInterface).getNote());
    }

    public void addTriggers() {
        addTrigger("1", "1", "First");
        addTrigger("2", "2", "Second");
        addTrigger("3", "3", "Third");
        System.out.println("Checking for the trigger list size");
        // Make sure there's three triggers
        assertEquals(3, this.mTriggerControl.getTriggersSize());
        System.out.println("Checking complete");
        TriggerInterface triggerInterface = this.mTriggerControl.getTrigger(0);
        assertTrue(triggerInterface instanceof WaitTrigger);
        assertEquals("First", ((WaitTrigger) triggerInterface).getNote());

        triggerInterface = this.mTriggerControl.getTrigger(2);
        assertTrue(triggerInterface instanceof WaitTrigger);
        assertEquals("Third", ((WaitTrigger) triggerInterface).getNote());
    }

    public void addTrigger(String m, String s, String n) {
        HashMap<String, String> jMap = new HashMap<>();
        jMap.put(WaitTrigger.WAITTIMEMINS, m);
        jMap.put(WaitTrigger.WAITTIMESECS, s);
        jMap.put(WaitTrigger.NOTES, n);

        JSONObject j = new JSONObject();
        j.putAll(jMap);

        this.mTriggerControl.addTrigger(-1, "Wait", j);
    }

    @Test
    public void testConversionOfTemp() throws InterruptedException {
        
        TempProbe localTempProbe = new TempProbe("Blank", "Blank");
        localTempProbe = spy(localTempProbe);
        TempRunner tempRunner = new TempRunner(localTempProbe, oneWireController);
        tempRunner = spy(tempRunner);

        when(this.launchControl.findTemp("temp")).thenReturn(tempRunner);

        Mockito.doReturn(new BigDecimal(40)).when(tempRunner).updateTemp();

        TemperatureTrigger firstTemperatureTrigger = new TemperatureTrigger(launchControl);
        firstTemperatureTrigger.init(0, "temp", 120, "Type A", "Method A");
        TemperatureTrigger secondTemperatureTrigger = new TemperatureTrigger(launchControl);
        secondTemperatureTrigger.init(1, "temp", 130, "Type B", "Method B");

        this.mTriggerControl.addTrigger(firstTemperatureTrigger);
        this.mTriggerControl.addTrigger(secondTemperatureTrigger);
        this.mTriggerControl.activateTrigger(0);
        tempRunner.updateTemp();

        assertTrue(firstTemperatureTrigger.isActive());
        tempRunner.updateTemp();
        firstTemperatureTrigger.waitForTrigger();

        // Make sure this comes back fine.
        localTempProbe.setScale("F");
        this.mTriggerControl.activateTrigger(1);
        assertTrue(secondTemperatureTrigger.isActive());

        doReturn(new BigDecimal(56)).when(tempRunner).updateTemp();
        secondTemperatureTrigger.waitForTrigger();
    }
}
