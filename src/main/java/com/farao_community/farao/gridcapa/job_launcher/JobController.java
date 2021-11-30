package com.farao_community.farao.gridcapa.job_launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobController {

    private RestTemplateBuilder restTemplateBuilder;

    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

    public JobController(RestTemplateBuilder restTemplateBuilder, JobLauncherConfigurationProperties jobLauncherConfigurationProperties) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
    }

    @PostMapping(value = "/start/{id}")
    public void launchJob(@PathVariable String id) {
        LOGGER.info("Received order to launch task {}", id);
        String url = jobLauncherConfigurationProperties.getTaskManagerUrlProperties().getTaskManagerUrl() + "tasks/launch/" + id;
        LOGGER.info("Requesting URL: " + url);
        RestTemplate restTemplate = restTemplateBuilder.build();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, null, String.class);
        // Code = 200.
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            //TaskDto taskDto = responseEntity.getBody(); //TODO make this work
            LOGGER.info("Task launched " +  responseEntity.getBody());
        }
    }

}
