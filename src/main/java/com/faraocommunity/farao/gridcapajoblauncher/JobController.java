package com.faraocommunity.farao.gridcapajoblauncher;

import org.apache.commons.httpclient.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobController {

    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

    public JobController(JobLauncherConfigurationProperties jobLauncherConfigurationProperties) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
    }

    @PostMapping(value = "/start/{id}")
    public void launchJob(@PathVariable String id) {
        LOGGER.info("Received order to launch task {}", id);
    }

}
