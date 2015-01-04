function getLanguage() {
	$.ajax({
		type : 'GET',
		url : '/getstatus',
		dataType : 'json',
		async : false,
		cache : false,
		timeout : 5000,
		success : function(data) {
			if ("language" in data) {
				window.customLang = data.language;
			}

			return;
		}
	});
};

function setup() {
	var langObj = getLanguage();
	$.i18n.properties({
		name : 'messages',
		path : '/nls/',
		mode : 'both',
		language : window.customLang

	});
	// $("#logo").css('opacity','0');
	window.heightFixed = 0;
	$.ajax({
		url : '/brewerImage.gif',
		type : 'HEAD',
		error : function() {
			$('#brewerylogo').css('display', 'none');
		},
		success : function(data, status, XMLHttpRequest) {
			if (XMLHttpRequest.status == 200) {
				$('#brewerylogo').attr('src', '/brewerImage.gif');
				$('#brewerylogo').css('display', 'block');
			} else {
				$('#brewerylogo').css('display', 'none');
			}
		}
	});

	$('#logo').fileupload({
		dataType : 'json',
		done : function(e, data) {
			$.each(data.result.files, function(index, file) {
				$('<p/>').text(file.name).appendTo(document.body);
			});
		}
	});

	var temp =$('div[class="center-left"]');
	
	if (temp == undefined || temp.length == 0) {
		temp = $('div[class="center-left col-md-4"]')
	}
	temp.append(
			"<button id='edit-page' class='col-md-4 btn' onclick='toggleEdit(true); return false;'>"
							+ $.i18n.prop("EDIT") + "</button>");

	temp.append(
			"<button id='change-scale' class='col-md-6 btn' onclick='changeScale(); return false;'>"
					+ $.i18n.prop("CHANGE_SCALE") + "</button>")

	$('div[id$=-graph_body]').each(function(index) {
		$(this).slideToggle();
	});

	$('#cool a').click(function(e) {
		e.preventDefault()
		$(this).tab('show')
	});

	$('#heat a').click(function(e) {
		e.preventDefault()
		$(this).tab('show')
	});


	$('html').on('click', function(e) {
		  if (typeof $(e.target).data('original-title') == 'undefined' && !$(e.target).parents().is('.popover.in')) {
		    $('[data-original-title]').popover('hide');
		  }
	});
   	
	waitForMsg();
};

$.fn.serializeObject = function() {
	var o = {};
	var a = this.serializeArray();
	$.each(a, function() {
		if (o[this.name] !== undefined) {
			if (!o[this.name].push) {
				o[this.name] = [ o[this.name] ];
			}
			o[this.name].push(this.value || '');
		} else {
			o[this.name] = this.value || '';
		}
	});
	return o;
};

function showGraph(element) {
	var vessel = element.id.substring(0, element.id.lastIndexOf("-graph_body"));
	window.open("/graph?vessel=" + vessel);
};

function sleep(milliseconds) {
	var start = new Date().getTime();
	for (var i = 0; i < 1e7; i++) {
		if ((new Date().getTime() - start) > milliseconds) {
			break;
		}
	}
}

/**
 * Return an Object sorted by it's Key
 */
var sortObjectByKey = function(obj) {
	var keys = [];
	var sorted_obj = {};

	for ( var key in obj) {
		if (obj.hasOwnProperty(key)) {
			keys.push(key);
		}
	}

	// sort keys
	keys.sort();

	// create new array based on Sorted Keys
	jQuery.each(keys, function(i, key) {
		sorted_obj[key] = obj[key];
	});

	return sorted_obj;
};

function waitForMsg() {
	if (window.disableUpdates) {
		return false;
	}
	jQuery
			.ajax({
				type : 'GET',
				url : '/getstatus',
				dataType : 'json',
				async : true,
				cache : false,
				timeout : 5000,
				success : function(data) {
					if (data == null)
						return;

					if ("breweryName" in data) {
						val = data.breweryName;
						if (val != null && val.length > 0 && val != "") {
							window.breweryName = val;
							jQuery("#breweryName").text(val);
						} else {
							window.breweryName = "Elsinore";
							jQuery("#breweryName").text("Elsinore");
						}
					}

					// Check for an error message
					if ("message" in data) {
						val = data.message;

						if (val.length > 0) {
							val += "<br/><button id='clearMessage' class='btn modeclass' "
									+ "onclick='clearStatus(); return false;'>"
									+ $.i18n.prop("CLEAR") + "</button>";
							jQuery("#messages-body").html(val);

							if (!$("#messages").is(":visible")) {
								jQuery("#messages").css('display', 'block');
								jQuery("#messages").toggleClass("hidden", false);
							}
						} else {
							if ($("#messages").is(":visible")) {
								jQuery("#messages").toggleClass("hidden", true);;
							}
						}
					}

					if ("brewday" in data) {
						val = data.brewday;
						$.each(val, function(timerName, timerStatus) {
							checkTimer(timerStatus, timerName);
						});

					}

					if ("mash" in data) {
						val = data.mash;
						if (val != 'Unset') {

							val = sortObjectByKey(val);

							$.each(val,
									function(mashPID, mashDetails) {
										// Iterate the list of mash Lists
										addMashTable(mashPID);
										$.each(mashDetails, function(mashStep,
												mashData) {
											if (mashStep != 'pid') {
												addMashStep(mashStep, mashData,
														mashPID);
											}
										});

										if ($("#mashTable" + mashPID).find(
												'.success').length > 0) {
											$("#mashButton-" + mashPID).text(
													$.i18n.prop("DISABLE"));
										} else {
											$("#mashButton-" + mashPID).text(
													$.i18n.prop("ACTIVATE"));
										}

									});
						}
					}

					if ("pumps" in data) {
						val = data.pumps;
						$
								.each(
										val,
										function(pumpName, pumpStatus) {
											// enable or disable the pump as
											// required
											if (pumpStatus) {
												jQuery('button[id^="'
														+ pumpName + '"]')[0].style.background = "red";
												jQuery('button[id^="'
														+ pumpName + '"]')[0].innerHTML = pumpName
														.replace("_", " ")
														+ " "
														+ $.i18n
																.prop("PUMP_ON");
											} else {
												jQuery('button[id^="'
														+ pumpName + '"]')[0].style.background = "#666666";
												jQuery('button[id^="'
														+ pumpName + '"]')[0].innerHTML = pumpName
														.replace("_", " ")
														+ " "
														+ $.i18n
																.prop("PUMP_OFF");
											}
										});
					}

					if (window.disableUpdates) {
						return false;
					}

					if ("vessels" in data) {
						val = data.vessels;

						if (!("system" in data.vessels)
								&& !("System" in data.vessels)) {
							// No System temperature, add a header to add it in.
							var sysTemp = $("[id=tempProbes] > [id=System]");
							if (sysTemp.length == 0 && !data.locked) {
								var sysHtml = '<div id="System" class="holo-content controller panel panel-primary Temp">'
										+ '<div id="System-title" class="title panel-heading "'
										+ 'onclick="enableSystem(this);" style="cursor: pointer;">'
										+ $.i18n.prop("SYSTEM")
										+ '</div>'
										+ '</div>';

								$("[id=tempProbes]").append(sysHtml)
							}
							if ($("[id=tempProbes] > [id=System] > div").length == 1
									&& data.locked) {
								sysTemp.remove();
							}
						}
						$
								.each(
										val,
										function(vesselName, vesselStatus) {

											// This should always be there
											if ("name" in vesselStatus) {
												vesselName = vesselStatus.name;
												if (vesselName == $.i18n
														.prop("SYSTEM")
														&& $('[id=System-tempGauge]').length == 0) {
													return;
												}
											}

											if ("tempprobe" in vesselStatus) {
												updateTempProbe(vesselName,
														vesselStatus.tempprobe);
											}

											if ("pidstatus" in vesselStatus) {

												addMashTable(vesselName);

												updatePIDStatus(vesselName,
														vesselStatus.pidstatus);

												// Hide the gauge if needs be
												if (vesselStatus.pidstatus.mode == "off") {
													$(
															'div[id^="'
																	+ vesselName
																	+ '-gage"]')
															.toggleClass("hidden", true);
												} else {
													$(
															'div[id^="'
																	+ vesselName
																	+ '-gage"]')
															.toggleClass("hidden", false);
													var duty = vesselStatus.pidstatus.duty;
													if ("actualduty" in vesselStatus.pidstatus) {
														duty = vesselStatus.pidstatus.actualduty;
													}

													if (duty < 0) {
														if (Gauges[vesselName].config.textMax != "0") {
															Gauges[vesselName].config.levelColors = [
																	"#0033CC",
																	"#CC00CC",
																	"#a9d70b" ];
														}
														Gauges[vesselName]
																.refreshBoth(
																		duty,
																		-100,
																		"0");
													} else {
														if (Gauges[vesselName].config.textMax != "0") {
															Gauges[vesselName].config.levelColors = [
																	"#a9d70b",
																	"#f9c802",
																	"#ff0000" ];
														}
														Gauges[vesselName]
																.refreshBoth(
																		duty,
																		"0",
																		100);
													}

												}
											} else {
												hidePIDForm(vesselName);
											}

											if ("volume" in vesselStatus) {
												updateVolumeStatus(vesselName,
														vesselStatus.volume);
											} else {
												jQuery("#" + vesselName+ "-volumeAmount")
													.text($.i18n.prop("NO_VOLUME"));
											}
										});
					}

					if ("locked" in data) {
						if (window.locked == undefined) {
							window.locked = !data.locked;
							toggleEdit(false);
							window.locked = data.locked;
						}
					}

					vessel = null;
					data = null;
					fixWebkitHeightBug();
				}
			});
	setTimeout(waitForMsg, 1000);

}

