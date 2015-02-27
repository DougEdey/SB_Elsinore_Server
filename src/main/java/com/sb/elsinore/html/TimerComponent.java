package com.sb.elsinore.html;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import static org.rendersnake.HtmlAttributesFactory.*;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Timer;

public class TimerComponent implements Renderable {

    private String name = "";
    private String safeName = "";

    public TimerComponent(String name) {
        this.name = name;
        this.safeName = name.replace(" ", "_");
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.div(id("div-" + safeName).class_("timer_wrapper")
                .onDragstart("dragTimer(event);").draggable("true")
                .onDrop("dropTimer(event);")
                .onDragover("allowDropTimer(event);")
                .onDragleave("leaveTimer(event);"))
            .div(class_("timerName holo"))
                .write(name)
            ._div()
            .span(id(safeName).class_("holo-button switch")
                    .type("submit").value(name + "Timer")
                    .onClick("setTimer(this, '" + name
                            + "'); waitForMsg(); return false;"))
                .write(Messages.START)
            ._span()
            .span(id(safeName + "Timer").class_("holo-button switch hidden")//.style("display:none")
                    .type("submit").value(name + "Timer")
                    .onClick("setTimer(this, '" + name
                            + "'); waitForMsg(); return false;"))
            ._span()
            .span(id(safeName).class_("holo-button switch")
                    .type("submit").value(name + "Timer")
                    .onClick("resetTimer(this, '" + name
                            + "'); waitForMsg(); return false;"))
                .write(Messages.RESET)
            ._span()
        ._div();
    }

}
