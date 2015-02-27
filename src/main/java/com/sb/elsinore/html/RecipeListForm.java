package com.sb.elsinore.html;

import com.sb.elsinore.BrewServer;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.ArrayList;

import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.name;
import static org.rendersnake.HtmlAttributesFactory.value;

/**
 * Render a form allowing selection of the recipe.
 * Created by Doug Edey on 07/02/15.
 */
public class RecipeListForm implements Renderable {

    @Override
    public void renderOn(HtmlCanvas htmlCanvas) throws IOException {
        ArrayList<String> recipeList = BrewServer.getRecipeList();
        htmlCanvas.div(id("selectRecipeForm"))
            .form()
                .select(name("name").class_("holo-spinner")
                .onClick("setRecipe(this);"));
            htmlCanvas.option(value("").selected_if(true))
                    .write("Select Recipe")
                    ._option();
            for (String entry: recipeList) {
                htmlCanvas.option(value(entry))
                    .write(entry)
                    ._option();
            }
            htmlCanvas._select()
        ._form()
        ._div()
        .div(id("recipeContent"))._div();
    }
}
