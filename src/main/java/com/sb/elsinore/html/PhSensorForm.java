package com.sb.elsinore.html;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.type;
import static org.rendersnake.HtmlAttributesFactory.value;

import java.io.IOException;

import com.sb.elsinore.devices.I2CDevice;
import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.inputs.PhSensor;

public class PhSensorForm implements Renderable {

    private final PhSensor phSensor;

    public PhSensorForm(PhSensor newSensor) {
        this.phSensor = newSensor;
    }

    @Override
    public void renderOn(final HtmlCanvas html) throws IOException {
        html.div(id(phSensor.getName() + "-editPhSensor").class_("col-md-12"))
            .form(id(phSensor.getName() + "-editPhSensor")
                    .name(phSensor.getName() + "-edit"))
                .input(type("text").class_("form-control m-t")
                        .name("name").id("name")
                        .value(phSensor.getName()));
        html.select(class_("form-control m-t").name("select_input")
                .id("select_input").onChange("selectedInput(this);").onLoad("selectedInput(this);"));

        html.option(value("ain")
                    .selected_if(phSensor.getAIN().length() > 0))
                    .write("Onboard AIN")
                    ._option();
        if (LaunchControl.useOWFS) {
            html.option(value("ds2450")
                    .selected_if(phSensor.getDsAddress().length() > 0))
                    .write("DS2450")
                    ._option();
        }
            html.option(value("i2c")
                    .selected_if(phSensor.getI2CDevAddressString().length() > 0))
                    .write("I2C Input")
                    ._option();
        html._select();

        String styleString;
        if (phSensor.getAIN().length() > 0)
        {
            styleString = "display: block";
        }
        else
        {
            styleString = "display: none";
        }
        html.div(id("ain_div").name("ain_div").style(styleString));

                html.input(type("text").class_("form-control m-t")
                        .name("adc_pin").onInput("phAINChange(this);")
                        .add("pattern", "[0-7]{1}")
                        .title("Only 0-7 are accepted pins.")
                        .id("adc_pin").value(phSensor.getAIN())
                        .add("placeholder", Messages.ANALOGUE_PIN));
        html._div();
                // Create a list of the DS2450 addresses
        if (phSensor.getDsAddress().length() > 0)
        {
            styleString = "display: block";
        }
        else
        {
            styleString = "display: none";
        }
        if (LaunchControl.useOWFS) {
            html.div(id("ds_div").name("ds_div").style(styleString));

            html.select(class_("form-control m-t").name("dsAddress")
                    .id("dsAddress").onClick("selectPhAddress(this);"));
            html.option(value("").selected_if(
                    "".equals(phSensor.getDsAddress())))
                    .write(Messages.DS2450_ADDRESS)
                    ._option();
            for (String addr : LaunchControl.getOneWireDevices("/20")) {
                String address = addr.substring(1);
                html.option(value(address)
                        .selected_if(address.equals(phSensor.getDsAddress())))
                        .write(address)
                        ._option();
            }

            html._select();
            html.input(type("text").class_("form-control m-t")
                    .name("dsOffset")
                    .id("dsOffset").value(phSensor.getDsOffset())
                    .add("pattern", "[ABCD]{1}")
                    .title("Only A, B, C, or D are accepted offsets")
                    .add("placeholder", Messages.DS2450_OFFSET));
            html._div();
        }
        // I2C Stuff
        if (phSensor.getI2CDevAddressString().length() > 0) {
            styleString = "display: block";
        }
        else
        {
            styleString = "display: none";
        }
        html.div(id("i2c_div").name("i2c_div").style(styleString));
                html.select(class_("form-control m-t")
                                .name("i2c_device").id("i2c_device"));

                html.option(value("").selected_if(phSensor.getI2CDevicePath().equals("")))
                        .write(Messages.I2C_DEVICE_NUMBER)._option();

                for (String device: I2CDevice.getAvailableDevices())
                {
                    html.option(value(device)
                            .selected_if(phSensor.getI2CDevicePath().endsWith(device)))
                            .write(device)
                            ._option();
                }
                html._select();
                html.input(type("text").class_("form-control m-t")
                                .name("i2c_address").id("i2c_address")
                                .value(phSensor.getI2CDevAddressString())
                                .add("placeholder", Messages.I2C_DEVICE_ADDRESS)
                );
                html.input(type("text").class_("form-control m-t")
                                .name("i2c_channel").id("i2c_channel")
                                .value(phSensor.geti2cChannel())
                                .add("placeholder", Messages.I2C_DEVICE_CHANNEL)
                );
                html.select(class_("form-control m-t").name("i2c_model")
                        .id("i2c_model"));
                html.option(value("").selected_if(
                        "".equals(phSensor.getI2CDevType())))
                        .write(Messages.I2C_MODEL)
                        ._option();
                for (String model: I2CDevice.getAvailableTypes()) {
                    html.option(value(model)
                            .selected_if(model.equals(phSensor.getI2CDevType())))
                            .write(model)
                            ._option();
                }

                html._select();
        html._div();
                html.select(class_("form-control m-t").name("ph_model")
                        .id("ph_model"));
                html.option(value("").selected_if(
                        "".equals(phSensor.getDsAddress())))
                    .write(Messages.PH_MODEL)
                ._option();
                for (String model: phSensor.getAvailableTypes()) {
                    html.option(value(model)
                        .selected_if(model.equals(phSensor.getModel())))
                        .write(model)
                    ._option();
                }

                html._select();
                html.input(type("number").class_("form-control  m-t")
                        .add("step", "any")
                        .name("calibration")
                        .id("calibration").value("")
                        .add("placeholder", Messages.CALIBRATE));
                html.button(id("updatePhSensor-" + phSensor.getName())
                        .onClick("submitForm(this.form);")
                        .class_("btn"))
                    .write(Messages.UPDATE)
                ._button()
                .button(id("cancelPh-" + phSensor.getName())
                        .onClick("cancelPhEdit(" + phSensor.getName() + ");")
                        .class_("btn"))
                    .write(Messages.CANCEL)
                ._button()
            ._form()
        ._div();
    }

}
