package com.sb.elsinore.html;

import java.io.IOException;
import static org.rendersnake.HtmlAttributesFactory.*;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public class SwitchComponent implements Renderable {

    private String name = "";
    
    public SwitchComponent(String name) {
        this.name = name;
    }
    
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.div(id("div-" + name.replaceAll(" ", "_")).class_("switch_wrapper"))
            .span(id(name.replaceAll(" ", "_")).class_("holo-button switch")
                    .type("submit").value("SubmitCommand")
                    .onClick("submitSwitch(this); waitForMsg(); return false;"))
                .write(name)
            ._span()
        ._div();
    }

}
