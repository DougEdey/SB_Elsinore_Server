import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.triggers.TriggerInterface;
import com.sb.elsinore.triggers.WaitTrigger;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Created by doug on 25/09/15.
 */
@RunWith(PowerMockRunner.class)
public class TriggerTest {

    private TriggerControl mTriggerControl;

    @Test
    public void testTriggerInterface()
    {
        //mLaunchControl = new LaunchControl(8080);
        //doNothing().when(mLaunchControl.saveSettings());//.saveSettings();
        //mock(LaunchControl.class);
        try {
            spy(LaunchControl.class);
            doNothing().when(LaunchControl.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assert(false);
        }
        mTriggerControl = new TriggerControl();
        addTriggers();
        reorderTriggers();
        deleteSecondTrigger();
        //deleteAllTriggers();
    }

    private void reorderTriggers() {
        mTriggerControl.getTrigger(1).setPosition(2);
        mTriggerControl.getTrigger(2).setPosition(1);

        TriggerInterface triggerInterface = mTriggerControl.getTrigger(0);
        assert(triggerInterface instanceof WaitTrigger);
        assert("First".equals(((WaitTrigger) triggerInterface).getNote()));

        triggerInterface = mTriggerControl.getTrigger(1);
        assert(triggerInterface instanceof WaitTrigger);
        assert("Third".equals(((WaitTrigger) triggerInterface).getNote()));
    }

    private void deleteSecondTrigger() {
        mTriggerControl.delTriggerStep(1);
        assert(2 == mTriggerControl.getTriggersSize());

        TriggerInterface triggerInterface = mTriggerControl.getTrigger(0);
        assert(triggerInterface instanceof WaitTrigger);
        assert("First".equals(((WaitTrigger) triggerInterface).getNote()));

        triggerInterface = mTriggerControl.getTrigger(1);
        assert(triggerInterface instanceof WaitTrigger);
        assert("Second".equals(((WaitTrigger) triggerInterface).getNote()));
    }

    public void addTriggers()
    {
        addTrigger("1", "1", "First");
        addTrigger("2", "2", "Second");
        addTrigger("3", "3", "Third");
        System.out.println("Checking for the trigger list size");
        // Make sure there's three triggers
        assertEquals(3, mTriggerControl.getTriggersSize());
        System.out.println("Checking complete");
        TriggerInterface triggerInterface = mTriggerControl.getTrigger(0);
        assert(triggerInterface instanceof WaitTrigger);
        assertEquals("First", ((WaitTrigger) triggerInterface).getNote());

        triggerInterface = mTriggerControl.getTrigger(2);
        assert(triggerInterface instanceof WaitTrigger);
        assertEquals("Third", ((WaitTrigger) triggerInterface).getNote());
    }

    public void addTrigger(String m, String s, String n)
    {
        Map<String, String> jMap = new HashMap<>();
        jMap.put(WaitTrigger.WAITTIMEMINS, m);
        jMap.put(WaitTrigger.WAITTIMESECS, s);
        jMap.put(WaitTrigger.NOTES, n);

        JSONObject j = new JSONObject();
        j.putAll(jMap);

        mTriggerControl.addTrigger(0, "Wait", j);
    }
}
