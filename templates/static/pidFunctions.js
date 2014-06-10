$.fn.serializeObject = function()
{
    var o = {};
    var a = this.serializeArray();
    $.each(a, function() {
        if (o[this.name] !== undefined) {
            if (!o[this.name].push) {
                o[this.name] = [o[this.name]];
            }
            o[this.name].push(this.value || '');
        } else {
            o[this.name] = this.value || '';
        }
    });
    return o;
};

function sleep(milliseconds) {
  var start = new Date().getTime();
  for (var i = 0; i < 1e7; i++) {
    if ((new Date().getTime() - start) > milliseconds){
      break;
    }
  }
}

/**
 * Return an Object sorted by it's Key
 */
var sortObjectByKey = function(obj){
    var keys = [];
    var sorted_obj = {};

    for(var key in obj){
        if(obj.hasOwnProperty(key)){
            keys.push(key);
        }
    }

    // sort keys
    keys.sort();

    // create new array based on Sorted Keys
    jQuery.each(keys, function(i, key){
        sorted_obj[key] = obj[key];
    });

    return sorted_obj;
};

function waitForMsg(){
	if (window.disableUpdates) {
		return false;
	}
	jQuery.ajax({
		type: 'GET',
		url: '/getstatus',
		dataType: 'json',
		async: true,
		cache: false,
		timeout:5000,
		success: function(data){
			if(data == null) 
				return;
		
			$.each(data, function(key, val) {
				vessel = key;
				
				if (vessel == "brewday") {
					$.each(val, function(timerName, timerStatus) {
						checkTimer(timerStatus, timerName);
					});
					return true;
					
				}

				// New Feature! Mash Profile!
				if (vessel == "mash") {
					if (this == 'Unset') {
						return true;
					}
					val = sortObjectByKey(val);

					$.each(val, function(mashPID, mashDetails) {
						// Iterate the list of mash Lists

						if ($("#mashTable"+mashPID).length == 0) {
							table = "<table id='mashTable"+mashPID+"' class='table'>";
							table += "<tbody class='tbody'><tr>"
							table += "<th colspan='2'>Mash Step</th>";
							table += "<th>Temp</th>";
							table += "<th>Time</th>";
							table += "</tr><tbody></table>";
							table += "<button class='btn btn-success' id='mashButton-"+mashPID+"' type='button' onclick='mashToggle(this)'>Activate</button>";
							table += "<br/>";
	
							$("#"+mashPID).append(table);
	
						}
	
						$.each(mashDetails, function(mashStep, mashData) {
							if (mashStep != 'pid') {
								addMashStep(mashStep, mashData, mashPID);
							}
							return true;
						});

						if ($("#mashTable"+mashPID).find('.success').length > 0) {
							$("#mashButton-" + mashPID).text("Disable");
						} else {
							$("#mashButton-" + mashPID).text("Activate");
						}

						return true;
					});
				}
				
				if(vessel == "pumps") {
					$.each(val, function (pumpName, pumpStatus) {
						//enable or disable the pump as required
						if (pumpStatus) {
							jQuery('button[id^="' + pumpName + '"]')[0].style.background="red";
							jQuery('button[id^="' + pumpName + '"]')[0].innerHTML= pumpName +" ON";
						} else {
							jQuery('button[id^="' + pumpName + '"]')[0].style.background="#666666";
							jQuery('button[id^="' + pumpName + '"]')[0].innerHTML= pumpName +" OFF";
						}
					});
					return true;
				}

				if(window.disableUpdates) {
					return false;
				}
				
				if (vessel == "vessels") {
					$.each(val, function(vesselName, vesselStatus) {
						// This should always be there
						if ("name" in vesselStatus) {
							vesselName = vesselStatus.name;
						}
						
						if ("tempprobe" in vesselStatus) {
							updateTempProbe(vesselName, vesselStatus.tempprobe);
						}
						if ("pidstatus" in vesselStatus) {
							updatePIDStatus(vesselName, vesselStatus.pidstatus);
						} else {
							hidePIDForm(vesselName);
						}
						
						if ("volume" in vesselStatus) {
							updateVolumeStatus(vesselName, vesselStatus.volume);
						} else {
							jQuery("#" + vesselName + "-volume").text("No Volume");
						}
					})
					
				}
			})
			
			vessel = null;
			data = null;
				
		}
	})
	setTimeout(waitForMsg, 1000); 
	
}

