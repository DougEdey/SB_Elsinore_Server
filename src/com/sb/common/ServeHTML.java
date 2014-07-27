package com.sb.common;

import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.PID;
import com.sb.elsinore.Pump;


/**
 * Class used to generate the core HTML for the controller webpage.
 * @author doug Edey
 *
 */
public class ServeHTML {
    // This class is used to serve the webpage for the PID control
    // Also known as the Elsinore web page
    // I can easily refactor this for a better name later

    /**
     * Helper to get the current system line seperator.
     */
    private String lineSep = System.getProperty("line.separator");
    /**
     * Local cache of the devices list (with types).
     */
    private HashMap<String, String> devices;
    /**
     * Local list of the pump names.
     */
    private List<Pump> pumps;

    /**
     * Constructor to setup the class.
     * @param devList map of devices in the system with their types.
     * @param pumpList List of pumps in the system.
     */
    public ServeHTML(final HashMap<String, String> devList,
            final List<Pump> pumpList) {
        // no passed values, just generate the basic data
        devices = devList;
        pumps = pumpList;
    }

    /**
     * Generate the page header.
     * @return The Page header.
     */
    public final String getHeader() {
        String header = "";
        return header;
    }

    /**
     * The method to get the actual page.
     * @return The HTML page.
     */
    public final String getPage() {
        String page = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
            + "<meta http-equiv='Content-Type' "
            + "content='text/html; charset=utf-8'>"
            + "<meta content='text/html; charset=UTF-8' "
            + "http-equiv='Content-Type'/>" + lineSep;

        page += "<title>Elsinore Controller</title>" + lineSep
                + "<meta name=\"viewport\" content=\"width=device-width,"
                + " initial-scale=1.0\">" + lineSep
                + getHeader() + lineSep + "</head><body>" + lineSep
                + addJS() + lineSep
                + "<div id=\"error\" class-\"panel-error\">"
                + "</div><br />";

        Iterator<Entry<String, String>> devIterator =
                devices.entrySet().iterator();

        String pidContent = lineSep + " <div id='PIDs'>" + lineSep;

        while (devIterator.hasNext()) {
            Entry<String, String> pairs =
                    (Entry<String, String>) devIterator.next();
            String devName = pairs.getKey();
            String devType = pairs.getValue();
            if (devType.equalsIgnoreCase("PID")) {
                pidContent += addController(devName, devType);
            }
        }

        pidContent += lineSep + " </div>" + lineSep;

        // iterate the Temp devices to make them into a table
        devIterator = devices.entrySet().iterator();
        String tempContent = lineSep + " <div id='tempProbes'>" + lineSep;
        while (devIterator.hasNext()) {
            Entry<String, String> pairs =
                    (Entry<String, String>) devIterator.next();
            String devName = (String) pairs.getKey();
            String devType = (String) pairs.getValue();
            if (!devType.equalsIgnoreCase("PID")) {
                tempContent += addController(devName, devType);
            }
        }
        tempContent += lineSep + " </div>" + lineSep;

        // Add in the pumps
        // Declare this outside as an empty string since not everyone has these
        String pumpContent =
            "<div id='pumps'><div class=\"panel panel-info\">" + lineSep;
        pumpContent +=
            "<div id=\"pumps-title\" class=\"title panel-heading \">"
            + "Pumps</div>";
        pumpContent += "<div id=\"pumps-body\" class=\"panel-body\">";

        if (pumps != null && pumps.size() > 0) {
            Iterator<Pump> pumpIterator = pumps.iterator();

            while (pumpIterator.hasNext()) {
                pumpContent += addPump(pumpIterator.next().getName());
            }

        }
        pumpContent += "<span class='holo-button pump' id=\"New\" type=\"submit\""
                + "onclick='addPump(); sleep(2000); location.reload();'>"
                + "Add New Pump</span>";
        pumpContent += lineSep + "</div></div>";
        pumpContent += lineSep + " </div>" + lineSep;

        // TODO: Update this for custom page ordering, we should be able to do this quickly and easily
        page += pidContent;
        page += tempContent;
        page += pumpContent;

        page += "<div id=\"timers\">" + lineSep;
        page += addTimers();
        page += "</div>" + lineSep;
        page += "</body>";
        return page;
    }

