/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobLauncherController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherController.class);

    private final RestTemplateBuilder restTemplateBuilder;
    private final JobLauncherService jobLauncherService;
    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;

    public JobLauncherController(RestTemplateBuilder restTemplateBuilder,
                                 JobLauncherService jobLauncherService,
                                 JobLauncherConfigurationProperties jobLauncherConfigurationProperties) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.jobLauncherService = jobLauncherService;
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
    }

    @PostMapping(value = "/start/{timestamp}")
    public ResponseEntity<Void> launchJob(@PathVariable String timestamp) {
        LOGGER.info("Received order to launch task {}", timestamp);
        String requestUrl = jobLauncherConfigurationProperties.getTaskManagerTimestampUrl() + timestamp;
        LOGGER.info("Requesting URL: {}", requestUrl);
        ResponseEntity<TaskDto> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto.class);
        TaskDto taskDto = responseEntity.getBody();
        if (taskDto != null) {
            String taskId = taskDto.getId().toString();
            // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", taskId);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                jobLauncherService.launchJob(taskDto);
                return ResponseEntity.ok().build();
            } else {
                return getEmptyResponseEntity(timestamp);
            }
        } else {
            return getEmptyResponseEntity(timestamp);
        }
    }

    private ResponseEntity<Void> getEmptyResponseEntity(@PathVariable String timestamp) {
        LOGGER.error("Failed to retrieve task with timestamp {}", timestamp);
        return ResponseEntity.notFound().build();
    }

}
