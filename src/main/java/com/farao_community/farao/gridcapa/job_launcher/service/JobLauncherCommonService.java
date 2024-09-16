/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.slf4j.Logger;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class JobLauncherCommonService {
    private static final String TASK_STATUS_UPDATE = "task-status-update";
    private final Logger jobLauncherEventsLogger;
    private final StreamBridge streamBridge;
    private final InterruptionServerService interruptionServerService;
    private final TaskManagerService taskManagerService;

    public JobLauncherCommonService(Logger jobLauncherEventsLogger,
                                    StreamBridge streamBridge,
                                    InterruptionServerService interruptionServerService,
                                    TaskManagerService taskManagerService) {
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.streamBridge = streamBridge;
        this.interruptionServerService = interruptionServerService;
        this.taskManagerService = taskManagerService;
    }

    public void launchJob(final TaskDto taskDto, final String runBinding) {
        this.launchJob(taskDto, runBinding, null);
    }

    public void launchJob(final TaskDto taskDto, final String runBinding, final List<TaskParameterDto> parameters) {
        final String timestamp = taskDto.getTimestamp().toString();
        final Optional<TaskDto> taskDtoWithRunOpt = taskManagerService.addNewRunInTaskHistory(timestamp, taskDto.getInputs());
        if (taskDtoWithRunOpt.isPresent()) {
            TaskDto taskDtoWithRun = taskDtoWithRunOpt.get();
            if (parameters != null && !parameters.isEmpty()) {
                taskDtoWithRun = new TaskDto(taskDtoWithRun.getId(), taskDtoWithRun.getTimestamp(), taskDtoWithRun.getStatus(), taskDtoWithRun.getInputs(), taskDtoWithRun.getAvailableInputs(), taskDtoWithRun.getOutputs(), taskDtoWithRun.getProcessEvents(), taskDtoWithRun.getRunHistory(), parameters);
            }

            final boolean taskStatusUpdated = taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING);
            if (taskStatusUpdated) {
                jobLauncherEventsLogger.info("Task launched on TS {}", timestamp);
                streamBridge.send(runBinding, taskDtoWithRun);
            } else {
                jobLauncherEventsLogger.warn("Failed to launch task on TS {}: could not set task's status to PENDING", taskDto.getTimestamp());
                streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(taskDto.getId(), TaskStatus.ERROR));
            }
        } else {
            jobLauncherEventsLogger.warn("Failed to launch task on TS {}: could not add new run to the task", taskDto.getTimestamp());
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(taskDto.getId(), TaskStatus.ERROR));
        }
    }

    public void stopJob(final UUID runId, final TaskDto taskDto, final String stopBinding) {
        final String timestamp = taskDto.getTimestamp().toString();
        jobLauncherEventsLogger.info("Stopping task with timestamp {}", timestamp);
        Optional<Boolean> interruptionOpt = interruptionServerService.interruptRun(runId, taskDto);
        if (interruptionOpt.isPresent()) {
            streamBridge.send(stopBinding, taskDto.getId().toString());
            taskManagerService.updateTaskStatus(timestamp, TaskStatus.STOPPING);
        } else {
            jobLauncherEventsLogger.warn("Failed to stop task on TS {}: could not contact interruption-server", taskDto.getTimestamp());
        }
    }
}
