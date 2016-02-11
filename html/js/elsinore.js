String.prototype.capitalizeFirstLetter = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

function jsonConcat(o1, o2) {
 for (var key in o2) {
  o1[key] = o2[key];
 }
 return o1;
}

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

var triggerDragSrc = null;

function parseVessels(vessels)
{
    $.each(vessels, function (vesselProbe, vesselStatus)
    {
        if (vesselStatus.tempprobe.hidden)
        {
            $("#hiddenProbes").show();

            var probeCard = $("#probes  #" + vesselStatus.deviceAddr);
            if (probeCard.eq(0).size() != 0)
            {
                probeCard.remove();
            }
            var vOption = $("#hiddenProbes #probeList option[value='"+vesselStatus.deviceaddr+"']");
            if (vOption.size() == 0)
            {
                $("#hiddenProbes #probeList").append($("<option>", {
                         value:vesselStatus.deviceaddr,
                         text:decodeURI(vesselStatus.name)
                     }));
                vOption = $("#hiddenProbes #probeList option[value='"+vesselStatus.deviceaddr+"']");
            }
            vOption.data("temp", vesselStatus.tempprobe);
            if ("piddata" in vesselStatus)
            {
                vOption.data("pid", vesselStatus.piddata);
            }
            return;
        }
        var vOption = $("#hiddenProbes #probeList option[value='"+vesselStatus.deviceaddr+"']");
        if (vOption.size() != 0)
        {
            vOption.remove();
        }
        var probeCard = $("#probes  #" + vesselStatus.deviceaddr);
        if (probeCard.eq(0).size() == 0)
        {
            probeCard = addProbeCard(vesselStatus.deviceaddr, vesselStatus.tempprobe.position);
        }
        // Set card name
        setCardName(probeCard, decodeURI(vesselStatus.name));
        loadTempProbeData(probeCard, vesselStatus.tempprobe);
        if ("pidstatus" in vesselStatus)
        {
            loadPIDData(probeCard, vesselStatus.pidstatus);
        }

    });
    if ($("#hiddenProbes #probeList option").length == 0)
    {
        $("#hiddenProbes").hide();
    }
}

function parseTriggers(triggers)
{
    $.each(triggers, function (vesselProbe, triggerList)
    {
        if ($("#probes #" + vesselProbe).size() == 0)
        {
            return;
        }
        var triggerTable = $("#probes #" + vesselProbe + " #triggers");

        if (triggerTable.eq(0).size() == 0)
        {
            $("#probes #" + vesselProbe).append("<ul class='list-group' id='triggers'></ul>");
            triggerTable = $("#probes #" + vesselProbe + " #triggers");
        }

        // Something has been removed, so clear the table.
        if ((triggerTable.children().size() - 1) > triggerList.length)
        {
            triggerTable.empty();
        }
        var enabled = false;
        $.each(triggerList, function(index, trigger)
        {
            if (trigger.active == "true")
            {
                enabled = true;
            }

            var triggerLi = triggerTable.find("#" + trigger.position);
            var content = trigger.description + ": " + trigger.target;
            if (triggerLi.size() == 0)
            {
                triggerTable.append("<li class='list-group-item trigger-row' draggable='true'" +
                    "id='"+trigger.position+"'>" + content +
                    "</li>");
                triggerLi = triggerTable.find("#" + trigger.position)[0];
                triggerLi.addEventListener('dragstart', handleTriggerDragStart, false);
                triggerLi.addEventListener('dragenter', handleTriggerDragEnter, false)
                triggerLi.addEventListener('dragover', handleTriggerDragOver, false);
                triggerLi.addEventListener('dragleave', handleTriggerDragLeave, false);
                triggerLi.addEventListener('drop', handleTriggerDrop, false);
                triggerLi.addEventListener('dragend', handleTriggerDragEnd, false);
                triggerLi.addEventListener('dblclick', handleTriggerEdit, false);
            }
            else
            {
                triggerLi = triggerLi[0];
                if (triggerLi.text != content)
                {
                    triggerLi.text = content;
                }
            }
            if (trigger.active == "true")
            {
                if (!triggerLi.classList.contains("active"))
                {
                    triggerLi.classList.add("active");
                }
            }
            else
            {
                if (triggerLi.classList.contains("active"))
                {
                    triggerLi.classList.remove("active");
                }
            }
        });
        var triggerLi = triggerTable.find("#footer");
        if (triggerLi.size() == 0)
        {
            triggerTable.append("<a href='#' class='list-group-item trigger-row' draggable='true'" +
                "id='footer' onClick='toggleTriggers(this);'>Toggle</a>");
            triggerLi = triggerTable.find("#footer");
        }
        if (enabled)
        {
            triggerLi.attr("name", "deactivate");
            triggerLi.html("Deactivate");
        }
        else
        {
            triggerLi.attr("name", "activate");
            triggerLi.html("Activate");
        }
    });
}

function parseTimers(timers)
{
    $.each(timers, function(name, data) {
        if (name == "")
        {
            return;
        }
        originalName = name;
        name = name.replace(" ", "_");
        var timerCard = $("#timers #"+name);
        if (timerCard.eq(0).size() == 0)
        {
            $("#timers").append("<div class='timer-row' id='"+name+"'>"+
                    "<div class='row'>" +
                        "<div class='form-group form-inline row'>" +
                            "<label for='timer' onclick='editTimer(this);' id='title' class='control-label'>"+originalName+"</label>" +
                            "<input type='text' id='timer' name='timer' class='timer-body form-control timer' placeholder='0 sec' />" +
                            "<button class='btn btn-success start-timer-btn' onClick='startTimer(this);'>Start</button>" +
                            "<button class='btn btn-success resume-timer-btn hidden' onClick='startTimer(this);'>Resume</button>" +
                            "<button class='btn btn-secondary pause-timer-btn hidden' onClick='startTimer(this);'>Pause</button>" +
                            "<button class='btn btn-secondary reset-timer-btn hidden' onClick='resetTimer(this);'>Reset</button>" +
                        "</div>" +
                    "</div>" +
                "</div>");
            timerCard = $("#timers #"+name);
        }

        timerCard.find("#timer").val(msToString(data.elapsedms));
        if (data.mode == "off")
        {
            timerCard.find(".start-timer-btn").show();
            timerCard.find(".resume-timer-btn").hide();
            timerCard.find(".pause-timer-btn").hide();
            timerCard.find(".reset-timer-btn").hide();
        }
        else if (data.mode == "running")
        {
            timerCard.find(".start-timer-btn").hide();
            timerCard.find(".resume-timer-btn").hide();
            timerCard.find(".pause-timer-btn").show();
            timerCard.find(".reset-timer-btn").hide();
        }
        else if (data.mode == "paused")
        {
            timerCard.find(".start-timer-btn").hide();
            timerCard.find(".resume-timer-btn").show();
            timerCard.find(".pause-timer-btn").hide();
            timerCard.find(".reset-timer-btn").show();
        }
    });
}

