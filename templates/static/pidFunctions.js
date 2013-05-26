function waitForMsg(){
	if (window.disableUpdates) {
		return false;
	}
	jQuery.ajax({
		type: 'GET',
		url: hostName+'/getstatus',
		dataType: 'json',
		async: true,
		cache: false,
		timeout:5000,
		success: function(data){
			if(data == null) 
				return;
			$.each(data, function(key, val) {
				
				vessel = key;

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
				if(!("gpio" in val )) {
					// hide the sections we don't want
					jQuery("#"+vessel+"-form").hide();	
					jQuery("#"+vessel+"-gage").hide();
					return; // nothing else to do
				}

				// setup the values
				var mode = val.mode.charAt(0).toUpperCase() + val.mode.slice(1);
				var s = jQuery('#' + vessel + '-mode'+mode);
				if(mode== "Off") {
					selectOff(vessel);
				}
				if(mode == "Auto") {
					selectAuto(vessel);
				}
				if(mode == "Manual") {
					selectManual(vessel);
				}
				s.attr('checked', 'checked');

				Gauges[vessel].refresh(val.duty);
//				justGage({vessel+'-gage', val.duty});
				jQuery('input[name="'+vessel+'-dutycycle"]').val(val.duty);
				jQuery('input[name="'+vessel+'-cycletime"]').val(val.cycle);
				jQuery('input[name="'+vessel+'-setpoint"]').val(val.setpoint);
				jQuery('input[name="'+vessel+'-p"]').val(val.p);
				jQuery('input[name="'+vessel+'-i"]').val(val.i);
				jQuery('input[name="'+vessel+'-d"]').val(val.k);
				jQuery("#pidInput").click(function() {disable(this)});
				//jQuery(".modeClass").click(function() {disable(this);});
				jQuery("#"+vessel+"-modeAuto").click(function() {disable(this); selectAuto(this);});
				jQuery("#"+vessel+"-modeOff").click(function() {disable(this); selectOff(this);});
				jQuery("#"+vessel+"-modeManual").click(function() {disable(this); selectManual(this);});
				Window.disableUpdates = 0;
			})
		}
	})
	
	setTimeout(waitForMsg, 5000);
}

function selectOff(vessel) {
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
	}
	jQuery('div[id^="'+vessel+'-label"]').each(function (index) {
		$(this).hide();
		}
	);
	jQuery('input[name^="'+vessel+'-"]').each(function (index) {
		$(this).parent().parent().parent().hide();
		}
	);
	jQuery('div[id^="'+vessel+'-unit"]').each(function (index) {
		$(this).hide();
		}
	);

}

function selectAuto(vessel) {
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
	}
	jQuery('div[id^="'+vessel+'-label"]').each(function (index) {
		$(this).show();
		}
	);
	jQuery('input[name^="'+vessel+'-"]').each(function (index) {
		$(this).parent().parent().parent().show();
		}
	);
	jQuery('div[id^="'+vessel+'-unit"]').each(function (index) {
		$(this).show();
		}
	);
	jQuery('div[id^="'+vessel+'-labelDC"]').each(function (index) {
		$(this).hide();
		}
	);
	jQuery('div[id^="'+vessel+'-unitDC"]').each(function (index) {
		$(this).hide();
		}
	);
	jQuery('input[name^="'+vessel+'-dutycycle"]').each(function (index) {
		$(this).parent().parent().parent().hide();
		}
	);
}

function selectManual(vessel) {
	if((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.indexOf("-");
		vessel = v.substr(0, i);
	}
	jQuery('div[id^="'+vessel+'-label"]').each(function (index) {
		$(this).hide();
		}
	);
	jQuery('input[name^="'+vessel+'-"]').each(function (index) {
		$(this).parent().parent().parent().hide();
		}
	);
	jQuery('div[id^="'+vessel+'-unit"]').each(function (index) {
		$(this).hide();
		}
	);
	jQuery('div[id^="'+vessel+'-labelDT"]').each(function (index) {
		$(this).show();
		}
	);
	jQuery('div[id^="'+vessel+'-labelDC"]').each(function (index) {
		$(this).show();
		}
	);
	jQuery('div[id^="'+vessel+'-unitDT"]').each(function (index) {
		$(this).show();
		}
	);
	jQuery('div[id^="'+vessel+'-unitDC"]').each(function (index) {
		$(this).show();
		}
	);
	jQuery('input[name^="'+vessel+'-dutycycle"]').each(function (index) {
		$(this).parent().parent().parent().show();
		}
	);
	jQuery('input[name^="'+vessel+'-cycletime"]').each(function (index) {
		$(this).parent().parent().parent().show();
		}
	);
}

function submitForm(form){
	formdata = jQuery(form).serialize();
	vessel = form.id.substring(0, form.id.indexOf("-form"));
	var find = vessel + '-';
	formdata = formdata.replace(new RegExp(find, 'g'), '');
	formdata = formdata.replace(new RegExp('&d=', 'g'), '&k=');
	formdata = "form=" + vessel + "&" + formdata;
	$.ajax({ 
		url: 'updatepid',
		type: 'POST',
		data: formdata,
 		async:false,
		success: function() {return false;}
	});	
}

function pad(n, width, z) {
	z = z || '0';
	n = n + '';
	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function disable(input) {
	window.disableUpdates = 1;
}
