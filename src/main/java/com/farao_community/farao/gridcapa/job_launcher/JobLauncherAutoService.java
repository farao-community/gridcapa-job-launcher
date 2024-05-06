/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
@ConditionalOnProperty(name = "job-launcher.auto", havingValue = "true")
public class JobLauncherAutoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherAutoService.class);
    private static final String RUN_BINDING = "run-task-auto";
    private final Logger jobLauncherEventsLogger;
    private final StreamBridge streamBridge;
    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final RestTemplateBuilder restTemplateBuilder;

    public JobLauncherAutoService(Logger jobLauncherEventsLogger, StreamBridge streamBridge, JobLauncherConfigurationProperties jobLauncherConfigurationProperties, RestTemplateBuilder restTemplateBuilder) {
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.streamBridge = streamBridge;
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeTaskDtoUpdate() {
        return f -> f
                .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
                .subscribe(this::runReadyTasks);
    }

    void runReadyTasks(TaskDto updatedTaskDto) {
        try {
            if (updatedTaskDto.getStatus().equals(TaskStatus.READY)) {
                boolean autoTriggerFiletypesDefinedInConfig = !jobLauncherConfigurationProperties.getAutoTriggerFiletypes().isEmpty();
                if (autoTriggerFiletypesDefinedInConfig && allTriggerFilesAlreadyUsed(updatedTaskDto)) {
                    // If all selected files corresponding to trigger filetypes are linked to some Run in Task's history,
                    // then the update does not concern a trigger file, so job launcher should do nothing
                    return;
                }

                // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
                // This should be done only once, as soon as the information to add in mdc is available.
                MDC.put("gridcapa-task-id", updatedTaskDto.getId().toString());
                jobLauncherEventsLogger.info("Task launched on TS {}", updatedTaskDto.getTimestamp());
                restTemplateBuilder.build().put(getUrlToUpdateTaskStatusToPending(updatedTaskDto), TaskDto.class);
                streamBridge.send(RUN_BINDING, Objects.requireNonNull(updatedTaskDto));
            }
        } catch (Exception e) {
            /* this exeption block avoids gridcapa export from disconnecting from spring cloud stream !*/
            LOGGER.error(e.getMessage(), e);
        }
    }

    private boolean allTriggerFilesAlreadyUsed(TaskDto updatedTaskDto) {
        List<ProcessFileDto> triggerFiles = updatedTaskDto.getInputs().stream()
                .filter(f -> jobLauncherConfigurationProperties.getAutoTriggerFiletypes().contains(f.getFileType()))
                .toList();

        Set<ProcessFileDto> filesUsedInPreviousRun = updatedTaskDto.getRunHistory().stream()
                .flatMap(run -> run.getInputs().stream())
                .collect(Collectors.toSet());
        return filesUsedInPreviousRun.containsAll(triggerFiles);
    }

    private String getUrlToUpdateTaskStatusToPending(TaskDto taskDto) {
        String url = jobLauncherConfigurationProperties.getUrl().getTaskManagerTimestampUrl() + taskDto.getTimestamp() + "/status";
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                .queryParam("status", TaskStatus.PENDING);
        return builder.toUriString();
    }
}
