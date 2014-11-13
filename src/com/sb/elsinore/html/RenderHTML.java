package com.sb.elsinore.html;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import static org.rendersnake.HtmlAttributesFactory.*;

import org.rendersnake.ext.jquery.JQueryLibrary;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.PID;
import com.sb.elsinore.Pump;
import com.sb.elsinore.Temp;
import com.sb.elsinore.Timer;

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
        html.div(id("PIDs"));
            for (Temp temp: LaunchControl.tempList) {
                PID pid = LaunchControl.findPID(temp.getName());
                if (pid != null) {
                    html.render(new PIDComponent(pid.getName()));
                }
            }
        html._div();

        // Add all the Temps
        html.div(id("tempProbes"));
            for (Temp temp: LaunchControl.tempList) {
                PID pid = LaunchControl.findPID(temp.getName());
                if (pid == null) {
                    html.render(new PIDComponent(temp.getName()));
                }
            }
        html._div();

        // Add in the pumps
        html.div(id("pumps"))
            .div(class_("panel panel-info"))
                .div(id("pumps-titled").class_("title panel-heading"))
                    .write(Messages.PUMPS)
                ._div()
                .div(id("pumps-body").class_("panel-body"));
                    for (Pump pump: LaunchControl.pumpList) {
                        html.render(new PumpComponent(pump.getName()));
                    }

                    html.span(id("NewPump").class_("holo-button pump")
                        .type("submit").onDrop("dropDeletePump(event")
                        .onDragover("allowDropPump(event)")
                        .onClick("addPump()"))
                        .write(Messages.NEW_PUMP)
                    ._span()
                ._div()
            ._div()
        ._div();

        // Add the timers
        html.div(id("timers"))
            .div(class_("panel panel-info"))
                .div(id("timers-header").class_("title panel-heading"))
                    .write(Messages.TIMERS)
                ._div()
                .div(id("timers-body").class_("panel-body"));

                    for (Timer timer: LaunchControl.timerList) {
                        html.render(new TimerComponent(timer.getName()));
                    }

                    html.span(id("NewPump").class_("holo-button pump")
                        .type("submit").onDrop("dropDeletePump(event")
                        .onDragover("allowDropPump(event)")
                        .onClick("addPump()"))
                        .write(Messages.NEW_PUMP)
                    ._span()
                ._div()
            ._div()
        ._div();

        // Check updates button
        html.br().br().div()
            .span(id("CheckUpdates").class_("holo-button")
                    .type("submit").onClick("checkUpdates();"))
                .write(Messages.UPDATE_CHECK)
            ._span()
        ._div()
        ._body()._html();

    }

}