    /**
     * get the Javascript html tags.
     * @return the HTML for embedding/linking the Javascript code.
     */
    public final String addJS() {
        String javascript =
            "<link rel=\"stylesheet\" type=\"text/css\" "
            + "href=\"/templates/static/raspibrew.css\" />" + lineSep
            + "<!-- Bootstrap -->" + lineSep
            + "<link "
            + "href=\"/templates/static/bootstrap-3.0.0/css/bootstrap.min.css\""
            + " rel=\"stylesheet\" media=\"screen\">" + lineSep
            + "<!--[if IE]>"
            + "<script type=\"text/javascript\" src=\"excanvas.js\">"
            + "</script><![endif]-->" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/jquery.js\"></script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/jquery.fs.stepper.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/segment-display.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/pidFunctions.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/raphael.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/justgage.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/tinytimer.min.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\""
            + " src=\"/templates/static/bootstrap-3.0.0/js/bootstrap.min.js\">"
            + "</script>" + lineSep
            + "<script type=\"text/javascript\">" + lineSep
            + "var update = 1;" + lineSep
            + "var GaugeDisplay = {}; " + lineSep
            + "var Gauges = {}; " + lineSep
            + "//long polling - wait for message" + lineSep
            + "jQuery(document).ready(function() {" + lineSep
            + "    waitForMsg();" + lineSep
            + "});" + lineSep
            + "</script>";

        return javascript;

    }

