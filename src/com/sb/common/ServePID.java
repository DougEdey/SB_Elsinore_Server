package com.sb.common;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import com.sb.elsinore.Pump;

public class ServePID {
	// This class is used to serve the webpage for the PID control
	// Also known as the Elsinore web page
	// I can easily refactor this for a better name later
	public String lineSep = System.getProperty("line.separator");
	private HashMap<String, String> devices;
	private List<Pump> pumps;
	
	public ServePID(HashMap<String, String> devList, List<Pump> pumpList) {
		// no passed values, just generate the basic data
		devices = devList;
		pumps = pumpList;
	}
	
	public String getHeader() {
		String header = "";
		return header;
	}
	
	public String getPage() {
		String page = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><meta content='text/html; charset=UTF-8' http-equiv='Content-Type'/>" + lineSep;
		page = page + "<title>Elsinore Controller</title>" + lineSep +
			    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + lineSep +
			    
				
				getHeader() + lineSep +
				"</head><body>" + lineSep +
				addJS() + lineSep;

		Iterator devIterator = devices.entrySet().iterator();
		
		
		String pidContent = lineSep + " <div id='PIDs'>" + lineSep;
		
		while ( devIterator.hasNext()) {
			Map.Entry pairs = (Map.Entry) devIterator.next();
			String devName = (String) pairs.getKey();
			String devType = (String) pairs.getValue();
			if (devType.equalsIgnoreCase("PID")) {
				pidContent += addController(devName, devType);
			}
		}
		
		pidContent += lineSep + " </div>" + lineSep;
		
		// iterate the Temp devices to make them into a table
		devIterator = devices.entrySet().iterator();
		String tempContent = lineSep + " <div id='tempProbes'>" + lineSep;
		while ( devIterator.hasNext()) {
			Map.Entry pairs = (Map.Entry) devIterator.next();
			String devName = (String) pairs.getKey();
			String devType = (String) pairs.getValue();
			if (!devType.equalsIgnoreCase("PID")) {
				tempContent += addController(devName, devType);
			}
		}
		tempContent += lineSep + " </div>" + lineSep;
		
		
		// Add in the pumps
		// Declare this outside as an empty string since not everyone has these
		String pumpContent = "";
		if (pumps != null && pumps.size() > 0) {
			Iterator<Pump> pumpIterator = pumps.iterator();
			
			pumpContent = "<div id='pumps' class=\"panel panel-info\">" + lineSep;	
			pumpContent += "<div id=\"pumps-title\" class=\"title panel-heading \">Pumps</div>";
			pumpContent += "<div id=\"pumps-body\" class=\"panel-body\">";
			while (pumpIterator.hasNext()) {
				pumpContent += addPump(pumpIterator.next().getName());
			}
			pumpContent += lineSep + "</div>";
			pumpContent += lineSep + " </div>" + lineSep;
		}
		
		// TODO: Update this for custom page ordering, we should be able to do this quickly and easily
		page += pidContent;
		page += tempContent;
		page += pumpContent;
		
		page += "</body>";
		return page;
	}
	
	public String addJS() {
		String javascript =  "<link rel=\"stylesheet\" type=\"text/css\" href=\"/templates/static/raspibrew.css\" />" + lineSep +  
				 "<!-- Bootstrap -->" + lineSep +
				"<link href=\"/templates/static/bootstrap-3.0.0/css/bootstrap.min.css\" rel=\"stylesheet\" media=\"screen\">" + lineSep + 
	        "<!--[if IE]><script type=\"text/javascript\" src=\"excanvas.js\"></script><![endif]-->" + lineSep +
	        
	        "<script type=\"text/javascript\" src=\"/templates/static/jquery.js\"></script>" + lineSep +   
		
	        "<script type=\"text/javascript\" src=\"/templates/static/jquery.fs.stepper.js\"></script>" + lineSep +

	        
	        "<script type=\"text/javascript\" src=\"/templates/static/segment-display.js\"></script>" + lineSep +
	        "<script type=\"text/javascript\" src=\"/templates/static/pidFunctions.js\"></script>" + lineSep +
			"<script type=\"text/javascript\" src=\"/templates/static/raphael.2.1.0.min.js\"></script>" + lineSep +
			"<script type=\"text/javascript\" src=\"/templates/static/justgage.js\"></script>" + lineSep +
			// BOOTSTRAP
			"<script type=\"text/javascript\" src=\"/templates/static/bootstrap-3.0.0/js/bootstrap.min.js\"></script>" + lineSep +
	        "<script type=\"text/javascript\">" + lineSep +
	        "var update = 1;" + lineSep +
	        "var GaugeDisplay = {}; " + lineSep +
	        "var Gauges = {}; " + lineSep;

		javascript +=  "//long polling - wait for message" + lineSep +
	         
	        "jQuery(document).ready(function() {" + lineSep +
	        "	waitForMsg();" + lineSep +
        	"});" + lineSep +
	        "</script>";
		
		return javascript;

	}
	