function updateTempProbe(vessel, val) {

	temp = parseFloat(val.temp).toFixed(2);
	
	// check to see if we have a valid vessel
	if (isNaN(temp)) {
		return true;
	}

	scale = val.scale;

	// set the current temp scale
	Temp = parseFloat(temp).toFixed(2);
	int = Math.floor(Temp);
	dec = Temp % 1;
	dec = dec.toString().substr(1,3);
	GaugeDisplay[vessel].setValue(pad(int, 3, 0) + "" + dec);
	jQuery("#"+vessel+"-tempStatus").text(temp);
	// cleanup
	dec = null;
	temp = null;
	Temp = null;
	
	// Check for an error message
	if ("errorMessage" in val) {
		jQuery("#" + vessel + "-error").text(val.errorMessage);
		jQuery("#" + vessel + "-error").show();
	} else {
		jQuery("#" + vessel + "-error").hide();
	}
	
}

function updateVolumeStatus(vessel, status) {
	jQuery("#" + vessel + "-volume").text(parseFloat(status.volume).toFixed(2) + " " + status.units);
	jQuery("#" + vessel + ' input[name="vol_units"]').val(status.units);
	
	var vesselDiv = '[id="'+vessel+'-form"]';
	
	if ("ain" in status) {
		jQuery(vesselDiv  + ' input[name="vol_ain"]').val(status.ain);
		jQuery(vesselDiv  + ' input[name="vol_add"]').val("");
		jQuery(vesselDiv  + ' input[name="vol_off"]').val("");
	} else if ("vol_add" in status) {
		jQuery(vesselDiv  + ' input[name="vol_add"]').val(status.address);
		jQuery(vesselDiv  + ' input[name="vol_off"]').val(status.offset);
		jQuery(vesselDiv  + ' input[name="vol_ain"]').val("");
	} else {
		jQuery(vesselDiv  + ' input[name="vol_ain"]').val("");
		jQuery(vesselDiv  + ' input[name="vol_add"]').val("");
		jQuery(vesselDiv  + ' input[name="vol_off"]').val("");
	}
	
}

function editVolume(element) {

	window.disableUpdates = 1;
	
	// Is the edit form already displayed
	var vessel = element.id.substring(0, element.id.indexOf("-volume"));
	var vesselEditForm = $('#'+vessel+'-editVol');
	if (vesselEditForm.val() != undefined) {
		return;
	}
	
	var vesselDiv = vessel + "-volume";
	var volPin = $('#' + vessel  + ' input[name="vol_ain"]').val();
	var volAdd = $('#' + vessel  + ' input[name="vol_add"]').val();
	var volOff = $('#' + vessel  + ' input[name="vol_off"]').val();
	var volUnits = $('#' + vessel  + ' input[name="vol_units"]').val();
	
	// Insert a couple of new form elements
	$('#' + vesselDiv).append("<div id='"+vessel+"-editVol'>"
		+ "<form id='" + vessel + "-editVol' name='" + vessel + "-edit'>"
		+ "<input type='hidden' name='name' id='name' value='"+vessel+"'/><br/>"
		+ "<input type='text' name='adc_pin' id='adc_pin' value='"+volPin+"' placeholder='AIN Pin'/><br/>"
		+ "<input type='text' name='onewire_address' id='onewire_address' value='"+volAdd+"' placeholder='DS2450 Address' /><br/>"
		+ "<input type='text' name='onewire_offset' id='onewire_offset' value='"+volOff+"' placeholder='DS2450 Offset' /><br/>"
		+ "<input type='text' name='volume' id='volume' value='' placeholder='New Volume' /><br/>"
		+ "<input type='text' name='units' id='units' value='' value='"+volUnits+"' placeholder='Litres' /><br/>"
		+ "<button id='updateVol-"+vessel+"' class='holo-button modeclass' "
		+ "onclick='submitForm(this.form); sleep(2000); location.reload();'>Update</button>"
		+ "<button id='cancelVol-"+vessel+"' class='holo-button modeclass' "
		+ "onclick='cancelVolEdit(vessel); waitForMsg(); return false;'>Cancel</button>"
		+ "</form>"
		+ "</div>");
}