function parseData(data)
{
    if ("breweryName" in data && $("#brewery_name").text() !== data.breweryName)
    {
        $("#brewery_name").text(data.breweryName);
    }

    if ("vessels" in data)
    {
        parseVessels(data.vessels);
    }
    if ("triggers" in data)
    {
        parseTriggers(data.triggers);
    }
    if ("switches" in data)
    {
        parseSwitches(data.switches);
    }
    if ("timers" in data)
    {
        parseTimers(data.timers);
    }
    if ("phSensors" in data)
    {
        parsePhSensors(data.phSensors);
    }

    if ("recipeCount" in data)
    {
        var recipeName = null;
        if ("recipe" in data)
        {
            recipeName = data.recipe;
        }
        showRecipes(data.recipeCount, recipeName);
        $("#recipeName").text(recipeName);
        $("#recipeName").show();
        $("#clearRecipe").show();
    }
    else
    {
        $("#recipeName").hide();
        $("#clearRecipe").hide();
    }

    if ("message" in data )
    {
        var messageElement = $("#message");
        if (data.message == "")
        {
            messageElement.text("");
            messageElement.hide();
        }
        else
        {
            messageElement.html(data.message);
            messageElement.show();
        }
    }
    if ("version" in data)
    {
        $(".footer #elsinore_sha").html(data.version.sha);
        $(".footer #date").html(data.version.date);
    }
}

function clearMessage()
{
    $.ajax({
        type: 'GET',
        url: '/clearstatus'
    });
}

function requestData()
{
    $.ajax({
        type: 'GET',
        url: '/getstatus',
        dataType: 'json',
        async: true,
        cache: false,
        timeout: 5000,
        success: parseData
        }
    );
}

function addProbeCard(vesselProbe, position)
{
    var div = "<div id='" + vesselProbe 
    + "' class='col-sm-12 col-md-6 col-lg-5 col-xl-4 card card-block text-center m-x'>"
        + "</div>";
    $("#probes > #card-deck").append(div);
    return $("#probes #" + vesselProbe);
}

function addTimerCard(name, position)
{
    var div = "<div id='" + name
    + "' class='col-sm-12 col-md-6 col-lg-5 col-xl-3 card card-block text-center'>"
        + "</div>";
    $("#timers > #card-body").append(div);
    return $("#timers #" + name);
}

function setCardName(card, name)
{
    var addr = card[0].id;
    var title = card.find("#card-header");
    if (title.size() == 0)
    {
        card.prepend("<div class='card-header' id='card-header' onDblClick='editDevice(this);' data-device='"+addr+"'></div>");
        title = card.find("#card-header");
    }

    if (title.text() != name)
    {
        title.text(name);
    }
}

function loadTempProbeData(card, tempprobe)
{
    card.find('.card-header').data("temp", tempprobe);
    var value = card.find("#value");
    if (value.size() == 0)
    {
        card.append("<p class='temperature' ><span id='value'></span><span id='units'></span></div>");
        value = card.find("#value");
    }
    units = card.find("#units");
    value.html(tempprobe.temp.toFixed(2));
    units.html("&#176" + tempprobe.scale);
}

function loadPIDData(card, pid)
{
    card.find('.card-header').data("pid", pid);

    // Setup the progress bar first
    var pidstatus = card.find("#status");
    if (pidstatus.size() == 0)
    {
        card.append("<div id='status-wrapper'><progress id='status' class='progress' min='0' max='100' value='0'></progress><div id='status-text'></div></div>");
        pidstatus = card.find("#status");
    }

    var duty = pid.duty;
    if ("actualduty" in pid)
    {
        duty = pid.actualduty;
    }
    pidstatus.html(Math.round(Math.abs(duty)));
    pidstatus.val(Math.round(Math.abs(duty)));
    card.find("#status-text").text(duty + "%")
    if (duty > 0)
    {
        pidstatus.removeClass("progress-info");
        pidstatus.addClass("progress-danger");
    }
    else
    {
        pidstatus.addClass("progress-info");
        pidstatus.removeClass("progress-danger");
    }

    // Mode buttons
    var pidmode = card.find("#mode");
    if (pidmode.size() == 0)
    {
        card.append("<div id='mode' class='btn-toolbar'>"
            + "<button type='button' onclick='toggleMode(this)' class='btn btn-secondary' id='off'>Off</button>"
            + "<button type='button' onclick='toggleMode(this)' class='btn btn-secondary' id='auto'>Auto</button>"
            + "<button type='button' onclick='toggleMode(this)' class='btn btn-secondary' id='manual'>Manual</button>"
            + "<button type='button' onclick='toggleMode(this)' class='btn btn-secondary' id='hysteria'>Hysteresis</button>"
            + "<button type='button' onclick='editButton(this)' class='btn btn-secondary' id='edit'>Edit</button>"
            + "</div>");
        pidmode = card.find("#mode");
    }

    if ("aux" in pid)
    {
        if (card.find("#aux").length == 0)
        {
            card.append("<div id='mode' class='btn-toolbar m-t'>"
                + "<button type='button' onclick='toggleAux(this)' class='btn btn-warning-outline' id='aux'>Aux</button>"
                + "</div>");
        }
        var auxSwitch = card.find("#aux");
        if (pid.aux.status == "1")
        {

            auxSwitch.removeClass("btn-warning-outline");
            auxSwitch.addClass("btn-warning");
        }
        else
        {
            auxSwitch.addClass("btn-warning-outline");
            auxSwitch.removeClass("btn-warning");
        }
    }

    var selected = pidmode.find(".btn-danger");
    if (selected.size() == 1 && selected.id != pid.mode)
    {
       selected.removeClass("btn-danger");
       selected.addClass("btn-secondary")
    }
    selected = pidmode.find("#" + pid.mode);
    if (!selected.hasClass("btn-danger active"))
    {
        selected.removeClass("btn-secondary");
        selected.addClass("btn-danger active");
    }
    if (pid.mode == "off")
    {
        pidstatus.parent().hide();
    }
    else
    {
        pidstatus.parent().show();
    }

}

function editButton(button) {
    var header = button.parentElement.parentElement.children[0];
    editDevice(header);
}

function toggleAux(button) {
    console.log(button);
    var card = $(getCard(button));

	$.ajax({
		url : 'toggleAux',
		type : 'POST',
		data : "toggle=" + card.attr("id"),
		success : function(data) {
			data = null
		}
	});
	window.disableUpdates = 0;
	return false;
}

function toggleMode(button)
{
    console.log(button);
    var card = $(getCard(button));
    if(!card.find("#mode > .active").hasClass("btn-danger"))
    {
        card.find("#mode > .active").addClass("btn-danger");
    }
    if (card.find("#mode > .btn-success").size() == 1)
    {
        card.find("#mode > .btn-success").removeClass('btn-success');
    }
    if ($(button).hasClass('btn-success'))
    {
        return;
    }
    $(button).removeClass('btn-success btn-danger');
    $(button).addClass('btn-success');
    var mode = button.id;
    switch(mode) {
        case "off":
            console.log("Off");
            showOff(card);
            break;
        case "auto":
            console.log("Auto");
            showAuto(card);
            break;
        case "manual":
            console.log("Manual");
            showManual(card);
            break;
        case "hysteria":
            console.log("Hysteria");
            showHysteria(card);
            break;
    }
}

function getCard(element)
{
    var card = $(element).closest('.card');
    if (card.size() == 1)
    {
        return card[0];
    }
    return null;
}

function showOff(card)
{
    card = $(card);
    pidsettings = card.find("#pidsettings");
    if (pidsettings.size() == 0)
    {
        pidsettings = addPIDSettings(card);
    }
    pidsettings.empty();
    pidsettings.append("<div class='form-group form-inline row'><button type='button' class='btn btn-danger' id='submit' onclick='submitForm(this);'>Submit</button></div>");
}

