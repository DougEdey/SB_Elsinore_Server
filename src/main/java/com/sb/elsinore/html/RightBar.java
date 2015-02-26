package com.sb.elsinore.html;

import java.io.IOException;

import com.sb.elsinore.BrewServer;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import static org.rendersnake.HtmlAttributesFactory.*;

public class RightBar implements Renderable {

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html.div(class_("col-md-2 text-center"))
            .div(class_("breweryImage"))
                .img(class_("center-block").height("200").width("200").id("brewerylogo").src(" "))
            ._div()
            .span(class_("btn btn-default btn-file")).write("Upload Logo")
                .input(type("file").id("logo").add("data-url", "uploadImage"))
            ._span()

            .div(class_("recipe"))
            ._div()
            .span(class_("btn btn-default btn-file")).write("Upload BeerXML")
                .input(type("file").id("beerxml").add("data-url", "uploadbeerxml"))
            ._span();

        if (BrewServer.getRecipeList() != null && BrewServer.getRecipeList().size() > 1) {
            html.select(name("selectRecipe").class_("holo-spinner")
                .onSelect("chooseRecipe(this);"));
            html.option(value("").selected_if(true))
                    .write("Select Recipe")
                    ._option();
            for (String entry: BrewServer.getRecipeList()) {
                html.option(value(entry))
                        .write(entry)
                        ._option();
            }
            html._select();
        }
        html._div();

    }

}
