package com.sb.elsinore.html;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import com.sb.elsinore.LaunchControl;

import static org.rendersnake.HtmlAttributesFactory.*;

/**
 * Create a top bar for the web page.
 * @author Doug Edey
 */
public class TopBar implements Renderable {

    @Override
    public final void renderOn(HtmlCanvas htmlCanvas) throws IOException {
        htmlCanvas.div(id("topbar").class_("col-md-12"))
            .div(id("breweryTitle").class_("left  col-md-2"))
                .h1(class_("breweryNameHeader").onClick("editBreweryName()"))
                    .div(id("breweryName")).write("Elsinore")._div()
                    .small()
                        .a(href(LaunchControl.RepoURL))
                            .content("StrangeBrew Elsinore")
                    ._small()
                ._h1()
            ._div()
            .div(class_("center-left col-md-4"))._div()
            .div(class_("center-right col-md-4"))._div()
            .div(class_("right col-md-2"))
                .div(class_("breweryImage"))
                    .img(height("200").width("200").id("brewerylogo").src(" "))
                ._div()
                .input(type("file").id("logo").add("data-url", "uploadImage"))
            ._div()
        ._div()
        .div(id("messages").style("display:none"))
            .div(class_("panel panel-warning"))
                .div(id("messages-title").class_("title panel-heading"))._div()
                .div(id("messages-body").class_("panel-body"))._div()
            ._div()
        ._div();
    }

}
