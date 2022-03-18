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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
@ConditionalOnProperty(name = "scheduler.enable", havingValue = "true")
public class JobLauncherScheduler {
    private static final String RUN_BINDING = "run-task-auto";
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherScheduler.class);
    private static final String PATTERN_DATE_FORMAT = "yyyy-MM-dd";

    private final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern(PATTERN_DATE_FORMAT);
    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final Logger jobLauncherEventsLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final StreamBridge streamBridge;

    public JobLauncherScheduler(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, Logger jobLauncherEventsLogger, RestTemplateBuilder restTemplateBuilder, StreamBridge streamBridge) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.streamBridge = streamBridge;
    }

    @Scheduled(cron = "0 */${scheduler.frequency-in-minutes} ${scheduler.start-hour}-${scheduler.end-hour} * * *")
    public void automaticTaskStart() {
        OffsetDateTime startOfDayTimestamp = getstartingDate();

        String requestUrl = jobLauncherConfigurationProperties.getUrl().getTaskManagerBusinessDateUrl() + timestampFormat.format(startOfDayTimestamp);
        LOGGER.info("Requesting URL: {}", requestUrl);

        try {
            ResponseEntity<TaskDto[]> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto[].class);

            if (responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                for (TaskDto task : responseEntity.getBody()) {

                    String taskId = task.getId().toString();
                    // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
                    // This should be done only once, as soon as the information to add in mdc is available.
                    MDC.put("gridcapa-task-id", taskId);

                    if (task.getStatus() == TaskStatus.READY) {
                        jobLauncherEventsLogger.info("Task launched on TS {}", task.getTimestamp());
                        streamBridge.send(RUN_BINDING, Objects.requireNonNull(task));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error during automatic launch", e);
        }
    }

    private OffsetDateTime getstartingDate() {
        return OffsetDateTime.now(ZoneId.of(jobLauncherConfigurationProperties.getProcess().getTimezone())).plusDays(findDaysToAdd());
    }

    private int findDaysToAdd() {
        if ("core_valid".equalsIgnoreCase(jobLauncherConfigurationProperties.getProcess().getTag())) {
            return 1;
        }
        return 0;
    }
}