function addMashTable(vesselName) {
	if ($("#mashTable" + vesselName).length == 0) {
		table = "<table id='mashTable" + vesselName
				+ "' class='table table-curved'>";
		table += "<thead><tr>";
		table += "<th colspan='2'>" + $.i18n.prop("TRIGGER") + "</th>";
		table += "<th></th>";
		table += "<th></th>";
		table += "</tr></thead>";
		table += "<tbody class='tbody'></tbody>"

		table += "<tfoot><tr><td colspan='2'>"
				+ "<button class='btn btn-success' id='addTrigger-" + vesselName
				+ "' type='button' onclick='addNewTrigger(this)' "
				+ "ondrop='dropDeleteTrigger(event);' "
				+ "ondragover='allowDropTrigger(event);'>"
				+ $.i18n.prop("ADD") + "</button></td>";
		table += "<td colspan='2'><button class='btn btn-success' id='triggerButton-"
				+ vesselName
				+ "' type='button' onclick='triggerToggle(this)'>"
				+ $.i18n.prop("ACTIVATE") + "</button></td></tr></tfoot>";
		+"</tbody></table>";
		table += "<br id='mashTable" + vesselName + "footer'/>";

		$("#" + vesselName + "-gage").after(table);

	}
}

function updateTempProbe(vessel, val) {

	temp = parseFloat(val.temp).toFixed(2);

	// check to see if we have a valid vessel
	if (isNaN(temp)) {
		return true;
	}

	scale = val.scale;
	if (scale == "F") {
		GaugeDisplay[vessel].pattern = "###.##";
		Temp = Number(temp).toFixed(2);
		int = Math.floor(Temp);
		dec = Number(Temp % 1).toFixed(2);
		dec = dec.toString().substr(2, 4);
	} else {
		GaugeDisplay[vessel].pattern = "###.#";
		Temp = Number(temp).toFixed(1);
		int = Math.floor(Temp);

		dec = (Temp % 1).toFixed(1);
		dec = dec.toString().substr(2, 3);
	}

	int = pad(int, 3, ' ');

	GaugeDisplay[vessel].setValue(int + "." + dec);
	var vesselDiv = '[id="' + vessel + '-form"]';
	if ("cutoff" in val) {
		jQuery(vesselDiv + ' input[name="cutoff"]').val(val.cutoff);
	}

	if ("calibration" in val) {
		jQuery(vesselDiv + ' input[name="calibration"]').val(val.calibration);
	}

	jQuery(vesselDiv + ' input[name="hidden"]').val(val.hidden);
	jQuery("#" + vessel + "-tempStatus").text(temp);

	$("[id=" + vessel + "]").find("[id=tempUnit]").each(function() {
		this.innerHTML = scale;
	});

	// cleanup
	dec = null;
	temp = null;
	Temp = null;

	// Check for an error message
	if ("errorMessage" in val) {
		jQuery("#" + vessel + "-error").text(val.errorMessage);
		jQuery("#" + vessel + "-error").toggleClass("hidden", false);
	} else {
		jQuery("#" + vessel + "-error").toggleClass("hidden", true);
	}

}

function updateVolumeStatus(vessel, status) {
	$("#" + vessel + "-volumeAmount").text(
			parseFloat(status.volume).toFixed(2) + " " + status.units);
	$("#" + vessel + ' input[name="vol_units"]').val(status.units);

	if ($("#" + vessel + "-volume-gravity").length == 0) {
		$("#" + vessel + "-volume").append(
			"<div id='" + vessel + "-volume-gravity'>"
				+ "<form id='" + vessel + "-gravity-edit' " +
						" name='" + vessel + "-gravity-edit'>"
				+ "<input type='number' name='gravity' class='form-control' id='gravity'" +
						" value='" + status.gravity + "' step='any'/>"
				+ "<button id='updategravity-" + vessel + "'" +
						" class='btn' "
				+ "onclick='submitForm(this.form); sleep(2000); location.reload();'>"
				+ $.i18n.prop("UPDATE_GRAVITY")
				+ "</button>"
			+ "</div>");
	}
	
	var vesselDiv = '[id="' + vessel + '-form"]';

	if ("ain" in status) {
		$(vesselDiv + ' input[name="vol_ain"]').val(status.ain);
		$(vesselDiv + ' input[name="vol_add"]').val("");
		$(vesselDiv + ' input[name="vol_off"]').val("");
	} else if ("vol_add" in status) {
		$(vesselDiv + ' input[name="vol_add"]').val(status.address);
		$(vesselDiv + ' input[name="vol_off"]').val(status.offset);
		$(vesselDiv + ' input[name="vol_ain"]').val("");
	} else {
		$(vesselDiv + ' input[name="vol_ain"]').val("");
		$(vesselDiv + ' input[name="vol_add"]').val("");
		$(vesselDiv + ' input[name="vol_off"]').val("");
	}
}

function editVolume(element) {

	window.disableUpdates = 1;

	// Is the edit form already displayed
	var vessel = element.id.substring(0, element.id.lastIndexOf("-volume"));
	var vesselEditForm = $('#' + vessel + '-editVol');
	if (vesselEditForm.val() != undefined) {
		return;
	}

	// Insert a couple of new form elements
	 var $tr = $(element);
     
     $tr.popover('destroy');
	 //$tr.popover();
     $.ajax({
         url: '/getVolumeForm',
         data: {vessel: vessel},
         dataType: 'html',
         success: function(html) {
             $tr.popover({
                 title: 'Volume Edit',
                 content: html,
                 placement: 'top',
                 html: true,
                 trigger: 'manual'
             }).popover('show');
         }
     });
}

function addPhSensor(element) {

	window.disableUpdates = 1;

	// Insert a couple of new form elements
	 var $tr = $(element);
     
     $tr.popover('destroy');
     $.ajax({
         url: '/getphsensorform',
         data: "",
         dataType: 'html',
         success: function(html) {
             $tr.popover({
                 title: 'Add New pH Sensor',
                 content: html,
                 placement: 'bottom',
                 html: true,
                 trigger: 'manual'
             }).popover('show');
         }
     });
}

