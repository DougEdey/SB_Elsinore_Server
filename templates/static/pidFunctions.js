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
				
				// Check for the volume
				if ("volume" in val) {
					jQuery("#" + vessel + "-volume").text(parseFloat(val.volume).toFixed(2) + " " + val.volumeUnits);
				}
				
				if(!("gpio" in val )) {
					return; // nothing else to do
				}

				// setup the values
				var mode = val.mode.charAt(0).toUpperCase() + val.mode.slice(1);
				if (Window.mode != mode ) {
				
					if(mode== "Off") {
						selectOff(vessel);
					}
					if(mode == "Auto") {
						selectAuto(vessel);
					}
					if(mode == "Manual") {
						selectManual(vessel);
					}
				}
				mode = null;
				if(val.actualduty != null) {
					Gauges[vessel].refresh(val.actualduty); 				
				} else {
					Gauges[vessel].refresh(val.duty); 
				}

			
				jQuery('div[id="tempUnit"]').text(val.scale);
				jQuery('input[name="'+vessel+'-dutycycle"]').val(val.duty);
				jQuery('input[name="'+vessel+'-cycletime"]').val(val.cycle);
				jQuery('input[name="'+vessel+'-setpoint"]').val(val.setpoint);
				jQuery('input[name="'+vessel+'-p"]').val(val.p);
				jQuery('input[name="'+vessel+'-i"]').val(val.i);
				jQuery('input[name="'+vessel+'-d"]').val(val.k);
				
				// Aux Mode check
				if ("auxStatus" in val) {
					if (val.auxStatus == "on" || val.auxStatus == "1") {
						jQuery("button[id='" + vessel + "Aux'")[0].style.background = "red";
						jQuery("button[id='" + vessel + "Aux'")[0].innerHTML = "Aux ON"
					} else {
						jQuery("button[id='" + vessel + "Aux'")[0].style.background = "#666666";
						jQuery("button[id='" + vessel + "Aux'")[0].innerHTML = "Aux OFF"
					}
				}
				
				window.disableUpdates = 0;
			})
			vessel = null;
			data = null;
		}
	})
	setTimeout(waitForMsg, 1000); 
	
}

function selectOff(vessel) {
	Window.mode = "off";
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

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
	Window.mode = "auto";
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

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
	Window.mode = "manual";
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

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
	formdata = jQuery(form).serialize();
	vessel = form.id.substring(0, form.id.indexOf("-form"));
	var find = vessel + '-';
	formdata = formdata.replace(new RegExp(find, 'g'), '');
	formdata = formdata.replace(new RegExp('&d=', 'g'), '&k=');
	formdata = "form=" + vessel + "&" + "mode=" + Window.mode + "&" +formdata;
	$.ajax({ 
		url: 'updatepid',
		type: 'POST',
		data: formdata,
		success: function(data) {data = null}
	});	
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

function pad(n, width, z) {
	z = z || '0';
	n = n + '';
	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function disable(input) {
	window.disableUpdates = 1;
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
	if(button.innerHTML.toLowerCase() == ("Start " + stage).toLowerCase()) {
		$("#" + stage).hide();
		$("#"+stage+"Timer").show();
		formdata = stage.toLowerCase() + "Start=" + curDate;
	} else {
		$("#"+stage).show();
		$("#"+stage+"Timer").hide();
		formdata = stage.toLowerCase()+"End=" + curDate;
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
	formdata = stage.toLowerCase()+"End=null&" + stage.toLowerCase() +"Start=null" ;

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
	// Check for a boil timing
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
