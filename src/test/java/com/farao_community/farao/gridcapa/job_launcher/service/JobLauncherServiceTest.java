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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
class JobLauncherServiceTest {

    @Autowired
    private JobLauncherService service;

    @MockBean
    private JobLauncherCommonService jobLauncherCommonService;
    @MockBean
    private Logger jobLauncherEventsLogger;
    @MockBean
    private TaskManagerService taskManagerService;

    @Test
    void launchJobWithNoTaskDtoTest() {
        final String timestamp = "2024-09-18T09:30Z";
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.empty());

        final boolean launchJobResult = service.launchJob(timestamp, List.of());

        Assertions.assertThat(launchJobResult).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "PENDING", "RUNNING", "STOPPING"})
    void launchJobWithNotReadyTask(final TaskStatus taskStatus) {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), taskStatus, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.of(taskDto));

        final boolean launchJobResult = service.launchJob(timestamp, List.of());

        Assertions.assertThat(launchJobResult).isTrue();
        Mockito.verify(jobLauncherEventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR", "INTERRUPTED"})
    void launchJobWithReadyTaskAndParameters(final TaskStatus taskStatus) {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), taskStatus, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.of(taskDto));
        final List<TaskParameterDto> parameters = List.of(new TaskParameterDto("id", "type", "value", "default"));

        final boolean launchJobResult = service.launchJob(timestamp, parameters);

        Assertions.assertThat(launchJobResult).isTrue();
        ArgumentCaptor<List<TaskParameterDto>> parametersCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).launchJob(Mockito.eq(taskDto), Mockito.anyString(), parametersCaptor.capture());
        Assertions.assertThat(parametersCaptor.getValue()).isEqualTo(parameters);
    }

    @Test
    void stopJobWithNoTaskDtoTest() {
        final String timestamp = "2024-09-18T09:30Z";
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.empty());

        final boolean stopJobResult = service.stopJob(timestamp, UUID.randomUUID());

        Assertions.assertThat(stopJobResult).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "READY", "SUCCESS", "ERROR", "STOPPING", "INTERRUPTED"})
    void stopJobWithNotRunningTask(final TaskStatus taskStatus) {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), taskStatus, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.of(taskDto));

        boolean stopJobResult = service.stopJob(timestamp, UUID.randomUUID());

        Assertions.assertThat(stopJobResult).isTrue();
        Mockito.verify(jobLauncherEventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"RUNNING", "PENDING"})
    void stopJobWithRunningTask(final TaskStatus taskStatus) {
        final String timestamp = "2024-09-18T09:30Z";
        final UUID runId = UUID.randomUUID();
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), taskStatus, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.of(taskDto));

        boolean stopJobResult = service.stopJob(timestamp, runId);

        Assertions.assertThat(stopJobResult).isTrue();
        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).stopJob(Mockito.eq(runId), Mockito.eq(taskDto), Mockito.anyString());
    }
}