	public String addController(String device, String type) {
		String controller = "<div id=\"" + device + "\" class=\"holo-content controller panel panel-primary " + type +"\">" + lineSep +
					"<script type='text/javascript'>" + lineSep +
					"jQuery(document).ready(function() {" + lineSep +

					"GaugeDisplay[\"" + device + "\"] = new SegmentDisplay(\""+device +"-tempGauge\");" + lineSep +
					"GaugeDisplay[\"" + device + "\"].pattern         = \"###.##\";" + lineSep +
					"GaugeDisplay[\"" + device + "\"].angle    = 0;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].digitHeight     = 20;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].digitWidth      = 14;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].digitDistance   = 2.5;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].segmentWidth    = 2;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].segmentDistance = 0.3;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].segmentCount    = 7;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].cornerType      = 3;" + lineSep +
					"GaugeDisplay[\"" + device + "\"].colorOn         = \"#e95d0f\";" + lineSep +
					"GaugeDisplay[\"" + device + "\"].colorOff        = \"#4b1e05\";" + lineSep +
					"GaugeDisplay[\"" + device + "\"].draw();" + lineSep;
			if(type.equals("PID")) {
				controller += "Gauges[\"" + device + "\"] = new JustGage({ id: \"" + device + "-gage\", min: 0, max:100, title:\"Cycle\" }); " + lineSep +
					"$(\"input[type='number']\").stepper();"+ lineSep;
			}
				
				controller += "});" + lineSep + lineSep +
					"</script>" + lineSep;
					
				
				controller += "<div id=\"" + device + "-title\" class=\"title panel-heading \"" +
				
			
					">" + device + "</div><div class=\"panel-body\">" + lineSep +
					" <canvas id=\"" + device + "-tempGauge\" class=\"gauge\" width=\"300\" height=\"140\">" + lineSep +
					"</canvas>" + lineSep +
					"<div id='" + device + "-tempSummary'>Temperature(<div id='tempUnit'>F</div>): " + lineSep +
							"<div id='" + device + "-tempStatus' >temp</div>&#176<div id='tempUnit'>F</div>" + lineSep + 
					"</div>" + lineSep +
					"</div>" + lineSep;
				
			if(type.equals("PID")) {
				controller += "<div id=\"" + device + "-controls\" class='controller'>" + lineSep + 	
					"<div id=\"" + device + "-gage\" class='gage'></div>" + lineSep +
					"<form id=\""+ device + "-form\" class=\"controlPanelForm\" >" + lineSep +
					"<div class=\"holo-buttons\">" + lineSep +
					"<button id=\"" + device + "-modeAuto\" class=\"holo-button modeclass\" onclick='disable(this); selectAuto(this); return false;'>Auto</button>" + lineSep +
					"<button id=\"" + device + "-modeManual\" class=\"holo-button modeclass\" onclick='disable(this); selectManual(this); return false;'>Manual</button>" + lineSep +
                    "<button id=\"" + device + "-modeOff\" class=\"holo-button modeclass\" onclick='disable(this); selectOff(this); return false;'>Off</button>" + lineSep +
                    "</div>" + lineSep +
                    
                    "<table id='pidInput' class='labels table'>" +
                    	// 	SET POINT
                    	"<tr id='"+ device + "-SP' class='holo-field'>" + lineSep +
	                    	"<td id='"+ device + "-labelSP' >Set Point</td>"+ lineSep +
	                    	"<td id=\"" + device + "-setpoint\">" + lineSep +
	                    		"<input class='inputBox setpoint' type=\"text\" name=\"" + device + "-setpoint\"  maxlength = \"4\" size =\"4\" value=\"\" style=\"text-align: left;\"/>" +
	                    	"</td>" + lineSep +
	                    	"<td id='"+ device + "-unitSP'><div id='tempUnit'>F</div></td>"+ lineSep +
	                    "</tr>" + lineSep +
            
                    	// DUTY CYCLE 
                    	"<tr id='"+ device + "-DC' class='holo-field'>" + lineSep +
                    		"<td id='"+ device + "-labelDC' >Duty Cycle</td>" + lineSep +
                    
                    		"<td id=\"" + device + "-dutycycle\">" + lineSep +
                    			"<input class='inputBox dutycycle' name=\"" + device + "-dutycycle\" maxlength = \"6\" size =\"6\" value=\"\" style=\"text-align: left;\"/>" +
                    		"</td>" + lineSep +
                    	"<td id='"+ device + "-unitDC'>%</td><br />"+ 
                    	"</tr>" + lineSep +
                    	
                    	// DUTY TIME
                    	"<tr id='"+ device + "-DT' class='holo-field'>" + lineSep +
                    	"<td id='"+ device + "-labelDT' >Duty Time:</td>" + lineSep +
                 			"<td id=\"" + device + "-cycletime\">" + lineSep +
                 				"<input class='inputBox dutytime' name=\"" + device + "-cycletime\" maxlength = \"6\" size =\"6\" value=\"\" style=\"text-align: left;\"/>" +
                 			"</td>" + lineSep +
                 		"<td id='"+ device + "-unitDT'>secs</td>"+ 
                 		"</tr>" + lineSep +
                 		
                 		// P
	                    "<tr id='"+ device + "-p' class='holo-field'>" + lineSep +
	                    "<td id='"+ device + "-labelp' >P</td>" + lineSep +
		                    "<td id=\"" + device + "-pinput\">" + lineSep +
		                    "	<input class='inputBox p' name=\"" + device + "-p\"  maxlength = \"6\" size =\"6\" value=\"\" style=\"text-align: left;\"/>" +
		                    "</td>" + lineSep +
		                    "<td id='"+ device + "-unitP'>secs#176<div id='tempUnit'>F</div></td>"+ 
		                "</tr>" + lineSep +
		                
		                // I
	                    "<br />" + "<tr id='"+ device + "-i' class='holo-field'>" + lineSep +
	                    "<td id='"+ device + "-labeli' >I</td>" + lineSep +
		                    "<td id=\"" + device + "-iinput\">" + lineSep +
		                    "	<input class='inputBox i' name=\"" + device + "-i\"  maxlength = \"6\" size =\"6\" value=\"\" style=\"text-align: left;\"/>" +
		                    "</td>" + lineSep +
		                    "<td id='"+ device + "-unitI'>&#176<div id='tempUnit'>F</div></td>" +
		                "</tr>" + lineSep +  
		                
		                // D
	                    "<br />" + "<tr id='"+ device + "-d' class='holo-field'>" + lineSep +
	                    "<td id='"+ device + "-labeld' >D</td>" + lineSep +
		        			"<td id=\"" + device + "-dinput\">" + lineSep +
				                    "	<input class='inputBox d ' name=\"" + device + "-d\"  maxlength = \"6\" size =\"6\" value=\"\" style=\"text-align: left;\"/>" +
							"</td>" + lineSep +
							"<td id='"+ device + "-unitD'>secs</td>" +
						"</tr>" + lineSep +	
					"</table>" + lineSep +

					"<div class='holo-buttons'>" + lineSep +
                    "<button class='holo-button modeclass' id=\"sendcommand\" type=\"submit\" value=\"SubmitCommand\" onClick='submitForm(this.form); waitForMsg(); return false;'>Send Command</button>" + lineSep +
                    "</div>" + lineSep +
					"</form>" + lineSep +
					"</div>" + lineSep; // finish off the Controller inputs
		}
			
		controller += 	"<div class='" + device + "-volume'></div>" + lineSep;
		controller +=	"</div>";
		
		return controller;
	}
	
	String addPump(String pumpName) {
		String pumpDetails = lineSep;
        pumpDetails += "<button class='holo-button pump' id=\"" + pumpName + "\" type=\"submit\" value=\"SubmitCommand\" onClick='submitPump(this); waitForMsg(); return false;'>" + pumpName + "</button>" + lineSep;
        
		
		return pumpDetails;
	}
	

}