function editDevice(element) {
	// Is the edit form already displayed
	var vessel = element.id.substring(0, element.id.indexOf("-title"));
	var vesselEditForm = $('#'+vessel+'-edit');
	if (vesselEditForm.val() != undefined) {
		return;
	}
	
	var vesselDiv = element.id;
	var gpio = $('#' + vesselDiv  + ' input[name="gpio"]').val();
	var auxgpio = $('#' + vesselDiv  + ' input[name="auxgpio"]').val();
	
	// Insert a couple of new form elements
	$('#' + vesselDiv).append("<div id='"+vessel+"-edit'>"
		+ "<form id='" + vessel + "-edit' name='" + vessel + "-edit'>"
		+ "<input type='text' name='new_name' id='new_name' value='"+vessel+"' /><br/>"
		+ "<input type='text' name='new_gpio' id='new_gpio' value='"+gpio+"' placeholder='GPIO'/><br/>"
		+ "<input type='text' name='aux_gpio' id='aux_gpio' value='"+auxgpio+"' placeholder='Aux GPIO' /><br/>"
		+ "<button id='update-"+vessel+"' class='holo-button modeclass' "
		+ "onclick='submitForm(this.form); sleep(2000); location.reload();'>Update</button>"
		+ "<button id='cancel-"+vessel+"' class='holo-button modeclass' "
		+ "onclick='cancelEdit(vessel); waitForMsg(); return false;'>Cancel</button>"
		+ "</form>"
		+ "</div>");
}

function cancelEdit(vessel) {
	$('#'+vessel+'-edit').empty().remove();
}

function hidePIDForm(vessel) {
	$("#" + vessel + "-controls").hide();
}

function updatePIDStatus(vessel, val) {
	// setup the values
	var vesselDiv = 'form[id="'+vessel+'-form"]';
	$("#" + vessel + "-controls").show();
	
	var mode = val.mode.charAt(0).toUpperCase() + val.mode.slice(1);
	var currentMode = jQuery(vesselDiv  + ' input[name="dutycycle"]');
	
	if (jQuery(vesselDiv  + ' input[name="dutycycle"]') != mode ) {
	
		if(mode== "Off") {
			selectOff(vessel);
		}
		if(mode == "Auto") {
			selectAuto(vessel);
		}
		if(mode == "Manual") {
			selectManual(vessel);
		}
		
		jQuery(vesselDiv  + '  input[name="dutycycle"]').val(mode);
	}
	mode = null;
	if(val.actualduty != null) {
		Gauges[vessel].refresh(val.actualduty); 				
	} else {
		Gauges[vessel].refresh(val.duty); 
	}


	jQuery('div[id="tempUnit"]').text(val.scale);
	
	jQuery(vesselDiv  + ' input[name="dutycycle"]').val(val.duty);
	jQuery(vesselDiv  + ' input[name="cycletime"]').val(val.cycle);
	jQuery(vesselDiv  + ' input[name="setpoint"]').val(val.setpoint);
	jQuery(vesselDiv  + ' input[name="p"]').val(val.p);
	jQuery(vesselDiv  + ' input[name="i"]').val(val.i);
	jQuery(vesselDiv  + ' input[name="d"]').val(val.d);
	jQuery(vesselDiv  + ' input[name="gpio"]').val(val.gpio);
	
	// Disable some stuff
	jQuery(vesselDiv  + ' input[name="dutycycle"]').prop("disabled", true);
	jQuery(vesselDiv  + ' input[name="cycletime"]').prop("disabled", true);
	jQuery(vesselDiv  + ' input[name="setpoint"]').prop("disabled", true);
	jQuery(vesselDiv  + ' input[name="p"]').prop("disabled", true);
	jQuery(vesselDiv  + ' input[name="i"]').prop("disabled", true);
	jQuery(vesselDiv  + ' input[name="d"]').prop("disabled", true);
	
	// Aux Mode check
	if ("auxStatus" in val) {
		jQuery(vesselDiv  + ' button[id="'+vessel+'Aux"]').show();
		if (val.auxStatus == "on" || val.auxStatus == "1") {
			jQuery(vesselDiv  + ' button[id="'+vessel+'Aux"]').style.background = "red";
			jQuery(vesselDiv  + ' button[id="'+vessel+'Aux"]').innerHTML = "Aux ON"
		} else {
			jQuery(vesselDiv  + ' button[id="'+vessel+'Aux"]').style.background = "#666666";
			jQuery(vesselDiv  + ' button[id="'+vessel+'Aux"]').innerHTML = "Aux OFF"
		}
	} else {
		jQuery(vesselDiv  + ' button[id="'+vessel+'Aux"]').hide();
	}
	
	window.disableUpdates = 0;

}

