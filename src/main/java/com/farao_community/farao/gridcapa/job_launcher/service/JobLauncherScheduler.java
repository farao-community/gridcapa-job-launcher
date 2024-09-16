/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.job_launcher.JobLauncherConfigurationProperties;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
@ConditionalOnProperty(name = "scheduler.enable", havingValue = "true")
public class JobLauncherScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherScheduler.class);
    private static final String RUN_BINDING = "run-task-auto";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final JobLauncherCommonService jobLauncherCommonService;
    private final TaskManagerService taskManagerService;

    public JobLauncherScheduler(JobLauncherConfigurationProperties jobLauncherConfigurationProperties,
                                JobLauncherCommonService jobLauncherCommonService,
                                TaskManagerService taskManagerService) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.jobLauncherCommonService = jobLauncherCommonService;
        this.taskManagerService = taskManagerService;
    }

    @Scheduled(cron = "0 */${scheduler.frequency-in-minutes} ${scheduler.start-hour}-${scheduler.end-hour} * * *")
    void automaticTaskStart() {
        final String startingDate = getStartingDate();
        final Optional<TaskDto[]> taskDtosForBusinessDateOpt = taskManagerService.getTasksFromBusinessDate(startingDate);
        if (taskDtosForBusinessDateOpt.isPresent()) {
            for (TaskDto taskDto : taskDtosForBusinessDateOpt.get()) {
                // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
                // This should be done only once, as soon as the information to add in mdc is available.
                MDC.put("gridcapa-task-id", taskDto.getId().toString());

                if (taskDto.getStatus() == TaskStatus.READY) {
                    jobLauncherCommonService.launchJob(taskDto, RUN_BINDING);
                }
            }
        } else {
            LOGGER.error("Failed to launch tasks for date {}: could not retrieve tasks from the task-manager", startingDate);
        }
    }

    String getStartingDate() {
        final OffsetDateTime startingDateTime = OffsetDateTime.now(ZoneId.of(jobLauncherConfigurationProperties.process().timezone()))
                .plusDays(jobLauncherConfigurationProperties.process().daysToAdd());
        return TIMESTAMP_FORMAT.format(startingDateTime);
    }
}