    /**
     * Get the HTML for the Controller on the device and type specified.
     * @param device The device name to create the object for
     * @param type "PID" or "Temp" to generate the correct output
     * @return The HTML String content
     */
    public final String addController(final String device, final String type) {
        String controller = "<div id=\"" + device
            + "\" class=\"holo-content controller panel panel-primary "
            + type + "\">" + lineSep
            + "<script type='text/javascript'>" + lineSep
            + "jQuery(document).ready(function() {" + lineSep
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
            + "GaugeDisplay[\"" + device + "\"].draw();" + lineSep;

        controller += "Gauges[\"" + device + "\"] = "
            + "new JustGage({ id: \"" + device + "-gage\","
            + " min: 0, max:100, relativeGaugeSize: true, title:\"Cycle\" }); " + lineSep
            + "$(\"input[type='number']\").stepper();" + lineSep;

        controller += "});" + lineSep + lineSep
            + "</script>" + lineSep;

        controller += "<div id=\"" + device
            + "-title\" class=\"title panel-heading \""
            + " ondblclick='editDevice(this)' >" + device + "</div>"
            + "<div id=\"" + device + "-error\" class-\"panel-error\">"
            + "</div>"
            + "<div class=\"panel-body\">" + lineSep
            + " <canvas id=\"" + device + "-tempGauge\" class=\"gauge\""
            + " width=\"300\" height=\"140\" onClick=\"showGraph(this)\">" + lineSep
            + "</canvas>" + lineSep
            + "<div id='" + device + "-tempSummary'>Temperature("
                + "<div id='tempUnit'>F</div>): " + lineSep
            + "<div id='" + device + "-tempStatus' >temp</div>&#176"
                + "<div id='tempUnit'>F</div>" + lineSep
            + "</div>" + lineSep
            + "</div>" + lineSep;


        controller +=
            "<div id=\"" + device + "-controls\" class='controller'>"
            + lineSep
            + "<div id=\"" + device + "-gage\" class='gage'></div>"
            + lineSep
            + "<form id=\"" + device + "-form\""
                + " class=\"controlPanelForm\" >"
            + lineSep
            + "<input type='hidden' name=\"mode\"/>"
            + "<div class=\"holo-buttons\">" + lineSep
            + "<button id=\"" + device + "-modeAuto\""
                + " class=\"holo-button modeclass\""
                + " onclick='disable(this);"
                + " selectAuto(this);"
                + " return false;'>Auto</button>"
            + lineSep
            + "<button id=\"" + device + "-modeHysteria\""
            + " class=\"holo-button modeclass\""
            + " onclick='disable(this);"
            + " selectHysteria(this);"
            + " return false;'>Hysteria</button>"
        + lineSep + "<br />"
            + "<button id=\"" + device + "-modeManual\""
                + " class=\"holo-button modeclass\""
                + " onclick='disable(this);"
                + " selectManual(this);"
                + " return false;'>Manual</button>"
            + lineSep
            + "<button id=\"" + device + "-modeOff\""
                + " class=\"holo-button modeclass\""
                + " onclick='disable(this);"
                + " selectOff(this);"
                + " return false;'>Off</button>"
            + lineSep
            + "</div>" + lineSep
            + "<table id='pidInput' class='labels table'>"

            // Set Point
            + "<tr id='" + device + "-SP' class='holo-field'>"
            + lineSep
                + "<td id='" + device + "-labelSP' >Set Point</td>"
            + lineSep
                + "<td id=\"" + device + "-setpoint\">"
                + lineSep
                    + "<input class='inputBox setpoint' type=\"text\""
                    + " name=\"setpoint\"  maxlength = \"4\""
                    + " size =\"4\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitSP'>"
                    + "<div id='tempUnit'>F</div>"
                + "</td>" + lineSep
            + "</tr>" + lineSep

            // DUTY CYCLE
            + "<tr id='" + device + "-DC' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelDC' >Duty Cycle</td>"
                + lineSep
                + "<td id=\"" + device + "-dutycycle\">" + lineSep
                    + "<input class='inputBox dutycycle'"
                        + " name=\"dutycycle\" maxlength = \"6\""
                        + " size =\"6\" value=\"\""
                        + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitDC'>%</td><br />"
            + "</tr>" + lineSep

                // DUTY TIME
            + "<tr id='" + device + "-DT' class='holo-field'>"
            + lineSep
                + "<td id='" + device + "-labelDT' >Duty Time:</td>"
                + lineSep
                + "<td id=\"" + device + "-cycletime\">" + lineSep
                + "<input class='inputBox dutytime' name=\"cycletime\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitDT'>secs</td>"
            + "</tr>" + lineSep

                 // P
            + "<tr id='" + device + "-p' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelp' >P</td>" + lineSep
                + "<td id=\"" + device + "-pinput\">" + lineSep
                    + "\t<input class='inputBox p' name=\"p\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitP'>secs/&#176"
                    + "<div id='tempUnit'>F</div>"
                + "</td>"
            + "</tr>" + lineSep

                // I
            + "<tr id='" + device + "-i' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labeli' >I</td>" + lineSep
                + "<td id=\"" + device + "-iinput\">" + lineSep
                    + "\t<input class='inputBox i' name=\"i\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitI'>&#176"
                    + "<div id='tempUnit'>F</div></td>"
            + "</tr>" + lineSep

                // D
            + "<tr id='" + device + "-d' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labeld' >D</td>" + lineSep
                + "<td id=\"" + device + "-dinput\">" + lineSep
                    + "\t<input class='inputBox d ' name=\"d\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitD'>secs</td>"
            + "</tr>" + lineSep
            // min
            + "<tr id='" + device + "-min' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelMin' >Min</td>" + lineSep
                + "<td id=\"" + device + "-mininput\">" + lineSep
                    + "\t<input class='inputBox min' name=\"min\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitMin'>&#176"
                    + "<div id='tempUnit'>F</div></td>"
            + "</tr>" + lineSep
            // max
            + "<tr id='" + device + "-max' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelMin' >Max</td>" + lineSep
                + "<td id=\"" + device + "-maxinput\">" + lineSep
                    + "\t<input class='inputBox max' name=\"max\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitMax'>&#176"
                    + "<div id='tempUnit'>F</div></td>"
            + "</tr>" + lineSep
            // minTime
            + "<tr id='" + device + "-time' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelTime' >Time</td>" + lineSep
                + "<td id=\"" + device + "-timeinput\">" + lineSep
                    + "\t<input class='inputBox time' name=\"time\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitTime'>Minutes</td>"
            + "</tr>" + lineSep
        + "</table>" + lineSep
        + "<div class='holo-buttons'>" + lineSep;

        controller += "<br/><button class='holo-button pump'"
            + " id=\"" + device + "Aux\""
            + " onClick='toggleAux(\"" + device + "\"),"
                    + " waitForMsg(); return false;' >"
            + lineSep
            + "Aux ON</button><br />";

        controller += "<input type='hidden' id='gpio' name='gpio' />"
            + "<input type='hidden' id='auxgpio' name='auxgpio' />"
            + "<input type='hidden' id='vol_ain' name='vol_ain' />"
            + "<input type='hidden' id='vol_add' name='vol_add' />"
            + "<input type='hidden' id='vol_off' name='vol_off' />"
            + "<input type='hidden' id='vol_units' name='vol_units' />"
            + "<button class='holo-button modeclass'"
            + " id=\"sendcommand\" type=\"submit\""
            + " value=\"SubmitCommand\""
            + " onClick='submitForm(this.form); waitForMsg();"
            + " return false;'>"
            + "Send Command</button>" + lineSep
            + "</div>" + lineSep
            + "</form>" + lineSep
            + "</div>" + lineSep; // finish off the Controller inputs


        controller += "<div id='" + device + "-volume' "
            + "ondblclick='editVolume(this)'></div>" + lineSep;
        controller += "</div>";

        return controller;
    }