function selectOff(vessel) {

	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}
	
	var vesselDiv = 'form[id="'+vessel+'-form"]';
	$(vesselDiv + ' input[name="mode"]').val("off"); 
	
	jQuery('button[id^="'+vessel+'-modeOff"]')[0].style.background="red";
	jQuery('button[id^="'+vessel+'-modeManual"]')[0].style.background="#666666";
	jQuery('button[id^="'+vessel+'-modeAuto"]')[0].style.background="#666666";

	jQuery('tr[id="'+vessel+'-SP"]').hide();
	jQuery('tr[id="'+vessel+'-DT"]').hide();
	jQuery('tr[id="'+vessel+'-DC"]').hide();
	jQuery('tr[id="'+vessel+'-p"]').hide();
	jQuery('tr[id="'+vessel+'-i"]').hide();
	jQuery('tr[id="'+vessel+'-d"]').hide();

	vessel = null;
	return false;
}

function selectAuto(vessel) {
	
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

	var vesselDiv = 'form[id="'+vessel+'-form"]';
	$(vesselDiv + ' input[name="mode"]').val("auto");
	
	jQuery('button[id^="'+vessel+'-modeOff"]')[0].style.background="#666666";
	jQuery('button[id^="'+vessel+'-modeManual"]')[0].style.background="#666666";
	jQuery('button[id^="'+vessel+'-modeAuto"]')[0].style.background="red";

	jQuery('tr[id="'+vessel+'-SP"]').show();
	jQuery('tr[id="'+vessel+'-DT"]').show();
	jQuery('tr[id="'+vessel+'-DC"]').hide();
	jQuery('tr[id="'+vessel+'-p"]').show();
	jQuery('tr[id="'+vessel+'-i"]').show();
	jQuery('tr[id="'+vessel+'-d"]').show();

	vessel = null;
	return false;
}

function selectManual(vessel) {
	
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}
	
	var vesselDiv = 'form[id="'+vessel+'-form"]';
	$(vesselDiv + ' input[name="mode"]').val("manual");

	jQuery('button[id^="'+vessel+'-modeOff"]')[0].style.background="#666666";
	jQuery('button[id^="'+vessel+'-modeManual"]')[0].style.background="red";
	jQuery('button[id^="'+vessel+'-modeAuto"]')[0].style.background="#666666";

	jQuery('tr[id="'+vessel+'-SP"]').hide();
	jQuery('tr[id="'+vessel+'-DT"]').show();
	jQuery('tr[id="'+vessel+'-DC"]').show();
	jQuery('tr[id="'+vessel+'-p"]').hide();
	jQuery('tr[id="'+vessel+'-i"]').hide();
	jQuery('tr[id="'+vessel+'-d"]').hide();

	vessel = null;
	return false;
}

