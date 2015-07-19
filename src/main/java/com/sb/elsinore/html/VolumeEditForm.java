package com.sb.elsinore.html;

import java.io.IOException;

import com.sb.elsinore.devices.I2CDevice;
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
                .br();
        html.select(class_("holo-spinner").name("select_input")
                .id("select_input").onChange("selectedInput(this);").onLoad("selectedInput(this);"));

        html.option(value("ain")
                .selected_if(vessel.getVolumeAIN().length() > 0))
                .write("Onboard AIN")
                ._option();
        html.option(value("ds2450")
                .selected_if(vessel.getVolumeAddress().length() > 0))
                .write("DS2450")
                ._option();
        html.option(value("i2c")
                .selected_if(vessel.getI2CDevAddressString().length() > 0))
                .write("I2C Input")
                ._option();
        html._select();

        String styleString;
        if (vessel.getVolumeAIN().length() > 0)
        {
            styleString = "display: block";
        }
        else
        {
            styleString = "display: none";
        }
        html.div(id("ain_div").name("ain_div").style(styleString));

        html.input(type("text").class_("form-control")
                .name("adc_pin").onInput("phAINChange(this);")
                .add("pattern", "[0-7]{1}")
                .title("Only 0-7 are accepted pins.")
                .id("adc_pin").value(vessel.getVolumeAIN())
                .add("placeholder", Messages.ANALOGUE_PIN));
        html._div();
        // Create a list of the DS2450 addresses
        if (vessel.getVolumeAddress().length() > 0)
        {
            styleString = "display: block";
        }
        else
        {
            styleString = "display: none";
        }
        html.div(id("ds_div").name("ds_div").style(styleString));

        html.select(class_("holo-spinner").name("dsAddress")
                .id("dsAddress").onClick("selectPhAddress(this);"));
        html.option(value("").selected_if(
                "".equals(vessel.getVolumeAddress())))
                .write(Messages.DS2450_ADDRESS)
                ._option();
        for (String addr: LaunchControl.getOneWireDevices("/20")) {
            String address = addr.substring(1);
            html.option(value(address)
                    .selected_if(address.equals(vessel.getVolumeAddress())))
                    .write(address)
                    ._option();
        }

        html._select();
        html.input(type("text").class_("form-control")
                .name("dsOffset")
                .id("dsOffset").value(vessel.getVolumeOffset())
                .add("pattern", "[ABCD]{1}")
                .title("Only A, B, C, or D are accepted offsets")
                .add("placeholder", Messages.DS2450_OFFSET));
        html._div();
        // I2C Stuff
        if (vessel.getI2CDevAddressString().length() > 0) {
            styleString = "display: block";
        }
        else
        {
            styleString = "display: none";
        }
        html.div(id("i2c_div").name("i2c_div").style(styleString));
        html.input(type("text").class_("form-control")
                        .name("i2c_device").id("i2c_device")
                        .value(vessel.getI2CDevNumberString())
                        .add("placeholder", Messages.I2C_DEVICE_NUMBER)
        );
        html.input(type("text").class_("form-control")
                        .name("i2c_address").id("i2c_address")
                        .value(vessel.getI2CDevAddressString())
                        .add("placeholder", Messages.I2C_DEVICE_ADDRESS)
        );
        html.input(type("text").class_("form-control")
                        .name("i2c_channel").id("i2c_channel")
                        .value(vessel.geti2cChannel())
                        .add("placeholder", Messages.I2C_DEVICE_CHANNEL)
        );
        html.select(class_("holo-spinner").name("i2c_model")
                .id("i2c_model"));
        html.option(value("").selected_if(
                "".equals(vessel.getI2CDevType())))
                .write(Messages.I2C_MODEL)
                ._option();
        for (String model: I2CDevice.getAvailableTypes()) {
            html.option(value(model)
                    .selected_if(model.equals(vessel.getI2CDevType())))
                    .write(model)
                    ._option();
        }

        html._select();
        html._div();

                html.br().input(type("number").class_("form-control").add("step", "any")
                        .name("volume")
                        .id("volume").value("")
                        .add("placeholder", Messages.NEW_VOLUME));
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
