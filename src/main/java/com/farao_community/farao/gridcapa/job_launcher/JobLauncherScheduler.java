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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
    private final RestTemplateBuilder restTemplateBuilder;
    private final JobLauncherCommonService jobLauncherCommonService;

    public JobLauncherScheduler(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, RestTemplateBuilder restTemplateBuilder, JobLauncherCommonService jobLauncherCommonService) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
        this.jobLauncherCommonService = jobLauncherCommonService;
    }

    @Scheduled(cron = "0 */${scheduler.frequency-in-minutes} ${scheduler.start-hour}-${scheduler.end-hour} * * *")
    void automaticTaskStart() {
        OffsetDateTime startOfDayTimestamp = getStartingDate();
        String requestUrl = jobLauncherConfigurationProperties.getUrl().taskManagerBusinessDateUrl() + timestampFormat.format(startOfDayTimestamp);
        LOGGER.info("Requesting URL: {}", requestUrl);

        try {
            ResponseEntity<TaskDto[]> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto[].class);

            if (responseEntity.getBody() != null && responseEntity.getStatusCode() == HttpStatus.OK) {
                for (TaskDto taskDto : responseEntity.getBody()) {
                    // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
                    // This should be done only once, as soon as the information to add in mdc is available.
                    MDC.put("gridcapa-task-id", taskDto.getId().toString());

                    if (taskDto.getStatus() == TaskStatus.READY) {
                        jobLauncherCommonService.launchJob(taskDto, RUN_BINDING);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error during automatic launch", e);
        }
    }

    private OffsetDateTime getStartingDate() {
        return OffsetDateTime.now(ZoneId.of(jobLauncherConfigurationProperties.getProcess().timezone()))
                .plusDays(jobLauncherConfigurationProperties.getProcess().daysToAdd());
    }
}
