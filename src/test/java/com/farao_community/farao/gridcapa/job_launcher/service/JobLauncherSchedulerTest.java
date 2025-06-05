/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class JobLauncherSchedulerTest {
    @Autowired
    private JobLauncherScheduler jobLauncherScheduler;

    @MockitoBean
    private JobLauncherCommonService jobLauncherCommonService;
    @MockitoBean
    private TaskManagerService taskManagerService;

    private String startingDate;

    @BeforeEach
    void init() {
        this.startingDate = jobLauncherScheduler.getStartingDate();
    }

    @Test
    void automaticTaskStartNoTasks() {
        Mockito.when(taskManagerService.getTasksFromBusinessDate(startingDate)).thenReturn(Optional.empty());

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void automaticTaskStartWithResponseStatusNotOk() {
        final TaskDto[] taskDtoArray = {};
        Mockito.when(taskManagerService.getTasksFromBusinessDate(startingDate)).thenReturn(Optional.of(taskDtoArray));

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "PENDING", "RUNNING", "SUCCESS", "ERROR", "STOPPING", "INTERRUPTED"})
    void automaticTaskStartWithNotReadyTask(final TaskStatus taskStatus) {
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), null, taskStatus, null, null, null, null, null, null);
        final TaskDto[] taskDtoArray = {taskDto};
        Mockito.when(taskManagerService.getTasksFromBusinessDate(startingDate)).thenReturn(Optional.of(taskDtoArray));

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void automaticTaskStartWithReadyTask() {
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), null, TaskStatus.READY, null, null, null, null, null, null);
        final TaskDto[] taskDtoArray = {taskDto};
        Mockito.when(taskManagerService.getTasksFromBusinessDate(startingDate)).thenReturn(Optional.of(taskDtoArray));

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).launchJob(Mockito.eq(taskDto), Mockito.anyString());
    }
}
