
package com.sb.elsinore.controller;


import com.sb.elsinore.Device;
import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.Temp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;
import java.util.stream.StreamSupport;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestController
public class DeviceController {
    private static final Logger logger = Logger.getLogger(DeviceController.class.getCanonicalName());

    private final DeviceRepository deviceRepository;

    @Autowired
    public DeviceController(DeviceRepository deviceRepository) {
        logger.warning("Foo");
        this.deviceRepository = deviceRepository;
    }

    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public void create(@RequestBody Device device, Model model) {
        logger.info("Created");
        this.deviceRepository.save(device);
        if (StreamSupport.stream(this.deviceRepository.findAll().spliterator(), true).filter(d -> d instanceof Temp)
                .noneMatch(d -> d.equals(device))) {
            if (device instanceof Temp) {
                LaunchControl.getInstance().addTemp((Temp) device);
            }
        }
    }

    @RequestMapping(value = "/devices/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void delete(@PathVariable Long id) {
        Device device = this.deviceRepository.findOne(id);
        if (device != null) {
            this.deviceRepository.delete(id);
            if (device instanceof Temp) {
                LaunchControl.getInstance().deleteTemp((Temp) device);
            }
        }
    }
}
