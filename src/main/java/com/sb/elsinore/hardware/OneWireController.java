package com.sb.elsinore.hardware;

import com.sb.common.Constants;
import com.sb.common.Result;
import com.sb.common.TemperatureResult;
import com.sb.elsinore.OWFSController;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.interfaces.TemperatureInterface;
import com.sb.elsinore.models.SystemSettings;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.elsinore.repositories.SystemSettingsRepository;
import com.sb.util.MathUtil;
import org.apache.commons.lang3.StringUtils;
import org.owfs.jowfsclient.OwfsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sb.common.Constants.ERROR_TEMP;

/**
 * This class bundles up access to one wire devices, using OWFS or native access as prescribed
 */
@Component
@Singleton
public class OneWireController {
    private final SystemSettings systemSettings;
    private OWFSController owfsController;

    private Logger logger = LoggerFactory.getLogger(OneWireController.class);

    @Autowired
    public OneWireController(SystemSettingsRepository systemSettingsRepository) {
        this.systemSettings = systemSettingsRepository.findAll().get(0);
    }

    public boolean useOWFS() {
        return this.systemSettings != null && this.systemSettings.isOwfsEnabled();
    }

    public List<TempProbe> getAvailableDevices(DeviceType deviceType) {
        if (useOWFS()) {
            return owfsController.getOneWireDevices(deviceType.getPrefix());
        }
        return getOneWireDevices(deviceType);
    }

    private List<TempProbe> getOneWireDevices(DeviceType deviceType) {
        File w1_device_dir = new File("/sys/bus/w1/devices/");
        List<TempProbe> result = new ArrayList<>();
        TemperatureInterface temperatureInterface = new TemperatureModel();
        temperatureInterface.setName(Constants.SYSTEM);
        temperatureInterface.setDevice(Constants.SYSTEM);

        result.add(new TempProbe(temperatureInterface));

        if (w1_device_dir.exists()) {
            String[] matchedFiles = w1_device_dir.list(new DeviceFileNameFilter(deviceType));
            if (matchedFiles != null) {
                for(String file: matchedFiles) {
                    temperatureInterface = new TemperatureModel();
                    temperatureInterface.setName(file);
                    temperatureInterface.setDevice(file);

                    result.add(new TempProbe(temperatureInterface));
                }

            }
        }
        return result;
    }

    public TemperatureResult updateTempFromDevice(String deviceAddress) {
        if (useOWFS()) {
            return updateTempFromOWFS(deviceAddress);
        }
        String fileProbe = String.format("/sys/bus/w1/devices/%s/w1_slave", deviceAddress);
        File probePath = new File(fileProbe);

        // Lets assume that OWFS has "." separated names
        if (!probePath.exists() && deviceAddress.contains(".")) {
            String[] newAddress = deviceAddress.split("[.\\-]");

            if (newAddress.length == 2) {
                String devFamily = newAddress[0];
                String devAddress = "";
                // Byte swap!
                devAddress += newAddress[1].subSequence(10, 12);
                devAddress += newAddress[1].subSequence(8, 10);
                devAddress += newAddress[1].subSequence(6, 8);
                devAddress += newAddress[1].subSequence(4, 6);
                devAddress += newAddress[1].subSequence(2, 4);
                devAddress += newAddress[1].subSequence(0, 2);

                String fixedAddress = devFamily + "-" + devAddress.toLowerCase();

                this.logger.info("Converted address: " + fixedAddress);

                fileProbe = String.format("/sys/bus/w1/devices/%s/w1_slave", fixedAddress);
                probePath = new File(fileProbe);

                if (!probePath.exists()) {
                    probePath = null;
                }
            }
        }

        return updateTempFromFile(probePath);
    }

    /**
     * @return Get the current temperature from the OWFS server
     */
    public TemperatureResult updateTempFromOWFS(String deviceAddress) {
        TemperatureResult result = new TemperatureResult();
        // Use the OWFS connection
        if (isNullOrEmpty(deviceAddress) || "Blank".equals(deviceAddress)) {
            result.temperature = BigDecimal.ZERO;
            return result;
        }

        BigDecimal temp = ERROR_TEMP;
        String rawTemp = "";


        try {
            rawTemp = this.owfsController.readOWFSPath(deviceAddress + "/temperature");
            if (rawTemp.equals("")) {
                this.logger.error("Couldn't find the probe {}", deviceAddress);
                this.owfsController.setupOWFS();
                result.result = Result.ERROR;
            } else {
                temp = new BigDecimal(rawTemp);
            }
        } catch (IOException e) {
            result.errorMessage = String.format("Couldn't read %s", deviceAddress);
            result.result = Result.ERROR;
            this.logger.error(result.errorMessage, e);
        } catch (OwfsException e) {
            result.errorMessage = String.format("Couldn't read %s", deviceAddress);
            result.result = Result.ERROR;
            this.logger.error(result.errorMessage, e);
            this.owfsController.setupOWFS();
        } catch (NumberFormatException e) {
            result.errorMessage = String.format("Couldn't parse %s", rawTemp);
            result.result = Result.ERROR;
            this.logger.error(result.errorMessage, e);
        }

        result.temperature = temp;
        return result;
    }

    public TemperatureResult updateTempFromPath(String path) {
        TemperatureResult result = new TemperatureResult();
        if (StringUtils.isEmpty(path)) {
            this.logger.warn("No File to probe");
            result.temperature = BigDecimal.ZERO;
        } else {
            result = updateTempFromFile(new File(path));
        }

        return result;
    }
    public TemperatureResult updateTempFromFile(File file) {
        TemperatureResult result = new TemperatureResult();


        BufferedReader br = null;
        String temp = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            if (line == null || line.contains("NO")) {
                // bad CRC, do nothing
                result.errorMessage = String.format("Bad CRC from %s", file.getAbsoluteFile());
            } else if (line.contains("YES")) {
                // good CRC
                line = br.readLine();
                // last value should be t=
                int t = line.indexOf("t=");
                temp = line.substring(t + 2);
                BigDecimal tTemp = new BigDecimal(temp);
                result.temperature = MathUtil.divide(tTemp, 1000);
            } else {
                // System TemperatureModel
                BigDecimal tTemp = new BigDecimal(line);
                result.temperature = MathUtil.divide(tTemp, 1000);
            }

        } catch (IOException ie) {
            result.setErrorMessage(String.format("Couldn't find the device under: %s", file.getAbsolutePath()));
            this.logger.warn(result.errorMessage);
        } catch (NumberFormatException nfe) {
            result.setErrorMessage(String.format("Couldn't parse %s as a double", temp));
            this.logger.error(result.errorMessage, nfe);
        } catch (Exception e) {
            result.setErrorMessage("Couldn't update temperature from file");
            this.logger.error(result.errorMessage, e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ie) {
                    this.logger.warn(ie.getLocalizedMessage());
                }
            }
        }
        return result;
    }

}
