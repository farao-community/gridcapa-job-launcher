package com.faraocommunity.farao.gridcapajoblauncher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobController {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

    @Value("${task.manager.url")
    private String taskManagerUrl;

    @RequestMapping(value = "/jobs/{id}", method = RequestMethod.POST)
    public void launchJob(@PathVariable int id) {
        LOGGER.info("Received order to launch task n°" + id);
    }
}
