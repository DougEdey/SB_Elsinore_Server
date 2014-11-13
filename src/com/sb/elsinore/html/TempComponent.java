package com.sb.elsinore.html;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import com.sb.elsinore.Temp;

public class TempComponent implements Renderable {
    public static String lineSep = System.getProperty("line.separator");
    private Temp temp = null;
    
    public TempComponent(Temp temp) {
        this.temp = temp;
    }
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        
    }

    public static String getGaugeScript(String device) {

        return "jQuery(document).ready(function() {" + lineSep
        + "GaugeDisplay[\"" + device + "\"] = new SegmentDisplay(\""
        + device + "-tempGauge\");" + lineSep
        + "GaugeDisplay[\"" + device + "\"].pattern = \"###.##\";" + lineSep
        + "GaugeDisplay[\"" + device + "\"].angle = 0;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].digitHeight = 20;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].digitWidth = 14;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].digitDistance = 2.5;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].segmentWidth = 2;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].segmentDistance = 0.3;"
        + lineSep
        + "GaugeDisplay[\"" + device + "\"].segmentCount = 7;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].cornerType = 3;" + lineSep
        + "GaugeDisplay[\"" + device + "\"].colorOn = \"#e95d0f\";"
        + lineSep
        + "GaugeDisplay[\"" + device + "\"].colorOff = \"#4b1e05\";"
        + lineSep
        + "GaugeDisplay[\"" + device + "\"].draw();" + lineSep
        + "Gauges[\"" + device + "\"] = "
            + "new JustGage({ id: \"" + device + "-gage\","
            + " min: 0, max:100, relativeGaugeSize: true, title:\"Cycle\" }); "
            + lineSep
            + "$(\"input[type='number']\").stepper();" + lineSep
        + "});" + lineSep + lineSep;
    }

}
