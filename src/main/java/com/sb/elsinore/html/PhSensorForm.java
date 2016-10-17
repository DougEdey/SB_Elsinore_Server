package com.sb.elsinore.html;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Messages;
import com.sb.elsinore.devices.I2CDevice;
import com.sb.elsinore.inputs.PhSensor;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.util.ArrayList;

import static com.sb.elsinore.UrlEndpoints.*;
import static org.rendersnake.HtmlAttributesFactory.*;

public class PhSensorForm implements Renderable {

    public static final String AIN = "ain";
    public static final String DS_2450 = "ds2450";
    public static final String I_2_C = "i2c";
    private final PhSensor phSensor;

    public PhSensorForm(PhSensor newSensor) {
        this.phSensor = newSensor;
    }

    @Override
    public void renderOn(final HtmlCanvas html) throws IOException {
        html.div(id(this.phSensor.getName() + "-editPhSensor").class_("col-md-12"))
                .form(id(this.phSensor.getName() + "-editPhSensor")
                        .name(this.phSensor.getName() + "-edit"))
                .input(type("text").class_("form-control m-t")
                        .name(NAME).id(NAME)
                        .value(this.phSensor.getName()));
        html.select(class_("form-control m-t").name("select_input")
                .id("select_input").onChange("selectedInput(this);").onLoad("selectedInput(this);"));

        html.option(value(AIN)
                .selected_if(this.phSensor.getAIN().length() > 0))
                .write("Onboard AIN")
                ._option();
        if (LaunchControl.getInstance().getOWFS() != null) {
            html.option(value(DS_2450)
                    .selected_if(this.phSensor.getDsAddress().length() > 0))
                    .write("DS2450")
                    ._option();
        }
        html.option(value(I_2_C)
                .selected_if(this.phSensor.getI2CDevAddressString().length() > 0))
                .write("I2C Input")
                ._option();
        html._select();

        String styleString;
        if (this.phSensor.getAIN().length() > 0) {
            styleString = "display: block";
        } else {
            styleString = "display: none";
        }
        html.div(id("ain_div").name("ain_div").style(styleString));

        html.input(type("text").class_("form-control m-t")
                .name(ADC_PIN).onInput("phAINChange(this);")
                .add("pattern", "[0-7]{1}")
                .title("Only 0-7 are accepted pins.")
                .id(ADC_PIN).value(this.phSensor.getAIN())
                .add("placeholder", Messages.ANALOGUE_PIN));
        html._div();
        // Create a list of the DS2450 addresses
        if (this.phSensor.getDsAddress().length() > 0) {
            styleString = "display: block";
        } else {
            styleString = "display: none";
        }
        if (LaunchControl.getInstance().getOWFS() != null) {
            html.div(id("ds_div").name("ds_div").style(styleString));

            html.select(class_("form-control m-t").name("dsAddress")
                    .id("dsAddress").onClick("selectPhAddress(this);"));
            html.option(value("").selected_if(
                    "".equals(this.phSensor.getDsAddress())))
                    .write(Messages.DS2450_ADDRESS)
                    ._option();
            for (String addr : LaunchControl.getInstance().getOneWireDevices("/20")) {
                String address = addr.substring(1);
                html.option(value(address)
                        .selected_if(address.equals(this.phSensor.getDsAddress())))
                        .write(address)
                        ._option();
            }

            html._select();
            html.input(type("text").class_("form-control m-t")
                    .name(DS_OFFSET).id(DS_OFFSET)
                    .value(this.phSensor.getDsOffset())
                    .add("pattern", "[ABCD]{1}")
                    .title("Only A, B, C, or D are accepted offsets")
                    .add("placeholder", Messages.DS2450_OFFSET));
            html._div();
        }
        // I2C Stuff
        if (this.phSensor.getI2CDevAddressString().length() > 0) {
            styleString = "display: block";
        } else {
            styleString = "display: none";
        }
        html.div(id("i2c_div").name("i2c_div").style(styleString));
        html.select(class_("form-control m-t")
                .onChange("selectedI2C(this);").onLoad("selectedI2C(this);")
                .name(I_2_C_DEVICE).id(I_2_C_DEVICE));

        html.option(value("").selected_if(this.phSensor.getI2CDevicePath().equals("")))
                .write(Messages.I2C_DEVICE_NUMBER)._option();

        for (String device : I2CDevice.getAvailableDevices()) {
            html.option(value(device)
                    .selected_if(this.phSensor.getI2CDevicePath().endsWith(device)))
                    .write(device)
                    ._option();
        }
        html._select();
        int i2cSensorAddr = Integer.parseInt(this.phSensor.getI2CDevAddressString());
        for (String device : I2CDevice.getAvailableDevices()) {
            String devStyle = "display: none";
            if (this.phSensor.getI2CDevicePath().endsWith(device)) {
                devStyle = "display: block";
            }
            html.select(class_("form-control m-t ").name(device).id(device).style(devStyle));
            html.option(value("").selected_if(this.phSensor.getI2CDevicePath().equals("")))
                    .write(Messages.I2C_DEVICE_ADDRESS)._option();
            ArrayList<String> devAddrs = I2CDevice.getAvailableAddresses(device);
            if (devAddrs.size() == 0) {
                html.option(value(""))
                        .write("No Devices detected")._option();
            } else {
                for (String addr : devAddrs) {
                    int addrInt = Integer.decode(addr);
                    html.option(value(addr).selected_if(addrInt == i2cSensorAddr))
                            .write(addr)._option();
                }
            }
            html._select();
        }

        html.input(type("text").class_("form-control m-t")
                .name(I_2_C_CHANNEL).id(I_2_C_CHANNEL)
                .value(this.phSensor.geti2cChannel())
                .add("placeholder", Messages.I2C_DEVICE_CHANNEL)
        );
        html.select(class_("form-control m-t").name(I_2_C_MODEL)
                .id(I_2_C_MODEL));
        html.option(value("").selected_if(
                "".equals(this.phSensor.getI2CDevType())))
                .write(Messages.I2C_MODEL)
                ._option();
        for (String model : I2CDevice.getAvailableTypes()) {
            html.option(value(model)
                    .selected_if(model.equals(this.phSensor.getI2CDevType())))
                    .write(model)
                    ._option();
        }

        html._select();
        html._div();
        html.select(class_("form-control m-t").name("ph_model")
                .id("ph_model"));
        html.option(value("").selected_if(
                "".equals(this.phSensor.getDsAddress())))
                .write(Messages.PH_MODEL)
                ._option();
        for (String model : this.phSensor.getAvailableTypes()) {
            html.option(value(model)
                    .selected_if(model.equals(this.phSensor.getModel())))
                    .write(model)
                    ._option();
        }

        html._select();
        html.input(type("number").class_("form-control  m-t")
                .add("step", "any")
                .name(CALIBRATION)
                .id(CALIBRATION).value("")
                .add("placeholder", Messages.CALIBRATE));
        html.button(id("updatePhSensor-" + this.phSensor.getName())
                .onClick("submitForm(this.form);")
                .class_("btn"))
                .write(Messages.UPDATE)
                ._button()
                .button(id("cancelPh-" + this.phSensor.getName())
                        .onClick("cancelPhEdit(" + this.phSensor.getName() + ");")
                        .class_("btn"))
                .write(Messages.CANCEL)
                ._button()
                ._form()
                ._div();
    }

}
