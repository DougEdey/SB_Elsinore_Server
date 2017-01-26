package com.sb.elsinore.controller;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.PID;
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
public class PIDController {
    private static final Logger logger = Logger.getLogger(TemperatureController.class.getCanonicalName());

    private PIDRepository pidRepository;

    @Autowired
    public PIDController(PIDRepository pidRepository) {
        logger.warning("Started PID controller");
        this.pidRepository = pidRepository;
    }

    @RequestMapping(value = "/pids", method = RequestMethod.POST)
    @ResponseBody
    public void create(@RequestBody PID pid, Model model) {
        logger.info("Created " + pid.getName());
        this.pidRepository.save(pid);

        if (StreamSupport.stream(this.pidRepository.findAll().spliterator(), true)
                .noneMatch(p -> p.getName().equals(pid.getName()))) {
            LaunchControl.getInstance().addTemp(pid);
        }
    }

    @RequestMapping(value = "/pids/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void delete(@PathVariable Long id) {
        Temp temp = this.pidRepository.findOne(id);
        if (temp != null) {
            this.pidRepository.delete(id);
            LaunchControl.getInstance().deleteTemp(temp);
        }
    }

}
