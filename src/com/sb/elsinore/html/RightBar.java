package com.sb.elsinore.html;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.height;
import static org.rendersnake.HtmlAttributesFactory.type;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public class RightBar implements Renderable {

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.div(class_("col-md-2"))
            .div(class_("breweryImage"))
                .img(height("200").width("200").id("brewerylogo").src(" "))
            ._div()
            .input(type("file").id("logo").add("data-url", "uploadImage"))
        ._div();

    }

}