function addPIDSettings(card)
{
    card.append("<div id='pidsettings' class='m-t'></div>");
    return card.find("#pidsettings");
}

function showAuto(card)
{
    card = $(card);
    var pidsettings = card.find('#pidsettings');
    var piddata = card.find(".card-header").data("pid");
    var tempdata = card.find(".card-header").data("temp");
    if (pidsettings.size() == 0)
    {

        pidsettings = addPIDSettings(card);
    }

    // Make sure we clear out the settings form
    pidsettings.empty();
    pidsettings.append("<form></form>");
    var form = pidsettings.find("form");
    form.append("<div class='form-group'>"
                + "<label class='sr-only' for='setpoint_input'>Setpoint</label>"
                + "<div class='input-group'>"
                    + "<div class='input-group-addon input-group-addon-label'>Setpoint</div>"
                    + "<input type='number' class='form-control' id='setpoint_input' placeholder='Setpoint' >"
                    + "<div class='input-group-addon input-group-addon-unit'>&#176" + tempdata.scale + "</div>"
                + "</div>"
            + "</div>");

    form.find("#setpoint_input").val(piddata.setpoint);
    // Set the first tab to active
    form.find(".nav a:first").tab("show");
    form.append("<div class='form-group form-inline row'><button type='button' class='btn btn-danger' id='submit' onclick='submitForm(this);'>Submit</button></div>");
}

function appendSettings(pidsettings, type, piddata, scale)
{
    pidsettings += "<div id='" + type + "' role='tabpanel' class='tab-pane'>";

    pidsettings += "<div class='form-group'>"
            + "<label class='sr-only' for='cycletime_input'>Cycle Time</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Cycle Time</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='cycletime_input' placeholder='Cycle Time' >"
                + "<div class='input-group-addon input-group-addon-unit'>secs</div>"
            + "</div>"
        + "</div>"
        + "<div class='form-group'>"
            + "<label class='sr-only' for='p_input'>Proportional</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Proportional</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='p_input' placeholder='Proportional' >"
                + "<div class='input-group-addon input-group-addon-unit'>secs/&#176" + scale + "</div>"
            + "</div>"
        + "</div>"
        + "<div class='form-group'>"
            + "<label class='sr-only' for='i_input'>Integral</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Integral</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='i_input' placeholder='Integral' >"
                + "<div class='input-group-addon input-group-addon-unit'>&#176" + scale + "/sec</div>"
            + "</div>"
        + "</div>"
        + "<div class='form-group'>"
            + "<label class='sr-only' for='i_input'>Differential</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Differential</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='d_input' placeholder='Differential' >"
                + "<div class='input-group-addon input-group-addon-unit'>secs</div>"
            + "</div>"
        + "</div>"
    + "</div>";
    return pidsettings;
}

function showManual(card)
{
    card = $(card);
    var pidsettings = card.find('#pidsettings');
    var piddata = card.find(".card-header").data("pid");
    if (pidsettings.size() == 0)
    {
        pidsettings = addPIDSettings(card);
    }

    // Make sure we clear out the settings form
    pidsettings.empty();
    pidsettings.append("<form></form>");
    var form = pidsettings.find("form");
    form.append("<div class='form-group'>"
            + "<label class='sr-only' for='cycletime_input'>Cycle Time</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Cycle Time</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='cycletime_input' placeholder='Cycle Time' >"
                + "<div class='input-group-addon input-group-addon-unit'>secs</div>"
            + "</div>"
        + "</div>");
    form.append("<div class='form-group'>"
            + "<label class='sr-only' for='dutycycle_input'>Cycle Time</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Duty Cycle</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='dutycycle_input' placeholder='Duty Cycle' >"
                + "<div class='input-group-addon input-group-addon-unit'>%</div>"
            + "</div>"
        + "</div>");

    form.find("#dutycycle_input").val(Math.abs(piddata.manualduty));
    form.find("#cycletime_input").val(piddata.manualtime);
    form.append("<div class='form-group form-inline row'><button type='button' class='btn btn-danger' id='submit' onclick='submitForm(this);'>Submit</button></div>");
}

function showHysteria(card)
{
    card = $(card);
    var pidsettings = card.find('#pidsettings');
    var piddata = card.find(".card-header").data("pid");
    var tempdata = card.find(".card-header").data("temp");
    if (pidsettings.size() == 0)
    {
        pidsettings = addPIDSettings(card);
    }

    // Make sure we clear out the settings form
    pidsettings.empty();
    pidsettings.append("<form></form>");
    var form = pidsettings.find("form");

    form.append("<div class='form-group'>"
            + "<label class='sr-only' for='min_input'>Min</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Min</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='min_input' placeholder='Min'>"
                + "<div class='input-group-addon input-group-addon-unit'>&#176" + tempdata.scale + "</div>"
            + "</div>"
        + "</div>");
    form.append("<div class='form-group'>"
            + "<label class='sr-only' for='max_input'>Max</label>"
            + "<div class='input-group'>"
                + "<div class='input-group-addon input-group-addon-label'>Max</div>"
                + "<input type='number' class='form-control form-control-minwidth' id='max_input' placeholder='Max'>"
                + "<div class='input-group-addon input-group-addon-unit'>&#176" + tempdata.scale + "</div>"
            + "</div>"
        + "</div>");
    form.append("<div class='form-group'>"
        + "<label class='sr-only' for='time_input'>Time</label>"
        + "<div class='input-group'>"
            + "<div class='input-group-addon input-group-addon-label'>Time</div>"
            + "<input type='number' class='form-control form-control-minwidth' id='time_input' placeholder='Time'>"
            + "<div class='input-group-addon input-group-addon-unit'>Minutes</div>"
        + "</div>"
    + "</div>");
    form.find("#min_input").val(piddata.min);
    form.find("#max_input").val(piddata.max);
    form.find("#time_input").val(piddata.time);
    form.append("<div class='form-group form-inline row'><button type='button' class='btn btn-danger' id='submit' onclick='submitForm(this);'>Submit</button></div>");
}

function submitForm(element)
{
    if (element.id.lastIndexOf("-editPhSensor") != -1) {
    		var sensorName = element.id.substring(0, element.id.lastIndexOf("-editPhSensor"));
    		var formdata = {}
    		var serialized = $(element).serializeObject();
    		serialized.new_name = encodeURI(serialized.name)
    		serialized["i2c_address"] = serialized[serialized.i2c_device];
    		formdata[sensorName] = JSON.stringify(serialized);
    		$.ajax({
    			url : 'addphsensor',
    			type : 'POST',
    			data : formdata,
    			dataType : 'json',
    			success : function(data) {
    				data = null
    			}
    		});
    		location.reload();
    		return;
    }
    var settings = $(element).closest("#pidsettings");
    var device = $(element).closest(".card-block")[0].id;
    var mButton = $(element).closest(".card-block").find("#mode").find(".btn-success")[0];
    if (mButton == undefined)
    {
        mButton = $(element).closest(".card-block").find("#mode").find(".btn-danger")[0];
    }
    var mode = mButton.id;
    if (mode == "off")
    {
        switchOff(device, settings);
    }
    else if (mode == "auto")
    {
        switchAuto(device, settings);
    }
    else if (mode == "hysteria")
    {
        switchHysteria(device, settings);
    }
    else if (mode == "manual")
    {
        switchManual(device, settings);
    }
}

