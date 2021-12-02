/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobController {
    private static final String RUN_BINDING = "run-task";

    private final RestTemplateBuilder restTemplateBuilder;
    private final StreamBridge streamBridge;

    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

    public JobController(RestTemplateBuilder restTemplateBuilder, StreamBridge streamBridge, JobLauncherConfigurationProperties jobLauncherConfigurationProperties) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.streamBridge = streamBridge;
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
    }

    @PostMapping(value = "/start/{timestamp}")
    public void launchJob(@PathVariable String timestamp) {
        LOGGER.info("Received order to launch task {}", timestamp);
        String url = jobLauncherConfigurationProperties.getTaskManagerUrlProperties().getTaskManagerUrl() + timestamp;
        LOGGER.info("Requesting URL: {}", url);
        RestTemplate restTemplate = restTemplateBuilder.build();
        ResponseEntity<TaskDto> responseEntity = restTemplate.getForEntity(url, TaskDto.class);
        TaskDto taskDto = responseEntity.getBody();
        // Code = 200.
        if (responseEntity.getStatusCode() == HttpStatus.OK && taskDto != null) {
            if (taskDto.getStatus() == TaskStatus.READY) {
                LOGGER.info("Task launched on TS {}", taskDto.getTimestamp());
                streamBridge.send(RUN_BINDING, Objects.requireNonNull(responseEntity.getBody()));
            } else {
                LOGGER.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
        } else {
            LOGGER.error("Failed to retrieve task with timestamp {}", timestamp);
        }
    }
}