function editDevice(element) {
	// Is the edit form already displayed
	var vessel = element.id.substring(0, element.id.lastIndexOf("-title"));
	var vesselEditForm = $('#' + vessel + '-edit');
	if (vesselEditForm.val() != undefined) {
		return;
	}

	var vesselDiv = element.id;
	var heatgpio = $('#' + vessel + ' input[name="heatgpio"]').val();
	var coolgpio = $('#' + vessel + ' input[name="coolgpio"]').val();
	var heatinvert = $('#' + vessel + ' input[name="heatinvert"]').val();
	var coolinvert = $('#' + vessel + ' input[name="coolinvert"]').val();
	var auxgpio = $('#' + vessel + ' input[name="auxgpio"]').val();
	var cutoff = $('#' + vessel + ' input[name="cutoff"]').val();
	var calibration = $('#' + vessel + ' input[name="calibration"]').val();
	var hidden = $('#' + vessel + ' input[name="hidden"]').val();

	if (hidden == "true") {
		var toggle = "SHOW";
	} else {
		var toggle = "HIDE";
	}
	// Insert a couple of new form elements
	$('#' + vesselDiv)
			.append(
					"<div id='"
							+ vessel
							+ "-edit'>"
							+ "<form id='"
							+ vessel
							+ "-edit' name='"
							+ vessel
							+ "-edit'>"
							+ "<input type='text' class='form-control' name='new_name' id='new_name' value='"
							+ vessel
							+ "' /><br/>"
							+ "<input type='text' class='form-control' name='new_heat_gpio' id='new_heat_gpio' onblur='validate_gpio(this)' "
							+ "value='"
							+ heatgpio
							+ "' placeholder='"
							+ $.i18n.prop("HEAT")
							+ " GPIO_X(_Y)'/><br/>"
							+ "<label>"
							+ "<input type='checkbox' name='heat_invert' />" + $.i18n.prop("INVERT_HEAT")
							+ "</label>"
							+ "<br />"
							+ "<input type='text' class='form-control' name='new_cool_gpio' id='new_cool_gpio' onblur='validate_gpio(this)' "
							+ "value='"
							+ coolgpio
							+ "' placeholder='"
							+ $.i18n.prop("COOL")
							+ " GPIO_X(_Y)'/><br/>"
							+ "<label>"
							+ "<input type='checkbox' name='cool_invert' />" + $.i18n.prop("INVERT_COOL")
							+ "</label>"
							+ "<br />"
							+ "<input type='text' class='form-control' name='aux_gpio' id='aux_gpio' onblur='validate_gpio(this)' "
							+ "value='"
							+ auxgpio
							+ "' placeholder='Aux GPIO_X(_Y)' /><br/>"
							+ "<input type='text' class='form-control' name='cutoff' id='cutoff' "
							+ "value='"
							+ cutoff
							+ "' placeholder='"
							+ $.i18n.prop("CUTOFF_TEMP")
							+ "' /><br/>"
							+ "<input type='text' class='form-control' name='calibration' id='calibration' "
							+ "value='"
							+ calibration
							+ "' placeholder='"
							+ $.i18n.prop("CALIBRATION")
							+ "' /><br/>"
							+ "<button id='update-"
							+ vessel
							+ "' class='btn modeclass' "
							+ "onclick='submitForm(this.form); sleep(2000); location.reload();'>"
							+ $.i18n.prop("UPDATE")
							+ "</button>"
							+ "<button id='cancel-"
							+ vessel
							+ "' class='btn modeclass' "
							+ "onclick='cancelEdit("
							+ vessel
							+ "); waitForMsg(); return false;'>"
							+ $.i18n.prop("CANCEL")
							+ "</button>"
							+ "</form>"
							+ "<button id='hide-"
							+ vessel
							+ "' class='btn modeclass' "
							+ "onclick='toggleDevice(\""
							+ vessel
							+ "\"); waitForMsg(); sleep(1000); location.reload();'>"
							+ $.i18n.prop(toggle) + "</button>" + "</form>"
							+ "</div>");
	
	var checked = (heatinvert == "true");
	$('form[id="' +vessel + '-edit"] input[name="heat_invert"]').prop("checked", checked);
	checked = (coolinvert == "true");
	$('form[id="' +vessel + '-edit"] input[name="cool_invert"]').prop("checked", checked);
}

function validate_gpio(gpio_input) {
	if ((typeof gpio_input) != "string") {
		gpio_string = gpio_input.value
	} else {
		gpio_string = gpio_input;
	}

	gpio_string = gpio_string.toLowerCase().trim();

	if (gpio_string == "") {
		return true;
	}

	if (gpio_string.match(/(gpio)([\d]+)(_[\d]+)$/)) {
		return true;
	}

	if (gpio_string.match(/(gpio)([\d]+)$/)) {
		return true;
	}

	alert($.i18n.prop("INVALID_GPIO"))
	return false;
}

function cancelEdit(vessel) {
	$('#' + vessel + '-edit').empty().remove();
}

function toggleDevice(vessel) {
	$.ajax({
		url : 'toggledevice',
		type : 'POST',
		data : "device=" + vessel,
		success : function(data) {
			data = null
		}
	});
}

function hidePIDForm(vessel) {
	$("#" + vessel + "-controls").hide();
}

function updatePIDStatus(vessel, val) {
	// setup the values
	var vesselDiv = 'form[id="' + vessel + '-form"]';
	$("#" + vessel + "-controls").toggleClass("hidden", false);

	var mode = val.mode.toLowerCase();
	var currentMode = jQuery(vesselDiv + ' input[name="dutycycle"]');

	if (jQuery(vesselDiv + ' input[name="dutycycle"]') != mode) {

		if (mode == "off") {
			selectOff(vessel);
		}
		if (mode == "auto") {
			selectAuto(vessel);
		}
		if (mode == "hysteria") {
			selectHysteria(vessel);
		}
		if (mode == "manual") {
			selectManual(vessel);
		}

		jQuery(vesselDiv + '  input[name="dutycycle"]').val(mode);
	}

	mode = null;

	jQuery('div[id="tempUnit"]').text(val.scale);

	jQuery(vesselDiv + ' input[name="dutycycle"]').val(val.manualduty);
	jQuery(vesselDiv + ' input[name="setpoint"]').val(val.setpoint);
	jQuery(vesselDiv + ' input[name="cycletime"]').val(val.manualtime);
	if ("heat" in val && val.heat.gpio != "") {
		jQuery(vesselDiv + ' div[id="heat"] input[name="heatcycletime"]').val(
				val.heat.cycle);
		jQuery(vesselDiv + ' div[id="heat"] input[name="heatp"]').val(
				val.heat.p);
		jQuery(vesselDiv + ' div[id="heat"] input[name="heati"]').val(
				val.heat.i);
		jQuery(vesselDiv + ' div[id="heat"] input[name="heatd"]').val(
				val.heat.d);
		jQuery(vesselDiv + ' input[name="heatgpio"]').val(val.heat.gpio);
		jQuery(vesselDiv + ' input[name="heatinvert"]').val(val.heat.inverted);
	} else {
		$(vesselDiv + ' a:first').toggleClass("hidden", true);
	}

	if ("cool" in val && val.cool.gpio != "") {
		jQuery(vesselDiv + ' div[id="cool"] input[name="coolcycletime"]').val(
				val.cool.cycle);
		jQuery(vesselDiv + ' div[id="cool"] input[name="coolp"]').val(
				val.cool.p);
		jQuery(vesselDiv + ' div[id="cool"] input[name="cooli"]').val(
				val.cool.i);
		jQuery(vesselDiv + ' div[id="cool"] input[name="coold"]').val(
				val.cool.d);
		jQuery(vesselDiv + ' div[id="cool"] input[name="cooldelay"]').val(
				val.cool.delay);
		jQuery(vesselDiv + ' input[name="coolgpio"]').val(val.cool.gpio);
		jQuery(vesselDiv + ' input[name="coolinvert"]').val(val.cool.inverted);
	} else {
		$(vesselDiv + ' a:last').toggleClass("hidden", true);
	}

	jQuery(vesselDiv + ' input[name="min"]').val(val.min);
	jQuery(vesselDiv + ' input[name="max"]').val(val.max);
	jQuery(vesselDiv + ' input[name="time"]').val(val.time);
	jQuery(vesselDiv + ' input[name="deviceaddr"]').val(val.deviceaddr);

	if ("auxgpio" in val) {
		jQuery(vesselDiv + ' input[name="auxgpio"]').val(val.auxgpio);
	}

	// Enable some stuff
	jQuery(vesselDiv + ' input[name="dutycycle"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="setpoint"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="cycletime"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="heatcycletime"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="heatp"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="heati"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="heatd"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="coolcycletime"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="cooldelay"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="coolp"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="cooli"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="coold"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="min"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="max"]').prop("disabled", true);
	jQuery(vesselDiv + ' input[name="time"]').prop("disabled", true);

	// Aux Mode check
	if ("auxStatus" in val) {
		jQuery(vesselDiv + ' button[id="' + vessel + 'Aux"]').toggleClass("hidden", false);
		if (val.auxStatus == "on" || val.auxStatus == "1") {
			jQuery(vesselDiv + ' button[id="' + vessel + 'Aux"]').style.background = "red";
			jQuery(vesselDiv + ' button[id="' + vessel + 'Aux"]').innerHTML = $.i18n
					.prop("AUX_ON");
		} else {
			jQuery(vesselDiv + ' button[id="' + vessel + 'Aux"]').style.background = "#666666";
			jQuery(vesselDiv + ' button[id="' + vessel + 'Aux"]').innerHTML = $.i18n
					.prop("AUX_OFF");
		}
	} else {
		jQuery(vesselDiv + ' button[id="' + vessel + 'Aux"]').toggleClass("hidden", true);
	}

	$(vesselDiv + ' button[id="sendcommand"]').toggleClass("hidden", true);
	window.disableUpdates = 0;

}

