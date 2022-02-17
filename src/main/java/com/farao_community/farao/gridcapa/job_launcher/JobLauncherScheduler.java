/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherScheduler.class);
    private static final String PATTERN_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final ZoneId EUROPE_BRUSSELS_ZONE_ID = ZoneId.of("Europe/Brussels");

    private final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern(PATTERN_DATE_FORMAT);
    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final JobLauncherService jobLauncherService;
    private final RestTemplateBuilder restTemplateBuilder;

    public JobLauncherScheduler(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, JobLauncherService jobLauncherService, RestTemplateBuilder restTemplateBuilder) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.jobLauncherService = jobLauncherService;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Scheduled(cron = "${scheduler.cronjob}")
    public void automaticTaskStart() {
        OffsetDateTime startOfDayTimestamp = getstartingTimestamp();
        OffsetDateTime endOfDayTimestamp = startOfDayTimestamp.plusDays(1);

        while (startOfDayTimestamp.isBefore(endOfDayTimestamp)) {
            String requestUrl = jobLauncherConfigurationProperties.getTaskManagerUrlProperties().getTaskManagerUrl() + timestampFormat.format(startOfDayTimestamp);
            LOGGER.info("Requesting URL: {}", requestUrl);

            try {
                ResponseEntity<TaskDto> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto.class);
                TaskDto taskDto = responseEntity.getBody();

                if (taskDto != null) {
                    LOGGER.info("status " + taskDto.getStatus().toString());
                    LOGGER.info("status " + taskDto.getProcessFiles().toString());
                    String taskId = taskDto.getId().toString();
                    // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
                    // This should be done only once, as soon as the information to add in mdc is available.
                    MDC.put("gridcapa-task-id", taskId);

                    if (responseEntity.getStatusCode() == HttpStatus.OK) {
                        jobLauncherService.launchJob(taskDto);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Exception " + e.getMessage());
            } finally {
                startOfDayTimestamp = startOfDayTimestamp.plusHours(1);
            }
        }
    }

    private OffsetDateTime getstartingTimestamp() {
        return OffsetDateTime.now().plusDays(1).withHour(0).withMinute(30).atZoneSameInstant(EUROPE_BRUSSELS_ZONE_ID).toOffsetDateTime();
    }
}
