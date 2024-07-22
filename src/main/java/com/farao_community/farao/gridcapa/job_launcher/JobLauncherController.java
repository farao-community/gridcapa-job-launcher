/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.ParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobLauncherController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherController.class);

    private final JobLauncherService jobLauncherService;

    public JobLauncherController(JobLauncherService jobLauncherService) {
        this.jobLauncherService = jobLauncherService;
    }

    @PostMapping(value = "/start/{timestamp}")
    public ResponseEntity<Void> launchJob(@PathVariable String timestamp, @RequestBody List<ParameterDto> parameters) {
        List<TaskParameterDto> taskParameterDtos = List.of();
        if (parameters != null) {
            taskParameterDtos = parameters.stream().map(TaskParameterDto::new).toList();
        }
        if (jobLauncherService.launchJob(timestamp, taskParameterDtos)) {
            return ResponseEntity.ok().build();
        }
        return getEmptyResponseEntity(timestamp);
    }

    @PostMapping(value = "/stop/{timestamp}/{runId}")
    public ResponseEntity<Void> stopJob(@PathVariable String timestamp, @PathVariable UUID runId) {
        if (jobLauncherService.stopJob(timestamp, runId)) {
            return ResponseEntity.ok().build();
        }
        return getEmptyResponseEntity(timestamp);
    }

    private ResponseEntity<Void> getEmptyResponseEntity(@PathVariable String timestamp) {
        LOGGER.error("Failed to retrieve task with timestamp {}", timestamp);
        return ResponseEntity.notFound().build();
    }

}