function switchOff(device, settings)
{
    var data = {};
    data["inputunit"] = device;
    data["mode"] = "off";
    updatePID(device, data);
}

function switchAuto(device, settings)
{
    var data = {};
    data["inputunit"] = device;
    data["mode"] = "auto";

    data["setpoint"] = settings.find("#setpoint_input").val();

    updatePID(device, data);
}

function switchManual(device, settings)
{
    var data = {};
    data["inputunit"] = device;
    data["mode"] = "manual";
    data["dutycycle"] = settings.find("#dutycycle_input").val();
    data["cycletime"] = settings.find("#cycletime_input").val();
    updatePID(device, data);
}

function switchHysteria(device, settings)
{
    var data = {};
    data["inputunit"] = device;
    data["mode"] = "hysteria";
    data["max"] = settings.find("#max_input").val();
    data["min"] = settings.find("#min_input").val();
    data["time"] = settings.find("#time_input").val();
    updatePID(device, data);
}

function updatePID(device, data)
{
    $("#"+device).find("#submit").text("Updating");
    $.ajax({
            url : '/updatepid',
            type : 'POST',
            data : data,
            dataType : 'text',
            success : function(data) {
                data = null
                $("#"+device).find("#submit").text("Updated");
                setTimeout(clearPIDSettings, 5000, device);
            },
            error: function(XMLHttpRequest, textStatus, errorThrown) { 
                alert("Status: " + textStatus); alert("Error: " + errorThrown); 
            }    

        });
    
}

function clearPIDSettings(device) 
{
    var pidSettings = $("#"+device).find("#pidsettings");
    pidSettings.animate({opacity:0.01}, 200, 
        function() {pidSettings.slideUp(200, function()
        { 
            pidSettings.remove();
        });
    });
    var successBtn = $("#"+device).find(".btn-success");
    if (successBtn.size() == 1)
    {
        successBtn.removeClass("btn-success");
        if (successBtn.hasClass("active"))
        {
            successBtn.addClass("btn-danger");
        }

    }
}

$(document).ready(function() {
    $('[data-toggle=offcanvas]').click(function() {
       $('.row-offcanvas').toggleClass('active');
    });
    $("#recipeFile").fileupload({
       dataType : 'json',
       url: '/uploadbeerxml',
       done: function (e, data) {
            $.each(data.result.files, function (index, file) {
                        $('<p/>').text(file.name).appendTo(document.body);
                });
       }
    });
    requestData();
    // When the Analog box is shown grab the analog data and render it
    $('#analog-modal').on('show.bs.modal', function (event) {

          var modal = $(this);

          $.ajax({
            url : '/getphsensorform',
            type : 'GET',
            data: {sensor: modal.data().name},
            success : function(data) {
                $("#analog-modal .modal-body").html(data);
            }
          })
    });
    setInterval("requestData()",10000);
});


function handleTriggerDragStart(e)
{
    this.style.opacity = '0.4';
    triggerDragSrc = e;
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData("text", e.id);
}

function handleTriggerDragOver(e) {
  if (e.preventDefault) {
    e.preventDefault(); // Necessary. Allows us to drop.
  }

  e.dataTransfer.dropEffect = 'move';  // See the section on the DataTransfer object.

  return false;
}

function handleTriggerDragEnter(e) {
  // this / e.target is the current hover target.
  this.classList.add('over');
}

function handleTriggerDragLeave(e) {
  this.classList.remove('over');  // this / e.target is previous target element.
}

function handleTriggerDrop(e) {
    if (e.stopPropagation) {
        e.stopPropagation(); // stops the browser from redirecting.
    }

    // Don't do anything if dropping the same column we're dragging.
    if (triggerDragSrc != e) {
        // Set the source column's HTML to the HTML of the column we dropped on.
        originalId = triggerDragSrc.target.id;
        originalHtml = triggerDragSrc.target.innerHTML;
        triggerDragSrc.target.innerHTML = this.innerHTML;
        triggerDragSrc.target.id = this.id;
        this.innerHTML = originalHtml;
        this.id = originalId;
        // TODO: Update the server here
        var originalProbe = $(triggerDragSrc.target).closest(".card")[0].id;
        var targetProbe = $(this).closest(".card")[0].id;
        if (originalProbe != targetProbe)
        {
            return false;
        }
        var data = {};
        data["tempprobe"] = originalProbe;
        $.each($(".trigger-row"), function (index, row)
        {
            data[row.id] = index;
        });

         $.ajax({
            url : '/reordertriggers',
            type : 'POST',
            data : data,
            dataType : 'text',
            success : function(data) {
                data = null
                $("#"+originalProbe + " .trigger-row").each(function(index, row) {
                    row.style.opacity = 1.0;
                });
            },
            error: function(XMLHttpRequest, textStatus, errorThrown) {
                alert("Status: " + textStatus); alert("Error: " + errorThrown);
            }

        });
    }
    return false;
}

function handleTriggerDragEnd(e) {
    if (e.dataTransfer.dropEffect == 'none')
    {
        var position = e.target.id;
        var device = $(e.target).closest(".card")[0].id;
        // Remove the trigger
        $.ajax({
            url : 'delTriggerStep',
            type : 'POST',
            data : "device=" + device + "&position=" + position,
            success : function(data) {
                data = null
            },
            error: function(XMLHttpRequest, textStatus, errorThrown) {
                alert("Status: " + textStatus); alert("Error: " + errorThrown);
            }
        });
        $(e.target).remove();
    }
    $(".trigger-row").each(function (index, row)
    {
        row.classList.remove('over');
        row.style.opacity = 1.0;
    });
}

function deleteDevice(submitButton)
{
    var form = $(submitButton).parent().parent().find("form");
    var data = {};
    data['probe'] = form.find('#device-address').val();
    $.ajax({
        url : 'deleteprobe',
        type : 'POST',
        data : data,
        data : data,
        dataType : 'text',
        success : function(data) {
             data = null;
             $("#edit-modal").modal('toggle');
             swal({title:"Deleted!"});
         },
         error: function(XMLHttpRequest, textStatus, errorThrown) {
             alert("Status: " + textStatus); alert("Error: " + errorThrown);
         }
    });

}
function saveDevice(submitButton)
{
    var form = $(submitButton).parent().parent().find("form");
    var data = {};
    data['address'] = form.find('#device-address').val();
    data['new_name'] = encodeURI(form.find('#device-name').val());
    data['new_cool_gpio'] = form.find('#cool-gpio').val();
    data['new_heat_gpio'] = form.find('#heat-gpio').val();
    data['aux_gpio'] = form.find('#aux-gpio').val();
    data['cool_invert'] = form.find('#invert-cool').parent().hasClass("active");
    data['heat_invert'] = form.find('#invert-heat').parent().hasClass("active");
    data['aux_invert'] = form.find('#invert-aux').parent().hasClass("active");
    data['cutoff'] = form.find('#shutoff').val();
    data['cutoff_enabled'] = form.find("#cutoff-enabled").parent().hasClass("active");
    data['calibration'] = form.find('#calibration').val();

    var heatdiv = form.parent().parent().find("#heat");
    if (heatdiv.length == 1)
    {
        data["heat_p"] = heatdiv.find("#p_input").val();
        data["heat_i"] = heatdiv.find("#i_input").val();
        data["heat_d"] = heatdiv.find("#d_input").val();
        data["heat_cycletime"] = heatdiv.find("#cycletime_input").val();
    }

    var cooldiv = form.parent().parent().find("#cool");
    if (cooldiv.length == 1)
    {
        data["cool_p"] = cooldiv.find("#p_input").val();
        data["cool_i"] = cooldiv.find("#i_input").val();
        data["cool_d"] = cooldiv.find("#d_input").val();
        data["cool_cycletime"] = cooldiv.find("#cycletime_input").val();
    }

    $.ajax({
        url : 'editdevice',
        type : 'POST',
        data : data,
        data : data,
        dataType : 'text',
        success : function(data) {
             data = null;
             $("#edit-modal").modal('toggle');
             swal({title:"Updated!"});
         },
         error: function(XMLHttpRequest, textStatus, errorThrown) {
             alert("Status: " + textStatus); alert("Error: " + errorThrown);
         }
    });

}

