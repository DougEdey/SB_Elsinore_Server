package com.sb.elsinore.html;

import java.io.IOException;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Temp;

import static org.rendersnake.HtmlAttributesFactory.*;

public class VolumeEditForm implements Renderable {

    private Temp vessel = null;
    public VolumeEditForm(Temp inVessel) {
        this.vessel = inVessel;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        if (this.vessel == null) {
            return;
        }
        html.div(id(vessel.getName() + "-editVol").class_("col-md-12"))
            .form(id(vessel.getName() + "-editVol")
                    .name(vessel.getName() + "-edit"))
                .input(type("hidden").name("name").id("name")
                        .value(vessel.getName()))
                .br()
//                html.input(type("text").class_("form-control")
//                        .name("adc_pin")
//                        .id("adc_pin").value(vessel.getVolumeAIN())
//                        .add("placeholder", Messages.ANALOGUE_PIN))
//                .br()
                // Replace this with a list
                .select(class_("holo-spinner").name("onewire_address")
                        .id("onewire_address"));
                html.option(value("").selected_if(
                        "".equals(vessel.getVolumeAddress())))
                ._option();
                for (String addr: LaunchControl.getOneWireDevices("/20")) {
                    String address = addr.substring(1);
                    html.option(value(address)
                        .selected_if(address.equals(vessel.getVolumeAddress())))
                        .write(address)
                    ._option();
                }

                try {
                    html._select();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                html.input(type("text").class_("form-control")
                        .name("onewire_offset")
                        .id("onewire_offset").value(vessel.getVolumeOffset())
                        .add("placeholder", Messages.DS2450_OFFSET))
                .br()
                .input(type("number").class_("form-control").add("step", "any")
                        .name("volume")
                        .id("volume").value("")
                        .add("placeholder", Messages.NEW_VOLUME))
                .br();
                html.select(class_("holo-spinner").name("units").id("units"))
                    .option(value(Messages.LITRES)
                            .selected_if(Messages.LITRES
                                    .equals(vessel.getVolumeUnit())))
                            .write(Messages.LITRES)
                    ._option()
                    .option(value(Messages.US_GALLONS)
                            .selected_if(Messages.US_GALLONS
                                    .equals(vessel.getVolumeUnit())))
                            .write(Messages.US_GALLONS)
                    ._option()
                    .option(value(Messages.UK_GALLONS)
                            .selected_if(Messages.UK_GALLONS
                                    .equals(vessel.getVolumeUnit())))
                            .write(Messages.UK_GALLONS)
                    ._option()
                ._select();
                html.br()
                .button(id("updateVol-" + vessel.getName())
                        .onClick("submitForm(this.form);")
                        .class_("btn"))
                    .write(Messages.UPDATE)
                ._button()
                .button(id("cancelVol-" + vessel.getName())
                        .onClick("cancelVolEdit(" + vessel.getName() + ");")
                        .class_("btn"))
                    .write(Messages.CANCEL)
                ._button();
    }

}
