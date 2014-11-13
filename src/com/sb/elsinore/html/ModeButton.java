package com.sb.elsinore.html;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import static org.rendersnake.HtmlAttributesFactory.*;

public class ModeButton implements Renderable {

    private String device = "";
    private String mode = "";
    private String modeString = "";
    
    public ModeButton(String device, String mode, String modeString) {
        this.device = device;
        this.mode = mode;
        this.modeString = modeString;
    }
    
    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.button(id(device + "-mode" + mode).class_("holo-button modeclass")
                .style("display: table-cell")
                .onClick("disable(this); select" + mode + "(this); return false;"))
            .write(modeString)
        ._button();
    }

}