function handleTriggerAdd(e)
{
    var pid = $(e).closest("form").find("#device-address").val();

    //$("#edit-modal").modal('toggle');
    $("#edit-modal").data('device', pid);
    $("#edit-modal .modal-body").empty();
    $.ajax({
        url: '/getNewTriggers',
        data: {temp: pid},
        dataType: 'html',
        showConfirmButton: false,
        showCancelButton: false,
        success: function(html) {
            $("#edit-modal-heading").html("Add New Trigger");
            $("#edit-modal .modal-body").html(html);
            $("#edit-modal .btn-primary").show();
            $("#edit-modal .modal-body").attr("height","100%");
        }
    });
}

function toggleVisibility(e)
{
    var pid = $(e).closest(".modal-body").find("#device-address").val();
    $.ajax({
        url: '/toggleDevice',
        data: {device: pid},
        dataType: 'text',
        success: function(data) { swal({title:"Updated!"});}
    });

}

function handleTriggerEdit(e)
{
    var target = getCard($(e.target));
    var triggerIndex = e.target.id;
    var device = target.id;

    $("#edit-modal").data('device', device);
    $.ajax({
        url: '/gettriggeredit',
        data: {tempprobe: device, position: triggerIndex},
        dataType: 'html',
        success: function(html) {
            $("#edit-modal-heading").html("Edit " + device + " trigger at " + triggerIndex);
            $("#edit-modal .modal-body").html(html);
            $("#edit-modal .modal-body").attr("height","100%");
            $("#edit-modal .btn-primary").show();
            $("#edit-modal .modal-footer").hide();
            $("#edit-modal").modal('toggle');
        }
    });
}

function updateTriggerStep(element) {
	var data = $(element).closest("#editTriggersForm").serializeObject();
    data['tempprobe'] = $("#edit-modal").data('device');

	$.ajax({
		url : 'updatetrigger',
		type : 'POST',
		data : data,
		success : function(data) {
			data = null
		},
		error: function(XMLHttpRequest, textStatus, errorThrown) {
            alert("Status: " + textStatus); alert("Error: " + errorThrown);
        }
	});

	window.disableUpdates = 0;
	return true;
}


function newTrigger(button, probe) {
	// Do we need to disable the input form?
	var childInput = $(button).closest(".modal-body").find("#childInput");
	if ($(button).closest("#newTriggersForm").find("[name=type] option:selected").val() == "") {
		childInput.html("");
		return false;
	}
	childInput.html("Loading...");
	$.ajax({
        url: '/getTriggerForm',
        data: $(button.parentElement).serializeObject(),
        dataType: 'html',
        success: function(html) {
        	childInput.html(html)
        }
    });
	return false;
}


function submitNewTriggerStep(button) {
	var data1 = $(button).closest("#newTriggersForm").serializeObject();
	var data2 = $("#newTriggersForm form").serializeObject();

    var data = {};
	data = jsonConcat(data, data1);
	data = jsonConcat(data, data2);
	if (!("tempprobe" in data)) {
		data['tempprobe'] = data['temp']
	}

	if (!("position" in data)) {
	    $("#" + data['tempprobe'] + ".trigger-row").length;
	}

	$.ajax({
		url : 'addtriggertotemp',
		type : 'POST',
		data : data,
		success : function(data) {
			data = null
		}
	});

	window.disableUpdates = 0;
	return false;
}

function editAnalog(button)
{
    $("#analog-modal").data("name", button.id);
    $("#analog-modal").modal("toggle");
}

function parsePhSensors(sensors)
{
    $.each(sensors, function(name, status){
        var textname = decodeURI(name) + "(" + status.phReading + ")";
        var sensorEle = $("#analog [id='" + name + "']");
        if (sensorEle.length == 0)
        {
            $("#analog .card-body").append("<button id='"+name
            +"' class='btn btn-primary col-xs-10 col-xs-offset-1' onClick='updateAnalog(this);' onDblClick='editAnalog(this);'>"
            +textname+"</button>");
            sensorEle = $("#analog [id='" + name +"']");
        }

        if (sensorEle.text() != textname)
        {
            sensorEle.text(textname);
        }
    });
}

function updateAnalog(element) {
	$.ajax({
		url : "/readPhSensor",
		type : "GET",
		data : {name: element.id},
		dataType : "json",
		success : function(html) {
			// We got the data from the sensor
			$(element).html(html);
		}
	});
}
function parseSwitches(switches)
{
    $.each(switches, function(name, status){
        var textname = decodeURI(name);
        var switchEle = $("#switches [id='" + name +"']");
        if (switchEle.length == 0)
        {
            $("#switches .card-body").append("<button id='"+name+"' class='btn btn-danger-outline m-t col-xs-10 col-xs-offset-1' onClick='toggleSwitch(this);' onDblClick='editSwitch(this);'>"+textname+"</button>");
            switchEle = $("#switches [id='" + name +"']");
        }

        if (status)
        {
            switchEle.removeClass("btn-danger-outline");
            switchEle.addClass("btn-danger");
        }
        else
        {
            switchEle.addClass("btn-danger-outline");
            switchEle.removeClass("btn-danger");
        }

    });
}

function createSwitch()
{
    // Clear the modal first
    var modal = $('#switches-modal');
    modal.find("#name").val("");
    modal.find("#gpio").val("");
    if (modal.find("#invert").hasClass("active"))
    {
        modal.find("#invert").toggle();
    }
    modal.modal('toggle');
}

function deleteSwitch(element)
{
    var data = $("#addSwitch").serializeObject();
    data['name'] = encodeURI(data['name']);
    $.ajax({
        url : 'deleteswitch',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null;
            location.reload();
        }
    });
}

function saveSwitch(element)
{
    var data = $("#addSwitch").serializeObject();
    data['name'] = encodeURI(data['name']);
    $.ajax({
        url : 'addswitch',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null
        }
    });
}

function toggleSwitch(element)
{
    var data = {};
    data["toggle"] = element.id;
    $.ajax({
        url : 'updateswitch',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null
        }
    });
}

