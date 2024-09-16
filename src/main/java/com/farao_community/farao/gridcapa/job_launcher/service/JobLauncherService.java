/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.job_launcher.util.LoggingUtil;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class JobLauncherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherService.class);
    private static final String RUN_BINDING = "run-task";
    private static final String STOP_BINDING = "stop-task";

    private final JobLauncherCommonService jobLauncherCommonService;
    private final Logger jobLauncherEventsLogger;
    private final TaskManagerService taskManagerService;

    public JobLauncherService(JobLauncherCommonService jobLauncherCommonService,
                              Logger jobLauncherEventsLogger,
                              TaskManagerService taskManagerService) {
        this.jobLauncherCommonService = jobLauncherCommonService;
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.taskManagerService = taskManagerService;
    }

    /**
     * Trying to launch a computation. TaskDto is retrieved and status is checked, if its status enables to launch
     * the computation (READY, SUCCESS or ERROR), task status is changed to PENDING in the meantime and a message
     * to launch the computation is sent.
     * A boolean is returned to define the status of the launch operation. It is false only when the task is not found.
     * When the status does not enable to launch the computation it still returns true because the HTTP request was
     * correctly formed, it is just internal business logic that does not allow to launch the computation. If not
     * launched status will remain the same, if launched status will be changed to PENDING.
     *
     * @param timestamp: Task timestamp to be launched.
     * @return False only when the timestamp does not exist. Otherwise, true whether computation is launched or not.
     */
    public boolean launchJob(final String timestamp, final List<TaskParameterDto> parameters) {
        final String sanifiedTimestamp = LoggingUtil.sanifyString(timestamp);
        LOGGER.info("Received order to launch task {}", sanifiedTimestamp);
        final Optional<TaskDto> taskDtoOpt = taskManagerService.getTaskFromTimestamp(timestamp);
        if (taskDtoOpt.isPresent()) {
            final TaskDto taskDto = taskDtoOpt.get();
            // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", taskDto.getId().toString());

            if (isTaskReadyToBeLaunched(taskDto)) {
                jobLauncherCommonService.launchJob(taskDto, RUN_BINDING, parameters);
            } else {
                jobLauncherEventsLogger.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
            return true;
        } else {
            LOGGER.error("Failed to launch task with timestamp {}: could not retrieve task from the task-manager", sanifiedTimestamp);
        }
        return false;
    }

    private static boolean isTaskReadyToBeLaunched(final TaskDto taskDto) {
        return taskDto.getStatus() == TaskStatus.READY
                || taskDto.getStatus() == TaskStatus.SUCCESS
                || taskDto.getStatus() == TaskStatus.ERROR
                || taskDto.getStatus() == TaskStatus.INTERRUPTED;
    }

    public boolean stopJob(final String timestamp, final UUID runId) {
        final String sanifiedTimestamp = LoggingUtil.sanifyString(timestamp);
        LOGGER.info("Received order to interrupt task {}", sanifiedTimestamp);
        final Optional<TaskDto> taskDtoOpt = taskManagerService.getTaskFromTimestamp(timestamp);
        if (taskDtoOpt.isPresent()) {
            final TaskDto taskDto = taskDtoOpt.get();
            // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", taskDto.getId().toString());

            if (isTaskReadyToBeStopped(taskDto)) {
                jobLauncherCommonService.stopJob(runId, taskDto, STOP_BINDING);
            } else {
                jobLauncherEventsLogger.warn("Failed to interrupt task with timestamp {} because it is not pending or running yet", taskDto.getTimestamp());
            }
            return true;
        } else {
            LOGGER.error("Failed to interrupt task with timestamp {}: could not retrieve task from the task-manager", sanifiedTimestamp);
        }
        return false;
    }

    private static boolean isTaskReadyToBeStopped(final TaskDto taskDto) {
        return taskDto.getStatus() == TaskStatus.RUNNING
                || taskDto.getStatus() == TaskStatus.PENDING;
    }
}
