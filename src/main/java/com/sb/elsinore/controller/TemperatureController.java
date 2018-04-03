package com.sb.elsinore.controller;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.devices.TempProbe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestController
public class TemperatureController {
    private static final Logger logger = Logger.getLogger(TemperatureController.class.getCanonicalName());

    private TemperatureRepository temperatureRepository;

    @Autowired
    public TemperatureController(TemperatureRepository temperatureRepository) {
        logger.warning("Started Temperature controller");
        this.temperatureRepository = temperatureRepository;
    }


    @RequestMapping(value = "/temperatures", method = RequestMethod.POST)
    @ResponseBody
    public void create(@RequestBody TempProbe tempProbe, Model model) {
        logger.info("Created " + tempProbe.getName());
        this.temperatureRepository.save(tempProbe);
        if (StreamSupport.stream(this.temperatureRepository.findAll().spliterator(), true)
                .anyMatch(t -> t.getName().equals(tempProbe.getName()))) {
            LaunchControl.getInstance().addTemp(tempProbe);
        }
    }

    @RequestMapping(value = "/temperatures/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void delete(@PathVariable Long id) {
        TempProbe tempProbe = this.temperatureRepository.findOne(id);
        if (tempProbe != null) {
            this.temperatureRepository.delete(id);
            LaunchControl.getInstance().deleteTemp(tempProbe);
        }
    }

    @RequestMapping(value = "addresses", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getAddresses() {
        ArrayList<String> addresses = new ArrayList<>();

        addresses.addAll(LaunchControl.getInstance().getOneWireDevices("28"));
        addresses.add("System");

        Iterable<TempProbe> tempIterator = this.temperatureRepository.findAll();
        tempIterator.forEach(t -> addresses.remove(t.getDevice()));
        
        return addresses;
    }

}