function editSwitch(element)
{
    var data = {};
    data["name"] = element.id;
    $.ajax({
        url : 'getswitchsettings',
        type : 'POST',
        data : data,
        dataType: 'json',
        success: function(switchSettings) {
            $("#switches-modal").modal();
            $("#switches-modal-heading").text("Edit Switch");
            $("#switches-modal #name").val(decodeURI(switchSettings.name));
            $("#switches-modal #gpio").val(switchSettings.gpio);
            if (switchSettings.inverted)
            {
                $("#switches-modal #invert").button("toggle");
            }
        }
    });

}

function dismissSwitch()
{
    $("#switches-modal-heading").text("Add Switch");
    $("#switches-modal #name").val("");
    $("#switches-modal #gpio").val("");
}

function editTimer(element)
{
    var data = {};
    data["timer"] = $(element).text();
    $.ajax({
        url : 'gettimersettings',
        type : 'POST',
        data : data,
        dataType: 'json',
        success: function(timerSettings) {
            $("#timers-modal-heading").text("Edit Timer")
            $("#timers-modal #name").val(decodeURI(timerSettings.name));
            $("#timers-modal #duration").val(timerSettings.duration);
            if (timerSettings.inverted)
            {
                $("#timers-modal #invert").button("toggle");
            }

            $('#timers-modal').modal('toggle');
        }
    });
}

function dismissTimer()
{
    $("#timers-modal-heading").text("Add Timer");
    $("#timers-modal #name").val("");
    $("#timers-modal #duration").val("");
}

function deleteTimer(element)
{
    var data = $("#addTimer").serializeObject();
    $.ajax({
        url : 'deletetimer',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null;
            location.reload();
        }
    });
}

function saveTimer(element)
{
    var data = $("#addTimer").serializeObject();
    $.ajax({
        url : 'addtimer',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null
        }
    });
}

function startTimer(element)
{
    var timer = $(element).closest(".timer_row").find("#timer");
    var name = $(element).closest(".timer-row").find("#title");
    timer.timer();
    toggleTimer(name);
}

function toggleTimer(element)
{
    var data = {};
    data["toggle"] = $(element).text();
    $.ajax({
        url : 'toggletimer',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null
        },
         error: function(XMLHttpRequest, textStatus, errorThrown) {
             alert("Status: " + textStatus); alert("Error: " + errorThrown);
         }
    });
}

function resetTimer(element)
{
    var data = {};
    data["reset"] = $(element).closest(".timer-row").find("#title").text();
    $.ajax({
        url : 'toggletimer',
        type : 'POST',
        data : data,
        success : function(data) {
            data = null
        },
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            alert("Status: " + textStatus); alert("Error: " + errorThrown);
        }
    });
}

function msToString(ms)
{
     var min = (ms/1000/60) << 0,
       sec = ((ms/1000) % 60) << 0;
       if (sec < 10)
       {
            sec = "0" + sec;
       }
     return min + ":" + sec;

}

