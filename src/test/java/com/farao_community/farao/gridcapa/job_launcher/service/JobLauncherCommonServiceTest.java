/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class JobLauncherCommonServiceTest {

    private static final String TEST_URL = "http://test-uri/";

    @Autowired
    private JobLauncherCommonService service;

    @MockBean
    private Logger jobLauncherEventsLogger;
    @MockBean
    private StreamBridge streamBridge;
    @MockBean
    private InterruptionServerService interruptionServerService;
    @MockBean
    private TaskManagerService taskManagerService;

    @Test
    void launchJobWithErrorAtAddingNewRun() {
        final String binding = "TEST_BINDING";
        final UUID id = UUID.randomUUID();
        final String timestamp = "2022-04-27T10:10Z";
        final List<ProcessFileDto> inputs = List.of();
        final TaskDto taskDto = new TaskDto(id, OffsetDateTime.parse(timestamp), TaskStatus.READY, inputs, List.of(), List.of(), List.of(), List.of(), List.of());
        Mockito.when(taskManagerService.addNewRunInTaskHistory(timestamp, inputs)).thenReturn(Optional.empty());

        service.launchJob(taskDto, binding);

        verify(streamBridge, times(1)).send(eq("task-status-update"), argThat((TaskStatusUpdate tsu) -> id.equals(tsu.getId()) && tsu.getTaskStatus() == TaskStatus.ERROR));
    }

    @Test
    void launchJobWithErrorAtStatusUpdate() {
        final String binding = "TEST_BINDING";
        final UUID id = UUID.randomUUID();
        final String timestamp = "2022-04-27T10:10Z";
        final List<ProcessFileDto> inputs = List.of();
        final List<ProcessRunDto> runHistory = List.of(new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now(), inputs));
        final TaskDto taskDto = new TaskDto(id, OffsetDateTime.parse(timestamp), TaskStatus.READY, inputs, List.of(), List.of(), List.of(), runHistory, List.of());
        Mockito.when(taskManagerService.addNewRunInTaskHistory(timestamp, inputs)).thenReturn(Optional.of(taskDto));
        Mockito.when(taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING)).thenReturn(false);

        service.launchJob(taskDto, binding);

        verify(streamBridge, times(1)).send(eq("task-status-update"), argThat((TaskStatusUpdate tsu) -> id.equals(tsu.getId()) && tsu.getTaskStatus() == TaskStatus.ERROR));
    }

    @Test
    void launchJobWithoutParameters() {
        final String binding = "TEST_BINDING";
        final UUID id = UUID.randomUUID();
        final String timestamp = "2022-04-27T10:10Z";
        final List<ProcessFileDto> inputs = List.of();
        final List<ProcessRunDto> runHistory = List.of(new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now(), inputs));
        final TaskDto taskDto = new TaskDto(id, OffsetDateTime.parse(timestamp), TaskStatus.READY, inputs, List.of(), List.of(), List.of(), runHistory, List.of());
        Mockito.when(taskManagerService.addNewRunInTaskHistory(timestamp, inputs)).thenReturn(Optional.of(taskDto));
        Mockito.when(taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING)).thenReturn(true);

        service.launchJob(taskDto, binding);

        verify(streamBridge, times(1)).send(binding, taskDto);
    }

    @Test
    void launchJobWithParameters() {
        final String binding = "TEST_BINDING";
        final UUID id = UUID.randomUUID();
        final String timestamp = "2022-04-27T10:10Z";
        final List<ProcessFileDto> inputs = List.of();
        final List<ProcessRunDto> runHistory = List.of(new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now(), inputs));
        final TaskDto taskDto = new TaskDto(id, OffsetDateTime.parse(timestamp), TaskStatus.READY, inputs, List.of(), List.of(), List.of(), runHistory, List.of());
        Mockito.when(taskManagerService.addNewRunInTaskHistory(timestamp, inputs)).thenReturn(Optional.of(taskDto));
        Mockito.when(taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING)).thenReturn(true);
        List<TaskParameterDto> parameters = List.of(new TaskParameterDto("id", "type", "value", "default"));

        service.launchJob(taskDto, binding, parameters);

        ArgumentCaptor<TaskDto> taskDtoCaptor = ArgumentCaptor.forClass(TaskDto.class);
        verify(streamBridge, times(1)).send(eq(binding), taskDtoCaptor.capture());
        Assertions.assertThat(taskDtoCaptor.getValue()).isNotNull();
        Assertions.assertThat(taskDtoCaptor.getValue().getParameters())
                .isNotEmpty()
                .containsAll(parameters);
    }

    @Test
    void stopJobWithInterruptionError() {
        final String binding = "TEST_BINDING";
        final OffsetDateTime timestamp = OffsetDateTime.parse("2022-04-27T10:10Z");
        final UUID taskId = UUID.randomUUID();
        final UUID runId = UUID.randomUUID();
        final TaskDto taskDto = new TaskDto(taskId, timestamp, TaskStatus.RUNNING, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        Mockito.when(interruptionServerService.interruptRun(runId, taskDto)).thenReturn(Optional.empty());

        service.stopJob(runId, taskDto, binding);

        verify(streamBridge, times(0)).send(binding, taskId.toString());
        verify(jobLauncherEventsLogger, times(1)).warn(Mockito.anyString(), eq(timestamp));
    }

    @Test
    void stopJobWithInterruptionOk() {
        final String binding = "TEST_BINDING";
        final String timestamp = "2022-04-27T10:10Z";
        final UUID taskId = UUID.randomUUID();
        final UUID runId = UUID.randomUUID();
        final TaskDto taskDto = new TaskDto(taskId, OffsetDateTime.parse(timestamp), TaskStatus.RUNNING, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        Mockito.when(interruptionServerService.interruptRun(runId, taskDto)).thenReturn(Optional.of(true));

        service.stopJob(runId, taskDto, binding);

        verify(interruptionServerService, times(1)).interruptRun(runId, taskDto);
        verify(streamBridge, times(1)).send(binding, taskId.toString());
        verify(taskManagerService, times(1)).updateTaskStatus(timestamp, TaskStatus.STOPPING);
    }
}
