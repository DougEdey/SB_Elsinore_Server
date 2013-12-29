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
				
				if(vessel == "pumps") {
					$.each(val, function (pumpName, pumpStatus) {
						//enable or disable the pump as required
						if (pumpStatus) {
							jQuery('button[id^="' + pumpName + '"]')[0].style.background="red";
						} else {
							jQuery('button[id^="' + pumpName + '"]')[0].style.background="#666666";
						}
					})
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
