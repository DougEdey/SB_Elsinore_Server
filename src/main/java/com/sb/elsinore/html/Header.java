package com.sb.elsinore.html;

import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import org.rendersnake.ext.jquery.JQueryLibrary;

/**
 * Generates the Header for the HTML UI.
 * @author Doug Edey
 *
 */
public class Header implements Renderable {

    @Override
    public void renderOn(HtmlCanvas htmlCanvas) throws IOException {
        htmlCanvas.head()
            .title().write("Elsinore Controller")._title()
            .meta(name("description")
                .add("content","StrangeBrew Elsinore Brewery Controller",false))
            .meta(charset("utf-8"))
            .macros().stylesheet("/templates/static/css/c3.css")
            .macros().stylesheet("/templates/static/elsinore.css")
            .macros().stylesheet("/templates/static/bootstrap-3.0.0/css/bootstrap.min.css")
            .write("<!--[if IE]>", HtmlCanvas.NO_ESCAPE)
                .macros().javascript("templates/static/excanvas.js")
            .write("<![endif]-->", HtmlCanvas.NO_ESCAPE)
            .macros().stylesheet("/templates/static/css/sweet-alert.css")
            .macros().javascript("/templates/static/js/jquery.js")
            .macros().javascript("/templates/static/js/jquery-ui.js")
            .script(language("javascript").type("text/javascript").src("templates/static/js/d3.js").enctype("utf-8"))._script()
            .macros().javascript("/templates/static/js/c3.js")
            .macros().javascript("/templates/static/jquery.fs.stepper.js")
            .macros().javascript("/templates/static/moment.js")
            .macros().javascript("/templates/static/segment-display.js")
            .macros().javascript("/templates/static/pidFunctions.js")
            .macros().javascript("/templates/static/raphael.js")
            .macros().javascript("/templates/static/justgage.js")
            .macros().javascript("/templates/static/tinytimer.min.js")
            .macros().javascript("/templates/static/bootstrap-3.0.0/js/bootstrap.js")
            .macros().javascript("/templates/static/file/jquery.ui.widget.js")
            .macros().javascript("/templates/static/file/jquery.iframe-transport.js")
            .macros().javascript("/templates/static/file/jquery.fileupload.js")
            
            .macros().javascript("/templates/static/jquery.i18n.properties.js")
            .macros().javascript("/templates/static/js/sweet-alert.js")
            .script(type("text/javascript"))
                    .write("var update = 1;"
                            + "var GaugeDisplay = {}; "
                            + "var Gauges = {}; "
                            + "jQuery(document).ready(function() {"
                            + "    setup();"
                            + "});")
            ._script()
        ._head();
    }

}