function loadRecipe()
{
    document.getElementById("recipeFile").click();
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

function showRecipes(count, name)
{
    if (name != null && count == 1)
    {
        $("#currentRecipe").hide();
    }
    else
    {
       $.ajax({
       		url : 'getrecipelist',
       		type : 'GET',
       		success : function(data) {
       			$("#currentRecipe").html(data);
       			$("#currentRecipe").show();
       		}
       	});
    }
}

function setRecipe(element)
{
    var recipeName;
    if ($(element).is("select"))
    {
        recipeName = $(element).find(":selected").val();
    }

    if (recipeName != undefined)
    {
        $("#edit-modal").modal('toggle');
        $("#edit-modal").data('recipe', recipeName);
        $("#edit-modal-heading").html("Recipe Settings");
        $.ajax({
            url: '/showrecipe',
            data: {recipeName: recipeName},
            dataType: 'html',
            success: function(html) {
                $("#edit-modal-heading").html(recipeName);
                $("#edit-modal .modal-body").html(html);
            }
        });
    }
}

function setMashProfile(element) {
    setProfile(element, "mash");
}

function setBoilHopProfile(element) {
    setProfile(element, "boil");
}

function setFermProfile(element) {
    setProfile(element, "ferm");
}

function setDryHopProfile(element) {
    setProfile(element, "dry");
}

function setProfile(element, profile) {
    var tempProbe = $(element).parent().find("[name=tempprobe] > :selected").val();
    $.ajax({
        url: '/setprofile',
        data: {profile: profile, tempprobe: tempProbe},
        dataType: "html"
    })
}

function toggleTriggers(element)
{
    var status = $(element).attr("name");
    var originalProbe = $(element).closest(".card")[0].id;
    $.ajax({
        url: '/toggleTrigger',
        data: {tempprobe: originalProbe, status:status, position:-1},
        dataType: "html"
    })
}

function showConfig()
{
    $("#edit-modal").modal('toggle');
    $("#edit-modal .btn-primary").show();
    $("#edit-modal-heading").html("General Settings");
    $.ajax({
        url: '/getsystemsettings',
        dataType: 'json',
        success: function(json) {
        var settingsHTML = "<form id='settings-form' class='form-horizontal'>"
                + "<div class='form-group'><div class='btn-group' data-toggle='buttons'>"
                + " <label class='btn btn-primary-outline active'>"
                       +"<input type='radio' name='scale' id='Celsius' value='C' autocomplete='off'> Celsius"
                     +"</label>"
                     +"<label class='btn btn-primary-outline'>"
                       +"<input type='radio' name='scale' id='Fahrenheit' value='F' autocomplete='off'> Fahrenheit"
                     +"</label>"
                + "</div></div>"
                + "<div class='form-group'>"
                + "<div class='btn-group' data-toggle='buttons'><label class='btn btn-primary-outline' >"
                + '<input type="checkbox" name="restore" autocomplete="off" id="restore">Restore State On Startup</label>'
                + "</div>"
                + "</div>"

                + "<div class='form-group'>"
                + "<div class='btn-group' data-toggle='buttons'><label class='btn btn-primary-outline' >"
                +'<input type="checkbox" name="recorder" autocomplete="off" id="recorder">Recorder Enabled</label>'
                + "</div>"
                + "</div>"
                + "<div class='form-group'>"
                + "<div class='input-group'>"
                    + "<div class='input-group-addon input-group-addon-label'>Tolerance</div>"
                    + "<input type='number' step='0.01' min='0' name='recorderDiff' class='form-control' id='recorderDiff' placeholder='tolerance' >"
                    + "<div class='input-group-addon input-group-addon-unit'>&#176</div>"
                + "</div>"
                + "<div class='form-group'>"
                + "<div class='input-group m-t'>"
                    + "<div class='input-group-addon input-group-addon-label'>Time</div>"
                    + "<input type='number' step='1' min='5000' name='recorderTime' class='form-control' id='recorderTime' placeholder='time' >"
                    + "<div class='input-group-addon input-group-addon-unit'>ms</div>"
                + "</div>"
                + "<div class='form-group m-t'>"
                    + '<div class="btn-group " data-toggle="buttons">'
                        + '<label class="btn btn-primary-outline">'
                        + '<input type="checkbox" id="use_owfs" name="use_owfs" autocomplete="off" value="on">'
                        + 'Use OWFS'
                        + '</label>'
                    + '</div>'
                + '</div>'
                + "<div class='input-group m-t'>"
                    + "<div class='input-group-addon input-group-addon-label'>Server</div>"
                    + "<input name='owfs_server' class='form-control' id='owfs_server' placeholder='Host' >"
                + "</div>"
                + "<div class='input-group m-t'>"
                    + "<div class='input-group-addon input-group-addon-label'>Port</div>"
                    + "<input name='owfs_port' type='number' step='1' min='1' max='65535' class='form-control' id='owfs_port' placeholder='Port' >"
                + "</div>"
                + "<div class='btn-toolbar text-center m-t'>"
                + "<button class='btn btn-success' onclick='saveSystem();'>Save</button>"
                + "</div>"
                + "</form>";
            $("#edit-modal .modal-body").html(settingsHTML);
            $("#edit-modal .modal-footer").hide();
            $("#edit-modal .modal-body #recorderDiff").val(json.recorderDiff);
            $("#edit-modal .modal-body #recorderTime").val(json.recorderTime);
            $("#edit-modal .modal-body #owfs_server").val(json.owfs_server);
            $("#edit-modal .modal-body #owfs_port").val(json.owfs_port);
            if (json.OWFS)
            {
                $("#edit-modal .modal-body #use_owfs").parent().click();
            }

            if (json.recorder)
            {
                $("#edit-modal .modal-body #recorder").parent().click();
            }

            if (json.restore)
            {
                $("#edit-modal .modal-body #restore").parent().click();
            }

            if (json.scale == "F")
            {
                $("#edit-modal .modal-body #Fahrenheit").click();
            }
            else if (json.scale == "C")
            {
                $("#edit-modal .modal-body #Celsius").click();
            }
        }

    });

}

function saveSystem()
{
    var formdata = $('form[id=settings-form]').serializeObject();
    $.ajax({
        url : 'updateSystemSettings',
        type : 'POST',
        data: formdata,
        success : function(data) {
            data = null;
            $("#edit-modal").modal('toggle');
            swal({title:"Updated!"});
        }
    });
}

function changeName()
{

    swal({
        title: "Brewery Name",
        html:'<p><input id="input-field">',
        showCancelButton: true,
        closeOnConfirm: false},
        function(){

            var inputValue = $('#input-field').val();
            if (inputValue === false) return false;
            if (inputValue === "") {
                //swal.showInputError("Please provide a name, or press cancel!");
                return false;
            }
            $.ajax({
                url : 'setBreweryName',
                type : 'POST',
                data: {name: inputValue},
                dataType: "text",
                success : function(data) {
                    data = null;
                    swal({title: "Completed", text:"Brewery name updated"});
                }
            });
        }
    );
};

function selectedI2C(input)
{
    var selectedDevice = input.selectedOptions[0].value;
    $(input.parentElement).find("select[id^=i2c-]").css("display", "none");
    $(input.parentElement).find("select[id=" + selectedDevice + "]").css("display", "block");
}

function selectedInput(input)
{
    if (input.selectedOptions[0].value == "ds2450")
    {
        $(input.parentElement).find("div[id=ds_div]").css("display", "block");
        $(input.parentElement).find("div[id=ain_div]").css("display", "none");
        $(input.parentElement).find("div[id=i2c_div]").css("display", "none");
    }
    else if (input.selectedOptions[0].value == "ain")
    {
        $(input.parentElement).find("div[id=ds_div]").css("display", "none");
        $(input.parentElement).find("div[id=ain_div]").css("display", "block");
        $(input.parentElement).find("div[id=i2c_div]").css("display", "none");
    }
    else if (input.selectedOptions[0].value == "i2c")
    {
        $(input.parentElement).find("div[id=ds_div]").css("display", "none");
        $(input.parentElement).find("div[id=ain_div]").css("display", "none");
        $(input.parentElement).find("div[id=i2c_div]").css("display", "block");
    }
}

function editHidden(element)
{
    var option = $(element).find("option:selected");
    showDeviceEdit(option, option.val(), option.text());
}
function editDevice(element)
{
    showDeviceEdit(element, $(element).data("device"), $(element).text());
}

function showDeviceEdit(element, addr, name)
{
    var piddata = $(element).data("pid");
    var temp = $(element).data("temp");
    var htmlContent = "<div class='text-center'>" +
    "<ul class='nav nav-tabs' role='tablist'>";
    type = "general";
    htmlContent += "<li class='nav-item'><a class='nav-link active' aria-controls='"+type+"' href='#" + type + "' role='tab' data-toggle='tab'>" + type.capitalizeFirstLetter() + "</a></li>";
    if (piddata != null) {
        if ("heat" in piddata && piddata.heat.gpio != "")
        {
            type = "heat";
            htmlContent += "<li class='nav-item'><a class='nav-link' aria-controls='"+type+"' href='#" + type + "' role='tab' data-toggle='tab'>" + type.capitalizeFirstLetter() + "</a></li>";
        }
        if ("cool" in piddata && piddata.cool.gpio != "")
        {
            type = "cool";
            htmlContent += "<li class='nav-item'><a class='nav-link' aria-controls='"+type+"' href='#" + type + "' role='tab' data-toggle='tab'>" + type.capitalizeFirstLetter() + "</a></li>";
        }
    }
    htmlContent += "</ul>";
    htmlContent += "<div class='tab-content'>";
    htmlContent += "<div id='general' role='tabpanel' class='tab-pane active'>";
    htmlContent += '<form id="editDevice">'+
                                '<input type="hidden" id="device-address" value="' + addr + '">' +
                                  '<div class="input-group m-t">'+
                                      '<div class="input-group-addon input-group-addon-label">Name</div>' +
                                      '<input type="text" class="form-control" id="device-name" value="'+name+'">' +
                                  '</div>' +
                                  '<div class="input-group m-t">' +
                                      '<div class="input-group-addon input-group-addon-label">Heat GPIO</div>' +
                                      '<input type="text" class="form-control" id="heat-gpio">' +
                                    '<div class="input-group-btn btn-group" data-toggle="buttons">'+
                                    '<label class="btn btn-primary-outline">' +
                                      '<input type="checkbox" id="invert-heat" autocomplete="off" value="invert">' +
                                      'Invert' +
                                    '</label>' +
                                    '</div>' +
                                  '</div>' +
                                  '<div class="input-group m-t">' +
                                      '<div class="input-group-addon input-group-addon-label">Cool GPIO</div>' +
                                      '<input type="text" class="form-control" id="cool-gpio">' +
                                    '<div class="input-group-btn btn-group" data-toggle="buttons">'+
                                        '<label class="btn btn-primary-outline">' +
                                        '<input type="checkbox" id="invert-cool" value="invert" autocomplete="off">' +
                                        'Invert' +
                                    '</label>' +
                                    '</div>' +
                                  '</div>' +
                                  '<div class="input-group m-t">' +
                                    '<div class="input-group-addon input-group-addon-label">Aux GPIO</div>' +
                                    '<input type="text" class="form-control" id="aux-gpio">' +
                                    '<div class="input-group-btn btn-group" data-toggle="buttons">'+
                                    '<label class="btn btn-primary-outline">' +
                                      '<input type="checkbox" id="invert-aux" value="invert" autocomplete="off">' +
                                      'Invert' +
                                    '</label>' +
                                    '</div>' +
                                  '</div>' +
                                  '<div class="input-group m-t">' +
                                      '<div class="input-group-addon input-group-addon-label">Calibration</div>' +
                                      '<input type="number" class="form-control" id="calibration">' +
                                  '</div>' +
                                  '<div class="input-group m-t">' +
                                      '<div class="input-group-btn btn-group" data-toggle="buttons">'+
                                      '<label class="btn btn-primary-outline">' +
                                        '<input type="checkbox" name="cutoff-enabled" id="cutoff-enabled" value="true" autocomplete="off">' +
                                        'Cutoff Enabled' +
                                      '</label>' +
                                      '</div>' +
                                  '</div>' +
                                  '<div class="input-group m-t">' +
                                      '<div class="input-group-addon input-group-addon-label">Safety</div>' +
                                      '<input type="number" class="form-control" id="shutoff">' +
                                  '</div>';
    htmlContent += "</div>";

    if (piddata != undefined && (piddata.heat.gpio != "" || piddata.cool.gpio != ""))
    {
        if ("heat" in piddata && piddata.heat.gpio != "")
        {
            htmlContent = appendSettings(htmlContent, "heat", piddata, temp.scale);
        }
        if ("cool" in piddata && piddata.cool.gpio != "")
        {
            htmlContent = appendSettings(htmlContent, "cool", piddata, temp.scale);
        }
        htmlContent += "</div>";
    }

    htmlContent += '</form>' +
         '<div class="btn-toolbar m-t">' +
             '<button type="button" id="visibility" class="btn btn-primary" onClick="toggleVisibility(this);">Show</button>' +
             '<button type="button" class="btn btn-primary" onClick="handleTriggerAdd(this);">Add New Trigger</button>' +
             '<button type="button" class="btn btn-primary" onclick="saveDevice(this);">Save Changes</button>' +
             '<button type="button" class="btn btn-primary" onclick="deleteDevice(this);">Delete</button>' +
         '</div>'
         +'</div>';
    $("#edit-modal").modal("toggle");
    $("#edit-modal .modal-body").empty();
    $("#edit-modal .modal-footer").hide();
    $("#edit-modal .modal-body").html(htmlContent);

        var modal = $("#edit-modal .modal-body");
         if (temp.hidden)
         {
           modal.find("#visibility").text("Show");
         }
         else
         {
           modal.find("#visibility").text("Hide");
         }


         if (piddata != undefined)
         {
             modal.find('#cool-gpio').val(piddata.cool.gpio);
             modal.find('#heat-gpio').val(piddata.heat.gpio);
             if (piddata.cool.inverted)
             {
                modal.find('#invert-cool').parent().click();
             }
             if (piddata.heat.inverted)
             {
                modal.find('#invert-heat').parent().click();
             }

             if ("heat" in piddata)
             {
                type = "heat";
                var heatDiv = modal.find("#" + type);
                 heatDiv.find("#cycletime_input").val(piddata[type].cycle);
                 heatDiv.find("#p_input").val(piddata[type].p);
                 heatDiv.find("#i_input").val(piddata[type].i);
                 heatDiv.find("#d_input").val(piddata[type].d);
             }
             if ("cool" in piddata)
             {
                 type = "cool";
                 var heatDiv = modal.find("#" + type);
                  heatDiv.find("#cycletime_input").val(piddata[type].cycle);
                  heatDiv.find("#p_input").val(piddata[type].p);
                  heatDiv.find("#i_input").val(piddata[type].i);
                  heatDiv.find("#d_input").val(piddata[type].d);
             }
             if ("aux" in piddata)
             {
                modal.find('#aux-gpio').val(piddata.aux.gpio);
                if (piddata.aux.inverted)
                {
                   modal.find('#invert-aux').parent().click();
                }
             }
         }
         if (temp != undefined)
         {
             modal.find('#calibration').val(temp.calibration);
             modal.find('#shutoff').val(temp.cutoff);
             if (temp.cutoffEnabled)
             {
                modal.find("#cutoff-enabled").parent().click();
             }
         }
}

function clearRecipe()
{
    $.ajax({
        url : '/clearbeerxml',
        type : 'get',
        dataType: "text",
        success : function(data) {
            data = null;
            swal({title: "Completed", text:"Cleared BeerXML"});
        }
    });
}

function checkForUpdate()
{
    $.ajax({
        url : '/checkgit',
        dataType: "text",
        success : function(data) {
            data = null;
            swal({title: "Checking", text:"Checking git for updates..."});
        },
        error: function (data)
        {
            swal({title:"Update", text:"Already checking for updates"});
        }

    });
    return false;
}

function showHopAdditions()
{
    showRecipeDetails("hops");
}

function showMashProfile()
{
    showRecipeDetails("mash");
}

function showFermentationProfile()
{
    showRecipeDetails("fermentation");
}

function showRecipeDetails(recipeSubset)
{
    var recipeName = $("#recipeName").text();

    if (recipeName != undefined && recipeName != "")
    {
        $("#edit-modal").modal('toggle');
        $("#edit-modal").data('recipe', recipeName);
        $("#edit-modal-heading").html("Recipe Settings");
        $.ajax({
            url: '/showrecipe',
            data: {recipeName: recipeName,
                subset: recipeSubset},
            dataType: 'html',
            success: function(html) {
                $("#edit-modal-heading").html(recipeName);
                $("#edit-modal .modal-body").html(html);
                $("#edit-modal .btn-primary").hide();
            }
        });
    }
    else
    {
        $("#edit-modal").modal('toggle');
        $("#edit-modal-heading").html(recipeSubset);
        $("#edit-modal .modal-body").html("<h1>Not implemented yet, sorry</h1>");
    }
}

function selectPhAddress(element) {
    var form = element.closest("form");
	if (element.value == "") {

		form.find("[id=adc_pin]").show();
	} else {
		form.find("[id=adc_pin]").hide();
	}
}

function phAINChange(element) {
    var form = element.closest("form");
	if (element.value == "") {
		form.find("[id=dsAddress]").show();
		form.find("[id=dsOffset]").show();
	} else {
		form.find("[id=dsAddress]").hide();
		form.find("[id=dsOffset]").hide();
	}
}


function shutdownSystem() {
    sweetAlert({
      title: "Shutdown!",
      text: "This will shutdown the whole system. Are you sure?",
      type: "warning",
      showCancelButton: true,
      confirmButtonColor: "#DD6B55",
      confirmButtonText: "Yes, shutdown system",
      closeOnConfirm: false,
      html: false,
      allowEscapeKey: true,
      allowOutsideClick: true
    }, function(shutdownSystem){
        if (!shutdownSystem) {
        return;
        }
        var txtMsg = "Shutting Down System.";
        reallyShutdown(true);
        swal("Shutting down!",
        txtMsg,
        "success");
      });
}

function shutdown() {
    sweetAlert({
      title: "Shutdown!",
      text: "This will shutdown Elsinore. Are you sure?",
      type: "warning",
      showCancelButton: true,
      confirmButtonColor: "#DD6B55",
      confirmButtonText: "Yes, shutdown",
      closeOnConfirm: false,
      html: false,
      allowEscapeKey: true,
      allowOutsideClick: true
    }, function(shutdownSystem){
        if (!shutdownSystem) {
        return;
        }
        var txtMsg = "Shutting Down Elsinore.";
        reallyShutdown(false);
        swal("Shutting down!",
        txtMsg,
        "success");
      });
}

function reallyShutdown(shutdownSystem) {
$.ajax({
        url : 'shutdownSystem',
        type : 'POST',
        data : "turnoff=" + shutdownSystem,
        success : function(data) {
            data = null
        }
    });
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