function submitForm(form){

	// Are we updating the data?
	if (form.id.indexOf("-form") != -1) {
		var vessel = form.id.substring(0, form.id.indexOf("-form"));
		
		var formdata = {};
		
		formdata[vessel] = JSON.stringify(jQuery(form).serializeObject());
		$.extend(formdata[vessel], {"name":"mode", "value":Window.mode});
		//formdata = ;
		
		$.ajax({ 
			url: 'updatepid',
			type: 'POST',
			data: formdata,
			dataType: 'json',
			success: function(data) {data = null}
		});
	} else if (form.id.indexOf("-editVol") != -1) {
		var vessel = form.id.substring(0, form.id.indexOf("-editVol"));
		var formdata = {}
		formdata[vessel] = JSON.stringify(jQuery(form).serializeObject());
		$.ajax({ 
			url: 'addvolpoint',
			type: 'POST',
			data: formdata,
			dataType: 'json',
			success: function(data) {data = null}
		});
	} else if (form.id.indexOf("-edit") != -1) {
		// We're editing
		var vessel = form.id.substring(0, form.id.indexOf("-edit"));
		var formdata = {}
		formdata[vessel] = JSON.stringify(jQuery(form).serializeObject());
		$.ajax({ 
			url: 'editdevice',
			type: 'POST',
			data: formdata,
			dataType: 'json',
			success: function(data) {data = null}
		});
	} else {
		// Another form...
		console.log("Unrecognised form: " + form.id);
		return;
	}
	
	window.disableUpdates = 0;
	return false;
}

function submitPump(pumpStatus) {
	$.ajax({
		url: 'updatepump',
		type: 'POST',
		data: "toggle=" + pumpStatus.id,
		success: function(data) {data = null}
	});	
	window.disableUpdates = 0;
	return false;
}

function addPump() {
	var newname = prompt("New pump name");
	var gpio = prompt("GPIO Pin");
	
	$.ajax({
		url: 'addpump',
		type: 'POST',
		data: "new_name=" + newname + "&new_gpio=" + gpio,
		success: function(data) {data = null}
	});	
	window.disableUpdates = 0;
	return false;
}

function addTimer() {
	var newname = prompt("New Timer Name");
	
	$.ajax({
		url: 'addtimer',
		type: 'POST',
		data: "new_name=" + newname,
		success: function(data) {data = null}
	});	
	window.disableUpdates = 0;
	return false;
}

function mashToggle(button, position) {
	// Parse out the PID from the controller
	var pid = button.id.replace("mashButton-", "");
	postData = {};
	postData['pid'] = pid;
	postData['status'] = button.innerText.toLowerCase();

	if (position !== 'undefined') {
		postData['position'] = position;
	}

	$.ajax({
			url: 'toggleMash',
			type: 'POST',
			data: postData,
			success: function(data) {data = null}
	});
	return false;
	
}

