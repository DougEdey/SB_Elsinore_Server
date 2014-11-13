package com.sb.elsinore.html;

import java.io.IOException;
import static org.rendersnake.HtmlAttributesFactory.*;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public class PumpComponent implements Renderable {

    private String name = "";
    
    public PumpComponent(String name) {
        this.name = name;
    }
    
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.div(id("div-" + name.replaceAll(" ", "_")).class_("pump_wrapper")
                .draggable("true")
                .onDragstart("dragPump(event)")
                .onDrop("dropPump(event)")
                .onDragover("allowDropPump(event)")
                .onDragleave("leavePump(event)"))
            .button(id(name.replaceAll(" ", "_")).class_("holo-button pump")
                    .type("submit").value("SubmitCommand")
                    .onClick("submitPump(this); waitForMsg(); return false;"))
                .write(name)
            ._button()
        ._div();
    }

}
