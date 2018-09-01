package com.sb.elsinore.controller;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.interfaces.TemperatureInterface;
import com.sb.elsinore.models.TemperatureModel;
import com.sb.elsinore.repositories.TemperatureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Created by doug on 25/01/17.
 */
@RepositoryRestController
public class TemperatureController {
    private static final Logger logger = Logger.getLogger(TemperatureController.class.getCanonicalName());

    private TemperatureRepository temperatureRepository;

    @Autowired
    public TemperatureController(TemperatureRepository temperatureRepository) {
        logger.warning("Started TemperatureModel controller");
        this.temperatureRepository = temperatureRepository;
    }


    @RequestMapping(value = "/temperatures", method = RequestMethod.POST)
    @ResponseBody
    public void create(@RequestBody TemperatureInterface temperatureInterface, Model model) {
        logger.info("Created " + temperatureInterface.getName());
        this.temperatureRepository.save(temperatureInterface.getModel());
        if (this.temperatureRepository.findAll().parallelStream()
                .anyMatch(t -> t.getName().equals(temperatureInterface.getName()))) {
            LaunchControl.getInstance().addTemp(temperatureInterface);
        }
    }

    @RequestMapping(value = "/temperatures/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void delete(@PathVariable Long id) {
        Optional<TemperatureModel> tempProbe = this.temperatureRepository.findById(id);
        if (tempProbe.isPresent()) {
            this.temperatureRepository.deleteById(id);
            LaunchControl.getInstance().deleteTemp(tempProbe.get());
        }
    }

    @RequestMapping(value = "addresses", method = RequestMethod.GET)
    @ResponseBody
    public List<String> getAddresses() {

        ArrayList<String> addresses = new ArrayList<>(LaunchControl.getInstance().getOneWireDevices("28"));
        addresses.add("System");

        Iterable<TemperatureModel> tempIterator = this.temperatureRepository.findAll();
        tempIterator.forEach(t -> addresses.remove(t.getDevice()));

        return addresses;
    }

}
