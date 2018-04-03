import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.triggers.TemperatureTrigger;
import com.sb.elsinore.triggers.TriggerInterface;
import com.sb.elsinore.triggers.WaitTrigger;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Trigger creation tests
 * Created by doug on 25/09/15.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LaunchControl.class)
public class TriggerTest {

    private TriggerControl mTriggerControl;
    private LaunchControl launchControl;

    @Before
    public void setup() {
        this.launchControl = mock(LaunchControl.class);
        PowerMockito.mockStatic(LaunchControl.class);
        try {
            Mockito.when(LaunchControl.getInstance()).thenReturn(this.launchControl);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        this.mTriggerControl = new TriggerControl();
    }

    @Test
    public void testTriggerInterface() {

        //doNothing().when(mLaunchControl.saveSettings());//.saveSettings();

        addTriggers();
        reorderTriggers();
        deleteSecondTrigger();
        //deleteAllTriggers();
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
        when(this.launchControl.findTemp("temp")).thenReturn(localTempProbe);
        //doReturn(new BigDecimal(40)).when(localTempProbe).getTempProbe();
        doReturn(new BigDecimal(40)).when(localTempProbe).updateTempFromFile();


        TemperatureTrigger firstTemperatureTrigger = new TemperatureTrigger(0, "temp", 120, "Type A", "Method A");
        TemperatureTrigger secondTemperatureTrigger = new TemperatureTrigger(1, "temp", 130, "Type B", "Method B");
        this.mTriggerControl.addTrigger(firstTemperatureTrigger);
        this.mTriggerControl.addTrigger(secondTemperatureTrigger);
        this.mTriggerControl.activateTrigger(0);

        assertTrue(firstTemperatureTrigger.isActive());

        //doReturn(new BigDecimal(54.44)).when(localTempProbe).getTempProbe();
        doReturn(new BigDecimal(54.44)).when(localTempProbe).updateTempFromFile();
        firstTemperatureTrigger.waitForTrigger();
        // Make sure this comes back fine.

        localTempProbe.setScale("F");
        this.mTriggerControl.activateTrigger(1);
        assertTrue(secondTemperatureTrigger.isActive());

        //doReturn(new BigDecimal(40)).when(localTempProbe).getTempProbe();
        doReturn(new BigDecimal(56)).when(localTempProbe).updateTempFromFile();
        secondTemperatureTrigger.waitForTrigger();
    }
}
