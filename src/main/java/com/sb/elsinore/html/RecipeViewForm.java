package com.sb.elsinore.html;

import ca.strangebrew.recipe.Hop;
import ca.strangebrew.recipe.Mash;
import ca.strangebrew.recipe.Recipe;
import com.sb.common.SBStringUtils;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.PID;
import com.sb.elsinore.Temp;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import static org.rendersnake.HtmlAttributesFactory.*;

import java.io.IOException;

/**
 * Render a specific recipe in HTML for use.
 * Created by Doug Edey on 24/02/15.
 */
public class RecipeViewForm implements Renderable {
    private Recipe currentRecipe = null;

    public RecipeViewForm(Recipe newRecipe) {
        currentRecipe = newRecipe;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        if (currentRecipe == null) {
            System.out.println("No recipe defined. Stacktrace for call: ");
            Exception runtimeException = new RuntimeException();
            runtimeException.printStackTrace();
            return;
        }

        html.macros().stylesheet("/bootstrap-v4/css/bootstrap.min.css");

        html.div(id("recipeView").class_("text-center"));
        // Do we have mash steps?
        if (currentRecipe.getMash() != null && currentRecipe.getMash().getStepSize() > 0) {
            // Show them!
            Mash mash = currentRecipe.getMash();
            html.div(id("recipeMash"));
            html.div(class_("lead")).write(Messages.MASH_PROFILE);
            html._div();
            html.table(id("mashTable").class_("table table-striped table-bordered"));

            html.tr();
                html.td().write(Messages.METHOD)._td();
                html.td().write(Messages.TYPE)._td();
                html.td().write(Messages.START)._td();
                html.td().write(Messages.END_TEMP)._td();
                html.td().write(Messages.TIME)._td();
            html._tr();

            for(int i = 0; i < mash.getStepSize(); i++) {
                html.tr(id("mashStep-" + i));
                    html.td(id("step-Method")).write(mash.getStepMethod(i))._td();
                    html.td(id("step-Type")).write(mash.getStepType(i))._td();
                    html.td(id("step-startTemp")).write(String.format("%.2f", mash.getStepStartTemp(i)) + mash.getStepTempU(i))._td();
                    html.td(id("step-endTemp")).write(String.format("%.2f", mash.getStepEndTemp(i)) + mash.getStepTempU(i))._td();
                    html.td(id("step-time")).write(SBStringUtils.formatTime(mash.getStepMin(i)))._td();
                html._tr();
            }
            html._table();
            html.select(name("tempprobe").class_("holo-spinner"));
            html.option(value("").selected_if(true))
                    .write("Select Probe")
                    ._option();
            for (PID entry: LaunchControl.pidList) {
                html.option(value(entry.getName()))
                        .write(entry.getName())
                        ._option();
            }
            html._select();
            html.span(id("setMashProfile").class_("btn btn-default").onClick("setMashProfile(this)"))
                    .write(Messages.SET_MASH_PROFILE)._span();
            html._div();
        }

        // Do we have hop additions?
        if (currentRecipe.getHopsListSize() > 0) {
            // Show them!
            html.div(id("recipeBoilHops"));
            html.div(class_("lead")).write(Messages.BOIL_ADDITIONS);
            html._div();
            html.table(id("hopTable").class_("table table-striped table-bordered"));
            html.tr(id("hopTitle"));
                html.th().write(Messages.HOP)._th();
                html.th().write(Messages.AMOUNT)._th();
                html.th().write(Messages.IBU)._th();
                html.th().write(Messages.ALPHA)._th();
                html.th().write(Messages.TIME)._th();
            html._tr();

            for(int i = 0; i < currentRecipe.getHopsListSize(); i++) {
                if (!currentRecipe.getHop(i).getAdd().equals(Hop.BOIL)) {
                    continue;
                }
                html.tr(id("hopAdd-" + i));
                    html.td(id("hop-Name")).write(currentRecipe.getHop(i).getName())._td();
                    html.td(id("hop-Amount")).write(currentRecipe.getHop(i).getAmount().toString())._td();
                    html.td(id("hop-IBU")).write(String.format("%.2f", currentRecipe.getHop(i).getIBU()))._td();
                    html.td(id("hop-Alpha")).write(String.format("%.2f", currentRecipe.getHop(i).getAlpha()))._td();
                    html.td(id("hop-Time")).write(SBStringUtils.formatTime(currentRecipe.getHop(i).getMinutes()))._td();
                html._tr();
            }
            html._table();
            html.select(name("tempprobe").class_("holo-spinner"));
            html.option(value("").selected_if(true))
                    .write("Select Probe")
                    ._option();
            for (Temp entry: LaunchControl.tempList) {
                html.option(value(entry.getName()))
                        .write(entry.getName())
                        ._option();
            }
            html._select();
            html.span(id("setBoilHopProfile").class_("btn btn-default").onClick("setBoilHopProfile(this)"))
                    .write(Messages.SET_BOIL_HOP_PROFILE)._span();
            html._div();
        }

        if (currentRecipe.getFermentStepSize() > 0) {
            html.div(id("recipeFermProfile"));
            html.div(class_("lead")).write(Messages.FERMENT_PROFILE);
            html._div();
            html.table(id("fermentTable").class_("table table-striped table-bordered"));
            html.tr();
                html.th().write(Messages.NAME)._th();
                html.th().write(Messages.TEMP)._th();
                html.th().write(Messages.TIME)._th();
            html._tr();

            for (int i = 0; i < currentRecipe.getFermentStepSize(); i++) {
                html.tr(id("fermStep-" + i));
                    html.td(id("ferm-Name")).write(currentRecipe.getFermentStepType(i))._td();
                    html.td(id("ferm-Temp")).write(String.format("%.2f", currentRecipe.getFermentStepTemp(i)) + currentRecipe.getFermentStepTempU(i))._td();
                    html.td(id("ferm-Time")).write(currentRecipe.getFermentStepTime(i) + " days")._td();
                html._tr();
            }
            html._table();
            html.select(name("tempprobe").class_("holo-spinner"));
            html.option(value("").selected_if(true))
                    .write("Select Probe")
                    ._option();
            for (PID entry: LaunchControl.pidList) {
                html.option(value(entry.getName()))
                        .write(entry.getName())
                        ._option();
            }
            html._select();
            html.span(id("setFermProfile").class_("btn btn-default").onClick("setFermProfile(this)"))
                    .write(Messages.SET_FERM_PROFILE)._span();
            html._div();
        }

        // Do we have hop additions?
        if (currentRecipe.getHopsListSize() > 0) {
            // Show them!
            html.div(id("recipeDryHops"));
            html.div(class_("lead")).write(Messages.DRY_ADDITIONS);
            html._div();
            html.table(id("hopTable").class_("table table-striped table-bordered"));

            html.tr(id("hopTitle"));
                html.th().write(Messages.HOP)._th();
                html.th().write(Messages.AMOUNT)._th();
                html.th().write(Messages.ALPHA)._th();
                html.th().write(Messages.TIME)._th();
            html._tr();

            for(int i = 0; i < currentRecipe.getHopsListSize(); i++) {
                Hop currentHop = currentRecipe.getHop(i);
                if (!currentHop.getAdd().equals(Hop.DRY)) {
                    continue;
                }
                html.tr(id("hopDry-" + i));
                html.td(id("hop-Name")).write(currentHop.getName())._td();
                html.td(id("hop-Amount")).write(currentHop.getAmount().toString())._td();
                html.td(id("hop-Alpha")).write(String.format("%.2f", currentHop.getAlpha()))._td();
                html.td(id("hop-Time")).write(SBStringUtils.formatTime(currentHop.getMinutes()))._td();
                html._tr();
            }
            html._table();
            html.select(name("tempprobe").class_("holo-spinner"));
            html.option(value("").selected_if(true))
                    .write("Select Probe")
                    ._option();
            for (Temp entry: LaunchControl.tempList) {
                html.option(value(entry.getName()))
                        .write(entry.getName())
                        ._option();
            }
            html._select();
            html.span(id("setDryHopProfile").class_("btn btn-default").onClick("setDryHopProfile(this)")).write(Messages.SET_DRY_HOP_PROFILE)._span();
            html._div();
        }
        html._div();
    }
}