function selectOff(vessel) {

	if ((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.lastIndexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

	var vesselDiv = 'form[id="' + vessel + '-form"]';
	$(vesselDiv + ' input[name="mode"]').val("off");

	jQuery('button[id^="' + vessel + '-modeOff"]')[0].style.background = "red";
	jQuery('button[id^="' + vessel + '-modeManual"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeAuto"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeHysteria"]')[0].style.background = "#666666";

	jQuery('tr[id="' + vessel + '-SP"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-DC"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-DT"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-min"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-max"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-time"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", true);
	$(vesselDiv + ' button[id="sendcommand"]').toggleClass("hidden", false);

	vessel = null;
	return false;
}

function selectAuto(vessel) {

	if ((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.lastIndexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

	var vesselDiv = 'form[id="' + vessel + '-form"]';
	$(vesselDiv + ' input[name="mode"]').val("auto");

	jQuery('button[id^="' + vessel + '-modeOff"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeManual"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeHysteria"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeAuto"]')[0].style.background = "red";

	jQuery('tr[id="' + vessel + '-SP"]').toggleClass("hidden", false);
	$('div[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", false);

	jQuery('tr[id="' + vessel + '-DT"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-DC"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-min"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-max"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-time"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", false)
	$(vesselDiv + ' button[id="sendcommand"]').toggleClass("hidden", false);
	vessel = null;
	return false;
}

function selectHysteria(vessel) {

	if ((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.lastIndexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

	var vesselDiv = 'form[id="' + vessel + '-form"]';
	$(vesselDiv + ' input[name="mode"]').val("hysteria");

	jQuery('button[id^="' + vessel + '-modeOff"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeManual"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeAuto"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeHysteria"]')[0].style.background = "red";

	jQuery('tr[id="' + vessel + '-SP"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-DC"]').toggleClass("hidden", true)
	jQuery('tr[id="' + vessel + '-DT"]').toggleClass("hidden", true);
	$('div[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-min"]').toggleClass("hidden", false);
	jQuery('tr[id="' + vessel + '-max"]').toggleClass("hidden", false);
	jQuery('tr[id="' + vessel + '-time"]').toggleClass("hidden", false);
	jQuery('tr[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", true)
	$(vesselDiv + ' button[id="sendcommand"]').toggleClass("hidden", false);
	vessel = null;
	return false;
}

function selectManual(vessel) {

	if ((typeof vessel) != "string") {
		var v = vessel.id;
		i = v.lastIndexOf("-");
		vessel = v.substr(0, i);
		v = null;
	}

	var vesselDiv = 'form[id="' + vessel + '-form"]';
	$(vesselDiv + ' input[name="mode"]').val("manual");

	jQuery('button[id^="' + vessel + '-modeOff"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeManual"]')[0].style.background = "red";
	jQuery('button[id^="' + vessel + '-modeAuto"]')[0].style.background = "#666666";
	jQuery('button[id^="' + vessel + '-modeHysteria"]')[0].style.background = "#666666";

	jQuery('tr[id="' + vessel + '-SP"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-DC"]').toggleClass("hidden", false);
	jQuery('tr[id="' + vessel + '-DT"]').toggleClass("hidden", false);
	$('div[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-min"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-max"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-time"]').toggleClass("hidden", true);
	jQuery('tr[id="' + vessel + '-tabbedInputs"]').toggleClass("hidden", true);
	$(vesselDiv + ' button[id="sendcommand"]').toggleClass("hidden", false);

	vessel = null;
	return false;
}
function addBadInput(badInputs, varName) {
	if (badInputs != "") {
		badInputs += ", " + varName;
	} else {
		badInputs = varName;
	}
	return badInputs;
}

function submitForm(form) {

	// Are we updating the data?
	if (form.id.lastIndexOf("-form") != -1) {
		var vessel = form.id.substring(0, form.id.lastIndexOf("-form"));

		var formdata = {};

		var preData = jQuery(form).serializeObject();
		
		var validData = true;
		var badInputs = "";
		
		if (isNaN(preData.coolcycletime)) {
			badInputs = "Cool Cycle Time";
		}
		
		if (isNaN(preData.coold)) {
			badInputs = addBadInput(badInputs, "Cool D"); 
		}
		
		if (isNaN(preData.cooldelay)) {
			badInputs = addBadInput(badInputs, "Cool Delay");
		}
		
		if (isNaN(preData.cooli)) {
			badInputs = addBadInput(badInputs, "Cool I");
		}
		
		if (isNaN(preData.coolp)) {
			badInputs = addBadInput(badInputs, "Cool P");
		}
		
		if (isNaN(preData.cutoff)) {
			badInputs = addBadInput(badInputs, "Cut Off");
		}
		
		if (isNaN(preData.cycletime)) {
			badInputs = addBadInput(badInputs, "Cycle Time");
		}
		
		if (isNaN(preData.dutycycle)) {
			badInputs = addBadInput(badInputs, "Duty Cycle");
		}
		
		if (isNaN(preData.heatcycletime)) {
			badInputs = addBadInput(badInputs, "Heat Duty Cycle");
		}
		
		if (isNaN(preData.heatd)) {
			badInputs = addBadInput(badInputs, "Heat D");
		}
		
		if (isNaN(preData.heati)) {
			badInputs = addBadInput(badInputs, "Heat I");
		}
		
		if (isNaN(preData.p)) {
			badInputs = addBadInput(badInputs, "P");
		}
		
		if (isNaN(preData.max)) {
			badInputs = addBadInput(badInputs, "Hysteria Max");
		}
		
		if (isNaN(preData.min)) {
			badInputs = addBadInput(badInputs, "Hysteria Min");
		}
		
		if (isNaN(preData.setpoint)) {
			badInputs = addBadInput(badInputs, "Setpoint");
		}
		
		if (isNaN(preData.time)) {
			badInputs = addBadInput(badInputs, "Hysteria Delay");
		}
		
		if (!validData) {
			alert("Invalid data provided. Check your inputs: " + badInputs);
			return false;
		}
		
		formdata[vessel] = JSON.stringify(preData);
		$.extend(formdata[vessel], {
			"name" : "mode",
			"value" : Window.mode
		});
		// formdata = ;

		$.ajax({
			url : 'updatepid',
			type : 'POST',
			data : formdata,
			dataType : 'json',
			success : function(data) {
				data = null
			}
		});
	} else if (form.id.lastIndexOf("-editVol") != -1) {
		var vessel = form.id.substring(0, form.id.lastIndexOf("-editVol"));
		var formdata = {}
		formdata[vessel] = JSON.stringify(jQuery(form).serializeObject());
		$.ajax({
			url : 'addvolpoint',
			type : 'POST',
			data : formdata,
			dataType : 'json',
			success : function(data) {
				data = null
			}
		});
	} else if (form.id.lastIndexOf("-gravity-edit") != -1) {
		var vessel = form.id.substring(0, form.id.lastIndexOf("-gravity-edit"));
		var formdata = {}
		formdata[vessel] = JSON.stringify(jQuery(form).serializeObject());
		$.ajax({
			url : 'setgravity',
			type : 'POST',
			data : formdata,
			dataType : 'json',
			success : function(data) {
				data = null
			}
		});
	} else if (form.id.lastIndexOf("-editPhSensor") != -1) {
		var sensorName = form.id.substring(0, form.id.lastIndexOf("-editPhSensor"));
		var formdata = {}
		formdata[sensorName] = JSON.stringify(jQuery(form).serializeObject());
		$.ajax({
			url : 'addphsensor',
			type : 'POST',
			data : formdata,
			dataType : 'json',
			success : function(data) {
				data = null
			}
		});
	} else if (form.id.lastIndexOf("-edit") != -1) {
		// We're editing
		var vessel = form.id.substring(0, form.id.lastIndexOf("-edit"));
		var formdata = {}
		formdata[vessel] = JSON.stringify(jQuery(form).serializeObject());
		$.ajax({
			url : 'editdevice',
			type : 'POST',
			data : formdata,
			dataType : 'json',
			success : function(data) {
				data = null
			}
		});
	} else {
		// Another form..
		console.log("Unrecognised form: " + form.id);
		return;
	}

	window.disableUpdates = 0;
	return false;
}

function submitPump(pumpStatus) {
	$.ajax({
		url : 'updatepump',
		type : 'POST',
		data : "toggle=" + pumpStatus.id,
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	return false;
}

function addPump() {
	// Is the edit form already displayed
	var pumpEditForm = $('#pumps-add');
	if (pumpEditForm.val() != undefined) {
		return;
	}

	var pumpDiv = "pumps-titled";

	// Insert a couple of new form elements
	$('#pumps-titled')
			.append(
					"<div id='pumps-add'>"
							+ "<form id='pumps-add-form' name='pumps-add'>"
							+ "<input type='text' name='new_name' id='new_name' value='' placeholder='"
							+ $.i18n.prop("NAME")
							+ "'/><br/>"
							+ "<input type='text' name='new_gpio' id='new_gpio' onblur='validate_gpio(this)' "
							+ "value='' placeholder='GPIO_X(_Y)'/><br/>"
							+ "<label>"
							+ "<input type='checkbox' name='invert' />" + $.i18n.prop("INVERT_GPIO")
							+ "</label><br/>"
							+ "<button id='add-pump' class='btn modeclass' "
							+ "onclick='submitNewPump(this.form); return false;'>"
							+ $.i18n.prop("ADD")
							+ "</button>"
							+ "<button id='cancel-add-pump' class='btn modeclass' "
							+ "onclick='cancelAddPump(); waitForMsg(); return false;'>"
							+ $.i18n.prop("" + $.i18n.prop("CANCEL") + "")
							+ "</button>" + "</form>" + "</div>");
	return false;
}

function cancelAddPump() {
	$('#pumps-add').empty().remove();
	return false;
}

function submitNewPump(form) {
	var data = JSON.stringify(jQuery(form).serializeObject());

	if (form["new_name"].value == null || form["new_name"].value == "") {
		alert($.i18n.prop("PUMPNAMEBLANK"));
		return false;
	}

	if (form["new_gpio"].value == null || form["new_gpio"].value == "") {
		alert($.i18n.prop("GPIO_BLANK"));
		return false;
	}

	$.ajax({
		url : 'addpump',
		type : 'POST',
		data : data,
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	sleep(2000);
	location.reload();
	return false;
}

function addTimer() {
	// Is the edit form already displayed
	var timerEditForm = $('#timer-add');
	if (timerEditForm.val() != undefined) {
		return false;
	}

	// Insert a couple of new form elements
	$('#timers > .panel > .title')
			.append(
					"<div id='timer-add'>"
							+ "<form id='timer-add-form' name='timer-add'>"
							+ "<input type='text' name='new_name' id='new_name' value='' placeholder='"
							+ $.i18n.prop("NAME")
							+ "' /><br/>"
							+ "<button id='add-timer' class='btn modeclass' "
							+ "onclick='submitNewTimer(this.form); return false;'>"
							+ $.i18n.prop("ADD")
							+ "</button>"
							+ "<button id='cancel-add-timer' class='btn modeclass' "
							+ "onclick='cancelAddTimer(); waitForMsg(); return false;'>"
							+ $.i18n.prop("CANCEL") + "</button>" + "</form>"
							+ "</div>");
	return false;

}

function cancelAddTimer() {
	$('#timer-add').empty().remove();
	return false;
}

function submitNewTimer(form) {
	var data = JSON.stringify(jQuery(form).serializeObject());

	if (form["new_name"].value == null || form["new_name"].value == "") {
		alert($.i18n.prop("TIMERNAMEBLANK"));
		return false;
	}

	$.ajax({
		url : 'addtimer',
		type : 'POST',
		data : data,
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	sleep(2000);
	location.reload();
	return false;
}

function addNewTrigger(button) {
	var pid = button.id.replace("addTrigger-", "");
	// Insert a couple of new form elements
	var tempUnit = $("#" + pid + " div >div >div[id='tempUnit']")[0].textContent;
	var $tr = $(button);
    
    $tr.popover('destroy');
	 //$tr.popover();
    $.ajax({
        url: '/getNewTriggers',
        data: {temp: pid},
        dataType: 'html',
        success: function(html) {
            $tr.popover({
                title: 'Create New Trigger',
                content: html,
                placement: 'top',
                html: true,
                trigger: 'manual'
            }).popover('show');
        }
    });
	return false;
}

function newTrigger(button, probe) {
	// Do we need to disable the input form?
	if ($(button).closest("#newTriggersForm").find("[name=type] option:selected").val() == "") {
		$(button).closest(".popover-content").find("#childInput").html("");
		return false;
	}
	
	$.ajax({
        url: '/getTriggerForm',
        data: $(button.parentElement).serializeObject(),
        dataType: 'html',
        success: function(html) {
        	$(button).closest(".popover-content").find("#childInput").html(html)
        }
    });
	return false;
}

function cancelAddTrigger(vessel) {
	var $tr = $(vessel);
	$tr.popover('destroy');
	return false;
}

function submitNewTrigger(form) {
	var data = JSON.stringify(jQuery(form).serializeObject());
	$.ajax({
		url : 'addmashstep',
		type : 'POST',
		data : data,
		success : function(data) {
			data = null
		}
	});
	// Increment the step.
	$("#" + form.id + " > input[name='step']").val(
			parseInt($("#" + form.id + " > input[name='step']").val()) + 1);
	window.disableUpdates = 0;
	return false;
}

function deleteTrigger(vessel, position) {
	// Delete the mash step at the specified position
	$.ajax({
		url : 'delTrigger',
		type : 'POST',
		data : "temp=" + vessel + "&position=" + position,
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	return false;
}

function triggerToggle(button, position) {
	// Parse out the PID from the controller
	var pid = button.id.replace("triggerButton-", "");
	postData = {};
	postData['trigger'] = pid;
	postData['status'] = button.innerText.toLowerCase();

	if (position !== 'undefined') {
		postData['position'] = position;
	}

	$.ajax({
		url : 'toggleTrigger',
		type : 'POST',
		data : postData,
		success : function(data) {
			data = null
		}
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
	var vessel = input.id.substring(0, input.id.lastIndexOf("-mode"));
	var vesselDiv = 'form[id="' + vessel + '-form"]';

	jQuery(vesselDiv + ' input[name="dutycycle"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="setpoint"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="heatcycletime"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="heatp"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="heati"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="heatd"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="coolcycletime"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="coolp"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="cooli"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="coold"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="cooldelay"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="min"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="max"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="time"]').prop("disabled", false);
	jQuery(vesselDiv + ' input[name="cycletime"]').prop("disabled", false);
	$(vesselDiv + ' button[id="sendcommand"]').toggleClass("hidden", true);
	return false;
}

function toggleDiv(id) {
	var e = document.getElementById(id);
	if (e.style.display == 'table-cell')
		e.style.display = 'none';
	else
		e.style.display = 'table-cell';

	e = null;
}

function setTimer(button, stage) {
	// get the current Datestamp
	var curDate = moment().format("YYYY/MM/DDTHH:mm:ssZZ")
	if (button.innerHTML == $.i18n.prop("START")) {
		$("#" + stage).toggleClass("hidden", true);
		$("#" + stage + "Timer").toggleClass("hidden", false);
		formdata = stage + "Start=" + 0;
	} else {
		var tt = $("#" + stage + "Timer").data('tinyTimer');
		if (tt != undefined) {
			tt.stop();
			$("#" + stage + "Timer").removeData('tinyTimer');
		}
		$("#" + stage).toggleClass("hidden", false);
		$("#" + stage + "Timer").toggleClass("hidden", true);
		formdata = stage + "End=" + curDate;
	}

	formdata += "&updated=" + curDate;

	$.ajax({
		url : 'updateday',
		type : 'POST',
		data : formdata,
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	return false;
}

function resetTimer(button, stage) {
	// get the current Datestamp
	var curDate = moment().format("YYYY/MM/DDTHH:mm:ssZZ")
	formdata = stage + "Reset=null";

	formdata += "&updated=" + curDate;

	$.ajax({
		url : 'updateday',
		type : 'POST',
		data : formdata,
		success : function(data) {
			data = null
		}
	});

	$("#" + stage)[0].innerHTML = $.i18n.prop("START");

	$("#" + stage).toggleClass("hidden", false);
	$("#" + stage + "Timer").toggleClass("hidden", true);
	window.disableUpdates = 0;
	return false;
}

function checkTimer(val, stage) {

	if ("name" in val) {
		stage = val.name;
	}

	stage = stage.replace(" ", "_");

	// If We're counting UP
	if ("up" in val) {
		var startTime = moment().subtract(val.up, 'seconds');
		$("#" + stage).toggleClass("hidden", true);
		$("#" + stage + "Timer").toggleClass("hidden", false);
		var tt = $("#" + stage + "Timer").data('tinyTimer');
		if (tt == undefined) {
			$("#" + stage + "Timer").tinyTimer({
				from : startTime.toString()
			});
		} else {
			tt.resetFrom(startTime)
		}
	} else if ("down" in val) {
		// TODO: COUNTDOWN
	} else if ("stopped" in val) {
		var diffTime = val.stopped;
		var hours = Math.floor(diffTime / (60 * 60));
		diffTime -= hours * 60 * 60;
		var mins = Math.floor(diffTime / (60));
		diffTime -= mins * 60;
		$("#" + stage).toggleClass("hidden", false);
		$("#" + stage + "Timer").toggleClass("hidden", true);
		$("#" + stage)[0].innerHTML = pad(hours, 2, 0) + ":" + pad(mins, 2, 0)
				+ ":" + pad(diffTime, 2, 0);
	} else {
		$("#" + stage).toggleClass("hidden", false);
		$("#" + stage + "Timer").toggleClass("hidden", true);
		$("#" + stage)[0].innerHTML = "" + $.i18n.prop("START") + "";
	}
}

function toggleAux(PIDName) {
	$.ajax({
		url : 'toggleAux',
		type : 'POST',
		data : "toggle=" + PIDName,
		success : function(data) {
			data = null
		}
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

	var mashStepRow = $("#mashRow" + pid + "-" + mashStep);
	if (mashStepRow.length == 0) {
		// Add a new row to the Mash Table
		tableRow = "<tr id='mashRow" + pid + "-" + mashStep + "'"
				+ " ondragstart='dragMashStep(event);' draggable='true'"
				+ " ondrop='dropMashStep(event);'"
				+ " ondragover='allowDropMashStep(event);'>"
		tableRow += ("<td>" + mashData['type'] + "</td>");
		tableRow += ("<td>" + mashData['method'] + "</td>");
		tableRow += ("<td>" + mashData['target_temp']
				+ mashData['target_temp_unit'] + "</td>");
		tableRow += ("<td id='mashTimer" + pid + "'>" + mashData['duration'] + "</td>");
		tableRow += ("</tr>");

		if ($("#mashTable" + pid + " > tbody > tr").length == 0) {
			$("#mashTable" + pid + " > tbody").append(tableRow);
		} else {
			mashStepRow = $("#mashTable" + pid + " > tbody > tr").eq(
					mashStep - 1).after(tableRow);
		}

		mashStepRow = mashStepRow.next();
	}

	// Do we have a start time?
	if ("start_time" in mashData) {
		// if there's an end time, we can show the actual time difference
		if ("end_time" in mashData) {
			startDate = moment(mashData['start_time'], "YYYY/MM/DDTHH:mm:ssZZ");
			endDate = moment(mashData['end_time'], "YYYY/MM/DDTHH:mm:ssZZ");
			diff = Math.abs(endDate - startDate);
			seconds = diff / 1000;
			minutes = Math.floor(seconds / 60);
			seconds = seconds - (minutes * 60);

			mashStepRow.find("#mashTimer" + pid).text(
					minutes + ":" + pad(seconds, 2, 0));
		} else {
			// start the timer
			var endMoment = moment(mashData['target_time'], "YYYY/MM/DDTHH:mm:ssZZ");
			mashStepRow.find("#mashTimer" + pid).tinyTimer({
				to : endMoment.toString()
			});
		}
	}
	// active the current row if needs be
	if ("active" in mashData) {
		mashStepRow.addClass('success');
	} else {
		mashStepRow.removeClass('success');
	}
}

function fixWebkitHeightBug() {
	if (window.heightFixed == 1) {
		return;
	}

	$('[id$=-gage]').each(function(index) {
		if ($(this).css('display') == 'none') {
			return;
		}
		$(this).css('display', 'none').height();
		$(this).css('display', 'block');
	});

	window.heightFixed = 1;

}

function toggleBlock(id) {
	$('#' + id).slideToggle();
}

$(window).resize(function() {

	fixWebkitHeightBug();

});

function checkUpdates() {
	$.ajax({
		url : 'checkGit',
		type : 'POST',
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	return false;
}

function updateElsinore() {
	$.ajax({
		url : 'restartUpdate',
		type : 'POST',
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	return false;
}

function editBreweryName() {

	var newName = prompt($.i18n.prop("BREWERY_QUESTION"), window.breweryName);
	if (newName.length == 0 || newName == "" || newName == window.breweryName) {
		return;
	}
	$.ajax({
		url : 'setBreweryName',
		type : 'POST',
		data : 'name=' + newName,
		success : function(data) {
			data = null
		}
	});

}

function readFile(file) {
	var loader = new FileReader();
	var def = $.Deferred(), promise = def.promise();

	// --- provide classic deferred interface
	loader.onload = function(e) {
		def.resolve(e.target.result);
	};
	loader.onprogress = loader.onloadstart = function(e) {
		def.notify(e);
	};
	loader.onerror = loader.onabort = function(e) {
		def.reject(e);
	};
	promise.abort = function() {
		return loader.abort.apply(loader, arguments);
	};

	loader.readAsBinaryString(file);

	return promise;
}

function upload(url, data) {
	var def = $.Deferred(), promise = def.promise();
	var mul = buildMultipart(data);
	var req = $.ajax({
		url : url,
		data : mul.data,
		processData : false,
		type : "post",
		async : true,
		contentType : "multipart/form-data; boundary=" + mul.bound,
		xhr : function() {
			var xhr = jQuery.ajaxSettings.xhr();
			if (xhr.upload) {

				xhr.upload.addEventListener('progress', function(event) {
					var percent = 0;
					var position = event.loaded || event.position; /*
																	 * event.position
																	 * is
																	 * deprecated
																	 */
					var total = event.total;
					if (event.lengthComputable) {
						percent = Math.ceil(position / total * 100);
						def.notify(percent);
					}
				}, false);
			}
			return xhr;
		}
	});
	req.done(function() {
		def.resolve.apply(def, arguments);
	}).fail(function() {
		def.reject.apply(def, arguments);
	});

	promise.abort = function() {
		return req.abort.apply(req, arguments);
	}

	return promise;
}

var buildMultipart = function(data) {
	var key, crunks = [], bound = false;
	while (!bound) {
		bound = $.md5 ? $.md5(new Date().valueOf()) : (new Date().valueOf());
		for (key in data)
			if (~data[key].lastIndexOf(bound)) {
				bound = false;
				continue;
			}
	}

	for (var key = 0, l = data.length; key < l; key++) {
		if (typeof (data[key].value) !== "string") {
			crunks.push("--" + bound + "\r\n"
					+ "Content-Disposition: form-data; name=\""
					+ data[key].name + "\"; filename=\"" + data[key].value[1]
					+ "\"\r\n" + "Content-Type: application/octet-stream\r\n"
					+ "Content-Transfer-Encoding: binary\r\n\r\n"
					+ data[key].value[0]);
		} else {
			crunks.push("--" + bound + "\r\n"
					+ "Content-Disposition: form-data; name=\""
					+ data[key].name + "\"\r\n\r\n" + data[key].value);
		}
	}

	return {
		bound : bound,
		data : crunks.join("\r\n") + "\r\n--" + bound + "--"
	};
};

/*******************************************************************************
 * Drag And Drop functionality *
 ******************************************************************************/

function dragPump(ev) {
	ev.dataTransfer.setData("pumpname", ev.target.childNodes[0].id);
	$('#NewPump')[0].innerHTML = $.i18n.prop("DELETE_PUMP");
}

function dropPump(ev) {
	ev.preventDefault();

	var timer = ev.target;
	if (timer.id != "NewPump") {
		if (timer.className != "pump_wrapper") {
			timer = ev.target.parentElement;
		}

		timer.style.border = "1px solid white";
	}

	var pumpName = ev.dataTransfer.getData("pumpname");

	if (pumpName.lastIndexOf("div-") != 0) {
		pumpName = "div-" + pumpName;
	}

	var refNode = ev.target.parentElement;
	refNode.parentNode.insertBefore(document.getElementById(pumpName),
			refNode.nextSibling);

	// TODO: Update the server with the new location
	var newOrder = "";
	$("[id^='div-Pump']").each(function(index) {
		var divID = this.id;

		if (divID.lastIndexOf('div-') == 0) {
			divID = divID.substring(4);
		}

		newOrder += divID + "=" + index + "&";
	});
	$('#NewPump')[0].innerHTML = $.i18n.prop("NEW_PUMP");
	$.ajax({
		url : 'updatePumpOrder',
		type : 'POST',
		data : newOrder,
		success : function(data) {
			data = null
		}
	});

	// DONE!
}

function allowDropPump(ev) {
	ev.preventDefault();
	var timer = ev.target;
	if (timer.id == "NewPump") {
		return;
	}

	if (timer.className != "pump_wrapper") {
		timer = ev.target.parentElement;
	}

	timer.style.border = "1px dashed black";
}

function dropDeletePump(ev) {
	ev.preventDefault();
	var timer = ev.target;
	if (timer.id != "NewPump") {
		if (timer.className != "pump_wrapper") {
			timer = ev.target.parentElement;
		}

		timer.style.border = "1px solid white";
	}

	var pumpName = ev.dataTransfer.getData("pumpname").replace(" ", "_");

	if (pumpName.lastIndexOf("div-") != 0) {
		basePumpName = pumpName;
		pumpName = "div-" + pumpName;
	}

	$('[id="' + pumpName + '"]').empty().remove();

	var newOrder = "name=" + basePumpName;
	$('#NewPump')[0].innerHTML = $.i18n.prop("NEW_PUMP");
	$.ajax({
		url : 'deletePump',
		type : 'POST',
		data : newOrder,
		success : function(data) {
			data = null
		}
	});
}

function leavePump(ev) {
	ev.preventDefault();
	var timer = ev.target;
	if (timer.id == "NewPump") {
		return;
	}
	if (timer.className != "pump_wrapper") {
		timer = ev.target.parentElement;
	}

	timer.style.border = "1px solid white";
}

// END OF PUMPS

function dragTimer(ev) {
	ev.dataTransfer.setData("timername", ev.target.childNodes[1].id);
	$('#NewTimer')[0].innerHTML = $.i18n.prop("DELETE_TIMER");
}

function dropTimer(ev) {
	ev.preventDefault();
	var timer = ev.target;
	if (timer.id != "NewTimer") {
		if (timer.className != "timer_wrapper") {
			timer = ev.target.parentElement;
		}

		timer.style.border = "1px solid white";
	}
	var timerName = ev.dataTransfer.getData("timername");

	if (timerName.lastIndexOf("div-") != 0) {
		timerName = "div-" + timerName;
	}

	var refNode = ev.target.parentElement;
	refNode.parentNode.insertBefore(document.getElementById(timerName),
			refNode.nextSibling);

	// TODO: Update the server with the new location
	var newOrder = "";
	$("div[id='timers'] > .panel > .panel-body > div").each(function(index) {
		// this.style.border = "1px dashed black";
		var divID = this.id;

		if (divID.lastIndexOf('div-') == 0) {
			divID = divID.substring(4);
		}

		newOrder += divID + "=" + index + "&";
	});
	$('#NewTimer')[0].innerHTML = $.i18n.prop("NEW_TIMER");
	$.ajax({
		url : 'updateTimerOrder',
		type : 'POST',
		data : newOrder,
		success : function(data) {
			data = null
		}
	});

	// DONE!
}

function allowDropTimer(ev) {
	ev.preventDefault();
	var timer = ev.target;
	if (timer.id == "NewTimer") {
		return;
	}

	if (timer.className != "timer_wrapper") {
		timer = ev.target.parentElement;
	}

	timer.style.border = "1px dashed black";
}

function leaveTimer(ev) {
	ev.preventDefault();
	var timer = ev.target;
	if (timer.id == "NewTimer") {
		return;
	}
	if (timer.className != "timer_wrapper") {
		timer = ev.target.parentElement;
	}

	timer.style.border = "1px solid white";
}

function dropDeleteTimer(ev) {
	ev.preventDefault();
	var timerName = ev.dataTransfer.getData("timername");

	if (timerName.lastIndexOf("div-") != 0) {
		baseTimerName = timerName;
		timerName = "div-" + timerName;
	}

	$('[id="' + timerName + '"]').empty().remove();
	var newOrder = "name=" + baseTimerName;

	$('#NewTimer')[0].innerHTML = $.i18n.prop("NEW_TIMER");
	$.ajax({
		url : 'deleteTimer',
		type : 'POST',
		data : newOrder,
		success : function(data) {
			data = null
		}
	});
}

// END OF TIMERS

// Drag and drop functions for mash steps
function getVesselFromMashStep(divID) {
	if (divID.lastIndexOf("mashStep") == 0) {
		var temp = divID.substring(8);
	} else {
		var temp = divID;
	}
	// Explode out
	var vessel = temp.substring(0, temp.lastIndexOf("-"));
	return vessel;
}

function getPositionFromMashStep(divID) {
	if (divID.lastIndexOf("mashStep") == 0) {
		var temp = divID.substring(8);
	} else {
		var temp = divID;
	}
	// Explode out
	var position = temp.substring(temp.lastIndexOf("-") + 1);
	return position;
}

function dragMashStep(ev) {
	var divID = ev.target.id;

	// Explode out
	var vessel = getVesselFromMashStep(divID.substring(7));
	// var position = getPositionFromMashStep(divID);
	ev.dataTransfer.setData("mashStepname", divID);
	$('#addMash-' + vessel)[0].innerHTML = $.i18n.prop("DELETE");
}

function dropMashStep(ev) {
	ev.preventDefault();
	var mashStepName = ev.dataTransfer.getData("mashStepname");
	var vessel = getVesselFromMashStep(mashStepName.substring(7));
	var position = getPositionFromMashStep(mashStepName);

	var refNode = ev.target.parentElement;
	refNode.parentNode.insertBefore(document.getElementById(mashStepName),
			refNode.nextSibling);

	var newOrder = "pid=" + vessel + "&";
	$("#mashTable" + vessel + " > tbody > tr").each(function(index) {
		var divID = this.id;
		if (divID == "") {
			return;
		}

		var oldStep = getPositionFromMashStep(divID);
		newOrder += oldStep + "=" + index + "&";
	});
	$('#addMash-' + vessel)[0].innerHTML = $.i18n.prop("ADD");
	$.ajax({
		url : 'reordermashprofile',
		type : 'POST',
		data : newOrder,
		success : function(data) {
			data = null
		}
	});

	// DONE!
}

function allowDropMashStep(ev) {
	ev.preventDefault();
}

function dropDeleteMashStep(ev) {
	ev.preventDefault();
	var mashStepName = ev.dataTransfer.getData("mashstepname");
	var vessel = getVesselFromMashStep(mashStepName.substring(7));
	var position = getPositionFromMashStep(mashStepName);

	$('[id="' + mashStepName + '"]').empty().remove();
	var newOrder = "pid=" + vessel + "&position=" + position;

	$('#addMash-' + vessel)[0].innerHTML = $.i18n.prop("ADD");
	$.ajax({
		url : 'delMashStep',
		type : 'POST',
		data : newOrder,
		success : function(data) {
			data = null
		}
	});
}

function clearStatus() {
	$.ajax({
		url : 'clearStatus',
		type : 'POST',
		success : function(data) {
			data = null
		}
	});
}

// Functions to hide editable things
function toggleEdit(manualChange) {
	if ("locked" in window) {
		if (window.locked) {
			readWrite(manualChange);
			$(".controller").each(function hideDevice(count, element) {
				$(element).toggleClass("hidden", false);
			});
			return;
		} else {
			readOnly(manualChange);
			$(".controller").each(function hideDevice(count, element) {
				if ($(element).find("input[id='hidden']").val() == "true") {
					$(element).toggleClass("hidden", true);
				} else {
					$(element).toggleClass("hidden", false);
				}
			});
			return;
		}
	}
	readWrite(manualChange);
}

function readOnly(manualChange) {
	if (window.locked != undefined && window.locked) {
		return;
	}
	
	if (manualChange) {
		var formdata = JSON.stringify(jQuery('form[id=settings-form]').serializeObject());
		formdata.recorder = $('form[id="settings-form"] div[id="recorder_enabled"] input').prop("checked");
		$.ajax({
			url : 'updateSystemSettings',
			type : 'POST',
			data: formdata,
			success : function(data) {
				data = null
			}
		});
		
		$.ajax({
			url : 'lockPage',
			type : 'POST',
			success : function(data) {
				data = null
			}
		});

		sleep(2000);
		location.reload();
	}
	
	readOnlyPumps();
	readOnlyTimers();
	readOnlyDevices();
	readOnlyPhSensors();
	$("[id=edit-page]").text($.i18n.prop("EDIT"));
	$("[id=change-scale]").toggleClass("hidden", true);
	$("[id=CheckUpdates]").toggleClass("hidden", true);
	$("[id=logo]").toggleClass("hidden", true);
	window.locked = true;
	
	window.disableUpdates = 0;
}

function readWrite(manualChange) {
	if (window.locked != undefined && !window.locked) {
		return;
	}

	window.locked = false;
	if (manualChange) {
		$.ajax({
			url : 'unlockPage',
			type : 'POST',
			success : function(data) {
				data = null
			}
		});

		sleep(2000);
		location.reload();
	}
	
	readWritePumps();
	readWriteTimers();
	readWriteDevices();
	readWritePhSensors();
	$("[id=edit-page]").text($.i18n.prop("LOCK"));
	$("[id=change-scale]").toggleClass("hidden", false);
	$("[id=CheckUpdates]").toggleClass("hidden", false);
	$("[id=logo]").toggleClass("hidden", false);
	displaySystemSettings();
	
	window.disableUpdates = 0;
}

function readOnlyPumps() {
	// Check the size of the pump list
	var currentCount = $("[id=pumps-body] > div").length;

	if (currentCount == 0) {
		// Hide the Div.
		$('[id=pumps]').css('display', 'none');
	} else {
		// Hide the button
		$('[id=NewPump]').css('display', 'none');
		// Disable drag and drop
		$("[id=pumps-body] > div").each(function(index) {
			this.setAttribute('draggable', false);
		});
	}
}

function readWritePumps() {
	// Check the size of the pump list
	var currentCount = $("[id=pumps-body] > div").length;

	if (currentCount == 0) {
		// Hide the Div.
		$('[id=pumps]').css('display', 'block');
	} else {
		// Hide the button
		$('[id=NewPump]').css('display', 'block');
		// Enable drag and drop
		$("[id=pumps-body] > div ").each(function(index) {
			this.setAttribute('draggable', true);
		});
	}
}

function readOnlyTimers() {
	// Check the size of the pump list
	var currentCount = $("[id=timers-body] > div").length;

	if (currentCount == 0) {
		// Hide the Div.
		$('[id=timers]').css('display', 'none');
	} else {
		// Hide the button
		$('[id=NewTimer]').css('display', 'none');
		// Disable drag and drop
		$("[id=timers-body] > div").each(function(index) {
			this.setAttribute('draggable', false);
		});
	}
}

function readWriteTimers() {
	// Check the size of the pump list
	var currentCount = $("[id=timers-body] > div").length;

	if (currentCount == 0) {
		// Hide the Div.
		$('[id=timers]').css('display', 'block');
	} else {
		// Hide the button
		$('[id=NewTimer]').css('display', 'block');
		// Disable drag and drop
		$("[id=timers-body] > div").each(function(index) {
			this.setAttribute('draggable', true);
		});
	}
}

function readOnlyPhSensors() {
	// Check the size of the pump list
	var currentCount = $("[id=phSensors-body] > div").length;

	if (currentCount == 0) {
		// Hide the Div.
		$('[id=phSensors]').css('display', 'none');
	} else {
		// Hide the button
		$('[id=NewPhSensor]').css('display', 'none');
		// Disable drag and drop
		$("[id=phSensors-body] > div").each(function(index) {
			this.setAttribute('draggable', false);
		});
	}
}

function readWritePhSensors() {
	// Check the size of the pump list
	var currentCount = $("[id=phSensors-body] > div").length;

	if (currentCount == 0) {
		// Hide the Div.
		$('[id=phSensors]').css('display', 'block');
	} else {
		// Hide the button
		$('[id=NewPhSensor]').css('display', 'block');
		// Disable drag and drop
		$("[id=phSensors-body] > div").each(function(index) {
			this.setAttribute('draggable', true);
		});
	}
}

function readWriteDevices() {
	// Check the devices to see which ones aren't configured.
	$("[id$='-title']")
			.each(
					function(index) {
						var vessel = this.id.replace("-title", "")
						var vesselForm = 'form[id="' + vessel + '-form"]';
						var devAddr = $(
								'#' + vesselForm
										+ ' > input[name="deviceaddr"]').val();
						if (devAddr == this.textContent) {
							$('[id=' + this.id.replace("-form", "") + ']').css(
									'display', 'block')
						}

						if (vessel.toLowerCase() == "system") {
							if ($('[id=System-tempGauge]').length == 0) {
								// not enabled
								this.setAttribute("onclick",
										"enableSystem(this);");
							} else {
								this.setAttribute("onclick",
										"disableSystem(this);");
							}
						} else {
							this.setAttribute("onclick", "editDevice(this);");
						}
						$('[id=' + vessel + '-volumeeditbutton').css('display', 'table-cell');
						$('[id=' + vessel + '-title]').css('cursor', "pointer");
						// display the mash table if needs be
						if ($('[id=mashTable' + vessel + '] > tbody > tr').length == 0) {
							// show it
							$('[id=mashTable' + vessel + ']').css('display',
									'block');
						}
					});
}

function readOnlyDevices() {
	// Check the devices to see which ones aren't configured.
	$("[id$='-title']")
			.each(
					function(index) {

						var vessel = this.id.replace("-title", "")
						if (vessel == "messages") {
							return;
						}
						$('[id=' + vessel + '-volumeeditbutton').css('display', 'none');
						var vesselForm = 'form[id="' + vessel + '-form"]';
						var devAddr = $(
								'#' + vesselForm
										+ ' > input[name="deviceaddr"]').val();
						if (devAddr == this.textContent) {
							if (vessel != "System"
									|| this.getAttribute("onClick")
											.lastIndexOf("enable") == 0) {
								$('[id=' + vessel + ']').css('display', 'none')
							}
						}
						this.removeAttribute("onClick");
						$('[id=' + vessel + '-title]').css('cursor', "auto");

						// disable the mash table if needs be
						if ($('[id=mashTable' + vessel + '] > tbody > tr').length == 0) {
							// hide
							$('[id=mashTable' + vessel + ']').css('display',
									'none');
						}
					});
}

function enableSystem(element) {
	$.ajax({
		url : 'addSystem',
		type : 'POST',
		success : function(data) {
			data = null
		}
	});

	sleep(2000);
	location.reload();
}

function disableSystem(element) {
	$.ajax({
		url : 'delSystem',
		type : 'POST',
		success : function(data) {
			data = null
		}
	});
	sleep(2000);
	location.reload();
}

function changeScale() {
	$.ajax({
		url : 'setscale',
		type : 'POST',
		success : function(data) {
			data = null
		}
	});
	sleep(2000);
	location.reload();
}

function embedGraph(vessel) {
	vessel = vessel.trim();
	if ($('#' + vessel + "-graph_title")[0].innerHTML == $.i18n
			.prop("SHOW_GRAPH")) {
		$('#' + vessel + "-graph_title")[0].innerHTML = $.i18n
				.prop("HIDE_GRAPH");
	} else {
		$('#' + vessel + "-graph_title")[0].innerHTML = $.i18n
				.prop("SHOW_GRAPH");
	}

	var alreadyFetched = {};
	window.updateOnly = false;
	var chart = null;
	$("#" + vessel + "-graph_body").width(300);
	$("#" + vessel + "-graph_body").height(200);
	function fetchData() {

		function onDataReceived(series) {
			if (chart == null) {
				series["size"] = {};
				series["size"]["height"] = 150;
				chart = c3.generate(series);
			} else {
				chart.load(series);
			}
			window.updateOnly = true;
		}

		// Normally we call the same URL - a script connected to a
		// database - but in this case we only have static example
		// files, so we need to modify the URL.
		var updateParams = {};
		updateParams["vessel"] = vessel;
		updateParams["bindto"] = vessel + "-graph_body";
		updateParams["updates"] = window.updateOnly;
		
		$.ajax({
			url : "/graph-data/",
			type : "GET",
			dataType : "json",
			data : updateParams,
			success : onDataReceived
		});

		setTimeout(fetchData, 5000);

	}

	fetchData();

}

function displaySystemSettings() {
	$.ajax({
		type : 'GET',
		url : '/getsystemsettings',
		dataType : 'json',
		async : false,
		cache : false,
		timeout : 5000,
		success : function(data) {
			// We have a recorder object, so display the options
			if ($('div[id="settings-form"]').length == 0) {
				var temp =$('div[class="center-right"]');
				
				if (temp == undefined || temp.length == 0) {
					temp = $('div[class="center-right col-md-4"]')
				}
				temp.append('<form id="settings-form" role="form">' +
						'</form>');
			}
			
			if ("recorder" in data) {
				if ($('form[id="settings-form"] div[id="recorder_enabled"').length == 0) {
					$('form[id="settings-form"]')
					.append(
						'<div class="col-md-4 checkbox" id="recorder_enabled">' +
							'<label>' +
								'<input type="checkbox" name="recorder">Recorder Enabled' +
							'</label>' +
						'</div>');
				}
				$('form[id="settings-form"] div[id="recorder_enabled"] input').prop("checked", data.recorder);
			}
			
			if ("recorderDiff" in data) {
				if ($('form[id="settings-form"] div[id="recorder_tolerence"').length == 0) {
					$('form[id="settings-form"]')
					.append(
						'<div class="col-md-4 form-group" id="recorder_tolerence">' +
							'<label for="recorderTolerance">Recorder Tolerance</label>' +
							'<input class="form-control" name="recorderTolerence" type="text">' +
						'</div>');
				}
				
				$('form[id="settings-form"] div[id="recorder_tolerence"] input').val(data.recorderDiff);
			}
			
			if ("recorderTime" in data) {
				if ($('form[id="settings-form"] div[id="recorder_time"').length == 0) {
					$('form[id="settings-form"]')
					.append(
						'<div class="col-md-4 form-group" id="recorder_time">' +
							'<label for="recorderTime">Recorder Sample Time</label>' +
							'<input name="recorderTime" class="form-control" type="text">' +
						'</div>');
				}
				
				$('form[id="settings-form"] div[id="recorder_time"] input').val(data.recorderTime);
			}
			return;
		}
	});
	
}

function readPhSensor(element, name) {
	$.ajax({
		url : "/readPhSensor",
		type : "GET",
		data : {name: name},
		dataType : "json",
		success : function(html) {
			// We got the data from the sensor
			$(element).html(html);
		}
	});
}