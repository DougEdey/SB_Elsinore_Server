package com.sb.elsinore.html;

import java.io.IOException;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import static org.rendersnake.HtmlAttributesFactory.*;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Switch;
import com.sb.elsinore.Temp;
import com.sb.elsinore.Timer;
import com.sb.elsinore.inputs.PhSensor;

public class RenderHTML implements Renderable {

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.render(DocType.HTML5)
            .html()
            .render(new Header())
            .body()
            .render(new TopBar())
            .br();

        // Add all the PIDs
        html.div(id("main").class_("container-fluid col-md-10"));

        html.div(id("Probes").class_("row no-gutter"));
        int i = 0;
            for (Temp temp: LaunchControl.tempList) {
                if (LaunchControl.isLocked()
                        && temp.getName().equals(temp.getProbe())) {
                    continue;
                }
                if (i > 0 && i % 4 == 0) {
                    html._div();
                    html.div(id("Probes").class_("row no-gutter"));
                }
                i++;
                html.render(new PIDComponent(temp.getName()));
            }
        html._div();
        // Add in the switches
        html.div(class_("row"));
        html.div(id("switches").class_("col-md-3 text-center"))
            .div(class_("panel panel-info"))
                .div(id("switches-titled").class_("title panel-heading"))
                    .write(Messages.SWITCHES)
                ._div()
                .div(id("switches-body").class_("panel-body"));
                    for (Switch aSwitch : LaunchControl.switchList) {
                        html.render(new SwitchComponent(aSwitch.getName()));
                    }

                    html.span(id("NewSwitch").class_("btn btn-info switch")
                        .onClick("addSwitch()"))
                        .write(Messages.NEW_SWITCH)
                    ._span()
                ._div()
            ._div()
        ._div();

        // Add the timers
        html.div(id("timers").class_("col-md-3 text-center"))
            .div(class_("panel panel-info"))
                .div(id("timers-header").class_("title panel-heading"))
                    .write(Messages.TIMERS)
                ._div()
                .div(id("timers-body").class_("panel-body"));

                    for (Timer timer: LaunchControl.timerList) {
                        html.render(new TimerComponent(timer.getName()));
                    }

                    html.span(id("NewTimer").class_("btn btn-info timer")
                        .onClick("addTimer()"))
                        .write(Messages.NEW_TIMER)
                    ._span()
                ._div()
            ._div()
        ._div();

        // Add the pH Sensors
        html.div(id("phSensors").class_("col-md-3 text-center"))
            .div(class_("panel panel-info"))
                .div(id("phSensors-header").class_("title panel-heading"))
                    .write(Messages.PH_SENSORS)
                ._div()
                .div(id("phSensors-body").class_("panel-body"));
                    for (PhSensor sensor: LaunchControl.phSensorList) {
                        html.render(new PhSensorComponent(sensor));
                    }
                html.span(id("NewPhSensor").class_("btn btn-info sensor")
                        .type("submit")
                        .onClick("addPhSensor(this);"))
                        .write(Messages.NEW_PHSENSOR)
                    ._span()
                ._div()
            ._div()
        ._div();
        html._div();
        // Check updates button
        html.br().br().div()
            .button(id("CheckUpdates").class_("btn")
                    .type("submit").onClick("checkUpdates();"))
                .write(Messages.UPDATE_CHECK)
            ._button()
        ._div()
        ._div()
        .render(new RightBar())
        ._body()._html();

    }

}