    /**
     * Get the HTML content for this pump.
     * @param pumpName The name of the pump to generate
     * @return The HTML for the pump
     */
    final String addPump(final String pumpName) {
        String pumpDetails = lineSep;
        pumpDetails += "<button class='holo-button pump' id=\""
                + pumpName + "\" type=\"submit\" value=\"SubmitCommand\""
                + " onClick='submitPump(this); waitForMsg(); return false;'>"
                + pumpName + "</button>" + lineSep;
        return pumpDetails;
    }

    /**
     * Get the HTML for the list of timers.
     * @return The Timer HTML
     */
    final String addTimers() {
        String timers = lineSep;
        timers += "<div class='panel panel-info'>\n"
            + "<div class='title panel-heading'>Timers</div>";

        timers += "<div class='panel-body'>";

        for (String timer : LaunchControl.getTimerList()) {
            // Mash button
            timers += "<div><span class='holo-button pump' id=\""
                + timer + "\" type=\"submit\" "
                + "value=\"" + timer + "Timer\""
                    + " onclick='setTimer(this, \"" + timer + "\");"
                    + " waitForMsg(); return false;'>"
                    + "Start " + timer
                + "</span>" + lineSep;

            timers += "<span class='holo-button pump' style='display:none'"
                    + " id=\"" + timer + "Timer\" type=\"submit\" "
                    + "value=\"" + timer + "Timer\""
                    + " onclick='setTimer(this, \"" + timer + "\");"
                    + " waitForMsg(); return false;'>"
                + "</span>" + lineSep;

            timers += "<span class='holo-button pump' id=\"" + timer + "\""
                    + " type=\"submit\" "
                    + "value=\"" + timer + "Timer\""
                    + " onclick='resetTimer(this, \"" + timer + "\");"
                    + " waitForMsg(); return false;'>" + lineSep
                    + "Reset " + timer
                + "</span></div><br />" + lineSep;
        }

        timers += "<span class='holo-button pump' id=\"New\" type=\"submit\""
                + "onclick='addTimer(); sleep(2000); location.reload();'>"
                + "Add New Timer</span>" + lineSep;

        timers += "</div>" // panel body
                + "</div>"; // panel

        return timers;
    }

}
