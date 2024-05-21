/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class JobLauncherCommonService {
    private final Logger jobLauncherEventsLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final StreamBridge streamBridge;
    private final String taskManagerTimestampBaseUrl;

    public JobLauncherCommonService(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, Logger jobLauncherEventsLogger, RestTemplateBuilder restTemplateBuilder, StreamBridge streamBridge) {
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.streamBridge = streamBridge;
        this.taskManagerTimestampBaseUrl = jobLauncherConfigurationProperties.getUrl().getTaskManagerTimestampUrl();
    }

    public void launchJob(TaskDto taskDto, String runBinding) {
        String timestamp = taskDto.getTimestamp().toString();
        jobLauncherEventsLogger.info("Task launched on TS {}", timestamp);
        restTemplateBuilder.build().put(getUrlToUpdateTaskStatus(timestamp, TaskStatus.PENDING), TaskDto.class);
        restTemplateBuilder.build().put(getUrlToAddNewRunInTaskHistory(timestamp), TaskDto.class);
        streamBridge.send(runBinding, taskDto);
    }

    public void stopJob(TaskDto taskDto, String stopBinding) {
        String timestamp = taskDto.getTimestamp().toString();
        jobLauncherEventsLogger.info("Stopping task with timestamp {}", timestamp);
        restTemplateBuilder.build().put(getUrlToUpdateTaskStatus(timestamp, TaskStatus.STOPPING), TaskDto.class);
        streamBridge.send(stopBinding, taskDto.getId().toString());
    }

    private String getUrlToUpdateTaskStatus(String timestamp, TaskStatus taskStatus) {
        return taskManagerTimestampBaseUrl + timestamp + "/status?status=" + taskStatus;
    }

    private String getUrlToAddNewRunInTaskHistory(String timestamp) {
        return taskManagerTimestampBaseUrl + timestamp + "/runHistory";
    }
}