function pad(n, width, z) {
	z = z || '0';
	n = n + '';
	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function disable(input) {
	window.disableUpdates = 1;
	
	// setup the values
	var vessel = input.id.substring(0, input.id.indexOf("-mode"));
	var vesselDiv = 'form[id="'+vessel+'-form"]';
	
	jQuery(vesselDiv  + ' input[name="dutycycle"]').prop("disabled", false);
	jQuery(vesselDiv  + ' input[name="cycletime"]').prop("disabled", false);
	jQuery(vesselDiv  + ' input[name="setpoint"]').prop("disabled", false);
	jQuery(vesselDiv  + ' input[name="p"]').prop("disabled", false);
	jQuery(vesselDiv  + ' input[name="i"]').prop("disabled", false);
	jQuery(vesselDiv  + ' input[name="d"]').prop("disabled", false);
	return false;
}

function toggleDiv(id) {
	var e = document.getElementById(id);
       if(e.style.display == 'table-cell')
          e.style.display = 'none';
       else
          e.style.display = 'table-cell';

	e = null;
}

function setTimer(button, stage) {
	// get the current Datestamp
	var curDate = Date.now();
	if(button.innerHTML == ("Start " + stage)) {
		$("#" + stage).hide();
		$("#"+stage+"Timer").show();
		formdata = stage + "Start=" + curDate;
	} else {
		$("#"+stage).show();
		$("#"+stage+"Timer").hide();
		formdata = stage+"End=" + curDate;
	}
	
	formdata +="&updated=" + curDate;
	
	$.ajax({ 
		url: 'updateday',
		type: 'POST',
		data: formdata,
		success: function(data) {data = null}
	});	
	window.disableUpdates = 0;
	return false;
}

function resetTimer(button, stage) {
	// get the current Datestamp
	var curDate = Date.now();
	formdata = stage+"End=null&" + stage +"Start=null" ;

	formdata +="&updated=" + curDate;
	
	$.ajax({ 
		url: 'updateday',
		type: 'POST',
		data: formdata,
		success: function(data) {data = null}
	});
	
	$("#"+stage)[0].innerHTML = "Start " + stage;
	
	$("#"+stage).show();
	$("#"+stage+"Timer").hide();
	
	window.disableUpdates = 0;
	return false;
}

function checkTimer(val, stage) {

	if ("name" in val) {
		stage = val.name;
	}
	
	if ("start" in val) {
		var startTime = new Date(val["start"]);
		
		// boil has been started, has it been finished
		if ("end" in val) {
			var endTime = new Date(val["end"]);
			var diffTime = endTime - startTime;
			var hours = Math.floor(diffTime/(1000*60*60));
			diffTime -= hours * 1000*60*60;
			var mins = Math.floor(diffTime/(1000*60));
			diffTime -= mins * 1000*60;
			$("#"+stage).show();
			$("#"+stage+"Timer").hide();
			$("#"+stage)[0].innerHTML = stage + ": " + hours + ":" + mins + ":" + diffTime/1000;
		} else {
			$("#"+stage).hide();
			$("#"+stage+"Timer").show();
			$("#"+stage+"Timer").tinyTimer({from: startTime.toString()});
		}
	} else {
		$("#"+stage+"Timer").hide();
		$("#"+stage)[0].innerHTML = "Start " + stage;
	}
}

function toggleAux(PIDName) {
	 $.ajax({
		 url: 'toggleAux',
			type: 'POST',
			data: "toggle=" + PIDName,
		success: function(data) {data = null}
	});	
	window.disableUpdates = 0;
	return false;
}

function addMashStep(mashStep, mashData, pid) {
	// Mashstep is the int position
	// mashData contains the actual data to be displayed
	if (mashStep == "mashstep" || "index" in mashData) {
		mashStep = mashData['index'];
	}
	
	var mashStepRow = $("#mashRow"+pid+"-"+mashStep);
	if (mashStepRow.length == 0) {
		// Add a new row to the Mash Table
		tableRow = "<tr id='mashRow"+pid+"-"+mashStep+"'>";
		tableRow += ("<td>"+mashData['type']+"</td>");
		tableRow += ("<td>"+mashData['method']+"</td>");
		tableRow += ("<td>"+mashData['target_temp']+mashData['target_temp_unit']+"</td>");
		tableRow += ("<td id='mashTimer"+pid+"'>"+mashData['duration']+"</td>");
		tableRow += ("</tr>");

		mashStepRow = $("#mashTable"+pid +" > tbody > tr").eq(mashStep).after(tableRow);
		mashStepRow = mashStepRow.next();
	}

	// Do we have a start time?
	if ("start_time" in mashData) {
		// if there's an end time, we can show the actual time difference
		if ("end_time" in mashData) {
			startDate = new Date(mashData['start_time']);
			endDate = new Date(mashData['end_time']);
			diff = Math.abs(endDate - startDate);
			seconds = diff/1000;
			minutes = Math.floor(seconds/60);
			seconds = seconds - (minutes * 60);

			mashStepRow.find("#mashTimer"+pid).text(minutes + ":" + pad(seconds, 2, 0));
		} else {
			// start the timer
			mashStepRow.find("#mashTimer"+pid).tinyTimer({to: mashData['target_time'].toString()});
		}
	}
	// active the current row if needs be
	if ("active" in mashData) {
		mashStepRow.addClass('success');
	} else {
		mashStepRow.removeClass('success');
	}
}
