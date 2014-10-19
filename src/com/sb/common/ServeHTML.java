package com.sb.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.PID;
import com.sb.elsinore.Pump;
import com.sb.elsinore.Temp;
import com.sb.elsinore.Timer;


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
            + "<link rel='shortcut icon' href=''/favicon.ico?v=2' />"
            + "<meta http-equiv='Content-Type' "
            + "content='text/html; charset=utf-8'>"
            + "<meta content='text/html; charset=UTF-8' "
            + "http-equiv='Content-Type'/>" + lineSep
            + "<meta charset='UTF-8' />" + lineSep;

        page += "<title>Elsinore "+Messages.CONTROLLER+"</title>" + lineSep
                + "<meta name=\"viewport\" content=\"width=device-width,"
                + " initial-scale=1.0\">" + lineSep
                + getHeader() + lineSep + "</head><body>" + lineSep
                + addJS() + lineSep
                + "<div class=\"page-header\" style='margin:0; display:inline; width=100%'>"
                + "<h1 onClick='editBreweryName();' class='brewerynameheader'><div id='breweryname'>Elsinore</div> "
                + "<small id='brewerysubtext'>"
                    + "<a href='http://dougedey.github.io/SB_Elsinore_Server/'>"
                    + "StrangeBrew Elsinore"
                    + "</a>"
                + "</small>"
                + "</h1>"
                + "<div class='imageheader'><img height=\"200\" width=\"200\" id='brewerylogo' src='' style='display:none;'/>"
                + "<br/>"
                + "<input type='file' id='logo' data-url='uploadImage'/>"
                + "</div>"
                + "</div><br />";

        // For displaying any info from the server
        String messageContent =
            "<div id='messages' style='display:none'>"
            + "<div class=\"panel panel-warning\">" + lineSep;
        messageContent +=
            "<div id=\"messages-title\" class=\"title panel-heading\">"
            + Messages.SERVER_MESSAGES + "</div>";
        messageContent += "<div id=\"messages-body\" class=\"panel-body\">"
                + "</div>"
                + "</div>"
                + "</div>";

        page += messageContent;

        Iterator<Entry<String, String>> devIterator =
                devices.entrySet().iterator();

        String pidContent = lineSep + " <div id='PIDs'>" + lineSep;

        while (devIterator.hasNext()) {
            Entry<String, String> pairs =
                    (Entry<String, String>) devIterator.next();
            String devName = pairs.getKey().replace(" ", "_");
            String devType = pairs.getValue();
            Temp tTemp = LaunchControl.findTemp(devName);
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
            devName = devName.replace(" ", "_");
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
            "<div id=\"pumps-titled\" class=\"title panel-heading \">"
            + Messages.PUMPS + "</div>";
        pumpContent += "<div id=\"pumps-body\" class=\"panel-body\">";

        Collections.sort(pumps);
        if (pumps != null && pumps.size() > 0) {
            Iterator<Pump> pumpIterator = pumps.iterator();

            while (pumpIterator.hasNext()) {
                pumpContent += addPump(pumpIterator.next().getName());
            }

        }
        pumpContent += "<span class='holo-button pump' id=\"NewPump\""
                + " type=\"submit\""
                + "ondrop='dropDeletePump(event);' "
                + "ondragover='allowDropPump(event);'"
                + "onclick='addPump()'>"
                + Messages.NEW_PUMP + "</span>";
        pumpContent += lineSep + "</div></div>";
        pumpContent += lineSep + " </div>" + lineSep;

        // TODO: Update this for custom page ordering,
        // we should be able to do this quickly and easily
        page += pidContent;
        page += tempContent;
        page += pumpContent;

        page += "<div id=\"timers\">" + lineSep;
        page += addTimers();
        page += "</div>" + lineSep;
        page += "<br /> <br /><div><span class='holo-button' id=\"CheckUpdates\""
                + " type=\"submit\""
                + " onClick='checkUpdates();'>"
                + Messages.UPDATE_CHECK+"</span>"
                + "</div>";
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
            + "href=\"/templates/static/elsinore.css\" />" + lineSep
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
            + "<script type=\"text/javascript\" src=\"/templates/static/moment.js\"></script>" + lineSep
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
            + "<script src=\"/templates/static/file/jquery.ui.widget.js\"></script>"
            + "<script src=\"/templates/static/file/jquery.iframe-transport.js\"></script>"
            + "<script src=\"/templates/static/file/jquery.fileupload.js\"></script>"
            + "<script type=\"text/javascript\">" + lineSep
            + "var update = 1;" + lineSep
            + "var GaugeDisplay = {}; " + lineSep
            + "var Gauges = {}; " + lineSep
            + "//long polling - wait for message" + lineSep
            + "jQuery(document).ready(function() {" + lineSep
            + "    setup();" + lineSep
            + "});" + lineSep
            + "</script>"
        + "<script language=\"javascript\" type=\"text/javascript\" src=\"/templates/static/jquery.flot.js\"></script>" + lineSep
        + "<script language=\"javascript\" type=\"text/javascript\" src=\"/templates/static/jquery.flot.axislabels.js\"></script>" + lineSep
        + "<script language=\"javascript\" type=\"text/javascript\" src=\"/templates/static/jquery.flot.time.js\"></script>" + lineSep
        + "<script language=\"javascript\" type=\"text/javascript\" src=\"/templates/static/jquery.i18n.properties.js\"></script>" + lineSep;

        return javascript;

    }

    /**
     * Get the HTML for the Controller on the device and type specified.
     * @param device The device name to create the object for
     * @param type "PID" or "Temp" to generate the correct output
     * @return The HTML String content
     */
    public final String addController(final String device, final String type) {
        Temp tTemp = LaunchControl.findTemp(device);
        if (LaunchControl.isLocked() && !tTemp.isSetup()) {
            return "";
        }

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
            + " onclick='editDevice(this)' >" + device.replace("_", " ") + "</div>"
            + "<div id=\"" + device + "-error\" class-\"panel-error\">"
            + "</div>"
            + "<div class=\"panel-body\">" + lineSep
            + " <canvas id=\"" + device + "-tempGauge\" class=\"gauge\""
            + " width=\"300\" height=\"140\">" + lineSep
            + "</canvas>" + lineSep
            + "<div id='" + device + "-tempSummary'>" + Messages.TEMPERATURE + "("
                + "<div id='tempUnit'>F</div>): " + lineSep
            + "<div id='" + device + "-tempStatus' >temp</div>&#176"
                + "<div id='tempUnit'>F</div>" + lineSep
            + "</div>" + lineSep;
        
        if (LaunchControl.recorderEnabled()) {
            controller += "<div id=\"" + device
            + "-graph_wrapper\" class=\"holo-content controller panel panel-info\">"
            + "<div id='" + device + "-graph_title' class=\"title panel-heading\""
                    + " onclick='embedGraph(\"" + device + "\"); toggleBlock(\""+device + "-graph_body\");' >"
            + Messages.SHOW_GRAPH + "</div>"
            + "<div id='" + device + "-graph_body' onclick='showGraph(this);' class=\"panel-body\">"
            + "</div></div>";
        }    
        controller += "</div>" + lineSep;


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
                + " return false;'>"+Messages.AUTO+"</button>"
            + lineSep
            + "<button id=\"" + device + "-modeHysteria\""
            + " class=\"holo-button modeclass\""
            + " onclick='disable(this);"
            + " selectHysteria(this);"
            + " return false;'>"+Messages.HYSTERIA+"</button>"
        + lineSep + "<br />"
            + "<button id=\"" + device + "-modeManual\""
                + " class=\"holo-button modeclass\""
                + " onclick='disable(this);"
                + " selectManual(this);"
                + " return false;'>"+Messages.MANUAL+"</button>"
            + lineSep
            + "<button id=\"" + device + "-modeOff\""
                + " class=\"holo-button modeclass\""
                + " onclick='disable(this);"
                + " selectOff(this);"
                + " return false;'>"+Messages.PID_OFF+"</button>"
            + lineSep
            + "</div>" + lineSep
            + "<table id='pidInput' class='labels table'>"

            // Set Point
            + "<tr id='" + device + "-SP' class='holo-field'>"
            + lineSep
                + "<td id='" + device + "-labelSP' >"+Messages.SET_POINT+"</td>"
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
                + "<td id='" + device + "-labelDC' >"+Messages.DUTY_CYCLE+"</td>"
                + lineSep
                + "<td id=\"" + device + "-dutycycle\">" + lineSep
                    + "<input class='inputBox dutycycle'"
                        + " name=\"dutycycle\" maxlength = \"6\""
                        + " size =\"6\" value=\"\""
                        + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitDC'>%</td><br />"
            + "</tr>" + lineSep

            // Add in the tabs
            + "<!-- Nav tabs -->" + lineSep
            + "<tr id='" + device + "-tabbedInputs'><td colspan='3'>"
            + "<ul class=\"nav nav-tabs\" role=\"tablist\" id='inputtabs'>"
            + lineSep
            + "<li class=\"active\"><a href=\"#heat\" role=\"tab\" data-toggle=\"tab\">"
            + Messages.HEAT + "</a></li>" + lineSep
            + "<li><a href=\"#cool\" role=\"tab\" data-toggle=\"tab\">"
            + Messages.COOL + "</a></li>" + lineSep
            + "</ul>"

                // DUTY TIME
            + "<div class=\"tab-content\">" + lineSep
            + "<div class=\"tab-pane active\" id='heat'>" + lineSep
            + "<table><tr id='" + device + "-heatDT' class='holo-field'>"
            + lineSep
            + "<td id='" + device + "-labelDT' >" + Messages.DUTY_TIME + "</td>"
            + lineSep
            + "<td id=\"" + device + "-cycletime\">" + lineSep
            + "<input class='inputBox heatdutytime' name=\"heatcycletime\""
            + " maxlength = \"6\" size =\"6\" value=\"\""
            + " style=\"text-align: left;\"/>"
            + "</td>" + lineSep
            + "<td id='" + device + "-unitDT'>" + Messages.SECS + "</td>"
            + "</tr>" + lineSep

                 // P
            + "<tr id='" + device + "-heatp' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelp' >P</td>" + lineSep
                + "<td id=\"" + device + "-pinput\">" + lineSep
                    + "\t<input class='inputBox heatp' name=\"heatp\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitP'>"+Messages.SECS+"/&#176"
                    + "<div id='tempUnit'>F</div>"
                + "</td>"
            + "</tr>" + lineSep

                // I
            + "<tr id='" + device + "-heati' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labeli' >I</td>" + lineSep
                + "<td id=\"" + device + "-iinput\">" + lineSep
                    + "\t<input class='inputBox heati' name=\"heati\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitI'>&#176"
                    + "<div id='tempUnit'>F</div></td>"
            + "</tr>" + lineSep

                // D
            + "<tr id='" + device + "-heatd' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labeld' >D</td>" + lineSep
                + "<td id=\"" + device + "-dinput\">" + lineSep
                    + "\t<input class='inputBox heatd ' name=\"heatd\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitD'>"+Messages.SECS+"</td>"
            + "</tr></table>" + lineSep
            + "</div>"

            // COOLING
            // DUTY TIME
            + "<div class=\"tab-pane\" id='cool'>" + lineSep
            + "<table><tr id='" + device + "-coolDT' class='holo-field'>"
            + lineSep
                + "<td id='" + device + "-labelDT' >"+Messages.DUTY_TIME+"</td>"
                + lineSep
                + "<td id=\"" + device + "-cycletime\">" + lineSep
                + "<input class='inputBox cooldutytime' name=\"coolcycletime\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitDT'>"+Messages.SECS+"</td>"
            + "</tr>" + lineSep

                 // P
            + "<tr id='" + device + "-coolp' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelp' >P</td>" + lineSep
                + "<td id=\"" + device + "-pinput\">" + lineSep
                    + "\t<input class='inputBox coolp' name=\"coolp\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitP'>"+Messages.SECS+"/&#176"
                    + "<div id='tempUnit'>F</div>"
                + "</td>"
            + "</tr>" + lineSep

                // I
            + "<tr id='" + device + "-cooli' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labeli' >I</td>" + lineSep
                + "<td id=\"" + device + "-iinput\">" + lineSep
                    + "\t<input class='inputBox cooli' name=\"cooli\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitI'>&#176"
                    + "<div id='tempUnit'>F</div></td>"
            + "</tr>" + lineSep

                // D
            + "<tr id='" + device + "-coold' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labeld' >D</td>" + lineSep
                + "<td id=\"" + device + "-dinput\">" + lineSep
                    + "\t<input class='inputBox coold ' name=\"coold\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitD'>"+Messages.SECS+"</td>"
            + "</tr>" + lineSep

            // DELAY
            + "<tr id='" + device + "-cooldelay' class='holo-field'>" + lineSep
            + "<td id='" + device + "-labeldelay' >"
                + Messages.DELAY + "</td>" + lineSep
            + "<td id=\"" + device + "-delayinput\">" + lineSep
                + "\t<input class='inputBox cooldelay ' name=\"cooldelay\""
                + " maxlength = \"6\" size =\"6\" value=\"\""
                + " style=\"text-align: left;\"/>"
            + "</td>" + lineSep
            + "<td id='" + device + "-unitDelay'>"+Messages.MINUTES+"</td>"
        + "</td></tr></table>" + lineSep
            + "</div>" + lineSep
        +"</tr>"
            // min
            + "<tr id='" + device + "-min' class='holo-field'>" + lineSep
                + "<td id='" + device + "-labelMin' >"+Messages.MIN+"</td>" + lineSep
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
                + "<td id='" + device + "-labelMin' >"+Messages.MAX+"</td>" + lineSep
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
                + "<td id='" + device + "-labelTime' >"+Messages.TIME+"</td>" + lineSep
                + "<td id=\"" + device + "-timeinput\">" + lineSep
                    + "\t<input class='inputBox time' name=\"time\""
                    + " maxlength = \"6\" size =\"6\" value=\"\""
                    + " style=\"text-align: left;\"/>"
                + "</td>" + lineSep
                + "<td id='" + device + "-unitTime'>"+Messages.MINUTES+"</td>"
            + "</tr>" + lineSep
        + "</table>" + lineSep
        + "<div class='holo-buttons'>" + lineSep;

        controller += "<br/><button class='holo-button pump'"
            + " id=\"" + device + "Aux\""
            + " onClick='toggleAux(\"" + device + "\"),"
                    + " waitForMsg(); return false;' >"
            + lineSep
            + Messages.AUX_ON +"</button><br />";

        controller += "<input type='hidden' id='deviceaddr' name='deviceaddr' />"
            + "<input type='hidden' id='heatgpio' name='heatgpio' />"
            + "<input type='hidden' id='coolgpio' name='coolgpio' />"
            + "<input type='hidden' id='auxgpio' name='auxgpio' />"
            + "<input type='hidden' id='cutoff' name='cutoff' />"
            + "<input type='hidden' id='calibration' name='calibration' />"
            + "<input type='hidden' id='vol_ain' name='vol_ain' />"
            + "<input type='hidden' id='vol_add' name='vol_add' />"
            + "<input type='hidden' id='vol_off' name='vol_off' />"
            + "<input type='hidden' id='vol_units' name='vol_units' />"
            + "<button class='holo-button modeclass'"
            + " id=\"sendcommand\" type=\"submit\""
            + " value=\"SubmitCommand\""
            + " onClick='submitForm(this.form); waitForMsg();"
            + " return false;'>"
            + Messages.SEND_COMMAND+"</button>" + lineSep
            + "</div>" + lineSep
            + "</form>" + lineSep
            + "</div>" + lineSep; // finish off the Controller inputs


        controller += "<div id='" + device + "-volume' "
            + "onclick='editVolume(this)'></div>" + lineSep;
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
        pumpDetails += "<div id='div-" + pumpName.replaceAll(" ", "_") + "' class='pump_wrapper'"
                + " ondragstart='dragPump(event);' draggable='true'"
                + " ondrop='dropPump(event);'"
                + " ondragover='allowDropPump(event);'"
                + " ondragleave='leavePump(event);'>"

                + "<button class='holo-button pump' id=\""
                + pumpName.replaceAll(" ", "_") + "\" type=\"submit\" value=\"SubmitCommand\""
                + " onClick='submitPump(this); waitForMsg(); return false;'>"
                + pumpName + "</button></div>" + lineSep;
        return pumpDetails;
    }

    /**
     * Get the HTML for the list of timers.
     * @return The Timer HTML
     */
    final String addTimers() {
        String timers = lineSep;
        timers += "<div class='panel panel-info'>\n"
            + "<div id='timers-header' class='title panel-heading'>Timers</div>";

        timers += "<div id='timers-body' class='panel-body'>";
        for (Timer timer : LaunchControl.getTimerList()) {
            // Mash button
            timers += "<div id='div-" + timer.getSafeName() + "'"
                + " ondragstart='dragTimer(event);' draggable='true'"
                + " ondrop='dropTimer(event);'"
                + " ondragover='allowDropTimer(event);'"
                + " ondragleave='leaveTimer(event);'"
                + " class='timer_wrapper'>"
                + "<div class='timerName holo'>" + timer.getName() + "</div>"
                + "<span class='holo-button pump' id=\""
                + timer.getSafeName() + "\" type=\"submit\" "
                + "value=\"" + timer.getName() + "Timer\""
                    + " onclick='setTimer(this, \"" + timer.getName() + "\");"
                    + " waitForMsg(); return false;'>"
                    + Messages.START
                + "</span>" + lineSep;

            timers += "<span class='holo-button pump' style='display:none'"
                    + " id=\"" + timer.getSafeName() + "Timer\" type=\"submit\" "
                    + "value=\"" + timer.getName() + "Timer\""
                    + " onclick='setTimer(this, \"" + timer.getName() + "\");"
                    + " waitForMsg(); return false;'>"
                + "</span>" + lineSep;

            timers += "<span class='holo-button pump' id=\""
                    + timer.getSafeName() + "\""
                    + " type=\"submit\" "
                    + "value=\"" + timer.getName() + "Timer\""
                    + " onclick='resetTimer(this, \"" + timer.getName() + "\");"
                    + " waitForMsg(); return false;'>" + lineSep
                    + Messages.RESET
                + "</span></div>" + lineSep;
        }

        timers += "<span class='holo-button pump' id=\"NewTimer\" type=\"submit\""
                + "onclick='addTimer();' "
                + "ondrop='dropDeleteTimer(event);' "
                + "ondragover='allowDropTimer(event);' >"
                + Messages.NEW_TIMER+"</span>" + lineSep;

        timers += "</div>" // panel body
                + "</div>"; // panel

        return timers;
    }

}
