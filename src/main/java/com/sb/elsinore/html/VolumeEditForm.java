package com.sb.elsinore.html;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.devices.I2CDevice;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

import static ca.strangebrew.recipe.Quantity.VOLUME;
import static com.sb.elsinore.TriggerControl.NAME;
import static com.sb.elsinore.inputs.PhSensor.DS_ADDRESS;
import static com.sb.elsinore.inputs.PhSensor.DS_OFFSET;
import static org.rendersnake.HtmlAttributesFactory.*;

public class VolumeEditForm implements Renderable {

    private static final String I_2_C_DEVICE = "i2c_device";
    private static final String I_2_C_ADDRESS = "i2c_address";
    private static final String I_2_C_CHANNEL = "i2c_channel";
    private static final String I_2_C_MODEL = "i2c_model";
    private TempProbe vessel = null;

    public VolumeEditForm(TempProbe inVessel) {
        this.vessel = inVessel;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        if (this.vessel == null) {
            return;
        }
        html.div(id(this.vessel.getName() + "-editVol").class_("col-md-12"))
                .form(id(this.vessel.getName() + "-editVol")
                        .name(this.vessel.getName() + "-edit"))
                .input(type("hidden").name(NAME).id(NAME)
                        .value(this.vessel.getName()))
                .br();
        html.select(class_("holo-spinner").name("select_input")
                .id("select_input").onChange("selectedInput(this);").onLoad("selectedInput(this);"));

        html.option(value("ain")
                .selected_if(this.vessel.getVolumeAIN().length() > 0))
                .write("Onboard AIN")
                ._option();
        html.option(value("ds2450")
                .selected_if(this.vessel.getVolumeAddress().length() > 0))
                .write("DS2450")
                ._option();
        html.option(value("i2c")
                .selected_if(this.vessel.getI2CDevAddressString().length() > 0))
                .write("I2C Input")
                ._option();
        html._select();

        String styleString;
        if (this.vessel.getVolumeAIN().length() > 0) {
            styleString = "display: block";
        } else {
            styleString = "display: none";
        }
        html.div(id("ain_div").name("ain_div").style(styleString));

        html.input(type("text").class_("form-control")
                .name("adc_pin").id("adc_pin")
                .onInput("phAINChange(this);")
                .add("pattern", "[0-7]{1}")
                .title("Only 0-7 are accepted pins.")
                .value(this.vessel.getVolumeAIN())
                .add("placeholder", Messages.ANALOGUE_PIN));
        html._div();
        // Create a list of the DS2450 addresses
        if (this.vessel.getVolumeAddress().length() > 0) {
            styleString = "display: block";
        } else {
            styleString = "display: none";
        }
        html.div(id("ds_div").name("ds_div").style(styleString));

        html.select(class_("holo-spinner")
                .name(DS_ADDRESS).id(DS_ADDRESS).onClick("selectPhAddress(this);"));
        html.option(value("").selected_if(
                "".equals(this.vessel.getVolumeAddress())))
                .write(Messages.DS2450_ADDRESS)
                ._option();
        for (String addr : LaunchControl.getInstance().getOneWireDevices("/20")) {
            String address = addr.substring(1);
            html.option(value(address)
                    .selected_if(address.equals(this.vessel.getVolumeAddress())))
                    .write(address)
                    ._option();
        }

        html._select();
        html.input(type("text").class_("form-control")
                .name(DS_OFFSET).id(DS_OFFSET)
                .value(this.vessel.getVolumeOffset())
                .add("pattern", "[ABCD]{1}")
                .title("Only A, B, C, or D are accepted offsets")
                .add("placeholder", Messages.DS2450_OFFSET));
        html._div();
        // I2C Stuff
        if (this.vessel.getI2CDevAddressString().length() > 0) {
            styleString = "display: block";
        } else {
            styleString = "display: none";
        }
        html.div(id("i2c_div").name("i2c_div").style(styleString));
        html.input(type("text").class_("form-control")
                .name(I_2_C_DEVICE).id(I_2_C_DEVICE)
                .value(this.vessel.getI2CDevNumberString())
                .add("placeholder", Messages.I2C_DEVICE_NUMBER)
        );
        html.input(type("text").class_("form-control")
                .name(I_2_C_ADDRESS).id(I_2_C_ADDRESS)
                .value(this.vessel.getI2CDevAddressString())
                .add("placeholder", Messages.I2C_DEVICE_ADDRESS)
        );
        html.input(type("text").class_("form-control")
                .name(I_2_C_CHANNEL).id(I_2_C_CHANNEL)
                .value(this.vessel.geti2cChannel())
                .add("placeholder", Messages.I2C_DEVICE_CHANNEL)
        );
        html.select(class_("holo-spinner").name(I_2_C_MODEL)
                .id(I_2_C_MODEL));
        html.option(value("").selected_if(
                "".equals(this.vessel.getI2CDevType())))
                .write(Messages.I2C_MODEL)
                ._option();
        for (String model : I2CDevice.getAvailableTypes()) {
            html.option(value(model)
                    .selected_if(model.equals(this.vessel.getI2CDevType())))
                    .write(model)
                    ._option();
        }

        html._select();
        html._div();

        html.br().input(type("number").class_("form-control").add("step", "any")
                .name(VOLUME).id(VOLUME).value("")
                .add("placeholder", Messages.NEW_VOLUME));
        html.select(class_("holo-spinner").name("units").id("units"))
                .option(value(Messages.LITRES)
                        .selected_if(Messages.LITRES
                                .equals(this.vessel.getVolumeUnit())))
                .write(Messages.LITRES)
                ._option()
                .option(value(Messages.US_GALLONS)
                        .selected_if(Messages.US_GALLONS
                                .equals(this.vessel.getVolumeUnit())))
                .write(Messages.US_GALLONS)
                ._option()
                .option(value(Messages.UK_GALLONS)
                        .selected_if(Messages.UK_GALLONS
                                .equals(this.vessel.getVolumeUnit())))
                .write(Messages.UK_GALLONS)
                ._option()
                ._select();
        html.br()
                .button(id("updateVol-" + this.vessel.getName())
                        .onClick("submitForm(this.form);")
                        .class_("btn"))
                .write(Messages.UPDATE)
                ._button()
                .button(id("cancelVol-" + this.vessel.getName())
                        .onClick("cancelVolEdit(" + this.vessel.getName() + ");")
                        .class_("btn"))
                .write(Messages.CANCEL)
                ._button();
    }

}
