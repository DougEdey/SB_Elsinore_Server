package com.sb.elsinore.html;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.id;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import com.sb.elsinore.Messages;
import com.sb.elsinore.inputs.PhSensor;

public class PhSensorComponent implements Renderable {
    PhSensor phSensor = null;

    public PhSensorComponent(PhSensor newSensor) {
        phSensor = newSensor;
    }
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.div(id("div-" + phSensor).class_("PhSensor_wrapper")
                .onDragstart("dragPhSensor(event);").draggable("true")
                .onDrop("dropPhSensor(event);")
                .onDragover("allowDropTimer(event);")
                .onDragleave("leavePhSensor(event);"))
            .div(class_("phSensorName holo"))
                .write(phSensor.toString())
            ._div()
            .button(id(phSensor.toString()).class_("btn phSensor")
                    .type("submit").value(phSensor + "PhSensor")
                    .onClick("readPhSensor(this, '" + phSensor + "');"
                            + " waitForMsg(); return false;"))
                .write(Messages.UPDATE_VALUE)
            ._button()
        ._div();
    }
}
