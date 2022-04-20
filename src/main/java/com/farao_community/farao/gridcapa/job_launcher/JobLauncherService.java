/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class JobLauncherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherService.class);
    private static final String RUN_BINDING = "run-task";

    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final StreamBridge streamBridge;
    private final Logger jobLauncherEventsLogger;

    public JobLauncherService(JobLauncherConfigurationProperties jobLauncherConfigurationProperties,
                              RestTemplateBuilder restTemplateBuilder, StreamBridge streamBridge,
                              Logger jobLauncherEventsLogger) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
        this.streamBridge = streamBridge;
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
    }

    public boolean launchJob(String timestamp) {
        LOGGER.info("Received order to launch task {}", timestamp);
        String requestUrl = getUrlToRetrieveTaskDto(timestamp);
        LOGGER.info("Requesting URL: {}", requestUrl);
        ResponseEntity<TaskDto> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto.class);
        TaskDto taskDto = responseEntity.getBody();
        if (taskDto != null) {
            String taskId = taskDto.getId().toString();
            // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", taskId);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                restTemplateBuilder.build().put(getUrlToUpdateTaskStatusToPending(timestamp), TaskDto.class);
                if (taskDto.getStatus() == TaskStatus.READY
                    || taskDto.getStatus() == TaskStatus.SUCCESS
                    || taskDto.getStatus() == TaskStatus.ERROR) {
                    jobLauncherEventsLogger.info("Task launched on TS {}", taskDto.getTimestamp());
                    streamBridge.send(RUN_BINDING, Objects.requireNonNull(taskDto));
                } else {
                    jobLauncherEventsLogger.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
                }
                return true;
            }
        }
        return false;
    }

    String getUrlToRetrieveTaskDto(String timestamp) {
        return jobLauncherConfigurationProperties.getUrl().getTaskManagerTimestampUrl() + timestamp;
    }

    String getUrlToUpdateTaskStatusToPending(String timestamp) {
        String url = jobLauncherConfigurationProperties.getUrl().getTaskManagerTimestampUrl() + timestamp + "/status";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
            .queryParam("status", TaskStatus.PENDING);
        return builder.toUriString();
    }
}
