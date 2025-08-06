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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@SpringBootTest
class JobLauncherServiceTest {

    @Autowired
    private JobLauncherService service;

    @MockitoBean
    private JobLauncherCommonService jobLauncherCommonService;
    @MockitoBean
    private Logger jobLauncherEventsLogger;
    @MockitoBean
    private TaskManagerService taskManagerService;

    @Test
    void launchJobWithNoTaskDtoTest() {
        final String timestamp = "2024-09-18T09:30Z";
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.empty());

        final boolean launchJobResult = service.launchJob(timestamp, List.of());

        Assertions.assertThat(launchJobResult).isFalse();
    }

    @Test
    @DisplayName("Testing that a timestamp cannot be launched twice simultaneously.")
    void testSimultaneity() throws ExecutionException, InterruptedException {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), TaskStatus.ERROR, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp))
                .thenAnswer(AdditionalAnswers
                        .answersWithDelay(1000, invocation -> Optional.of(taskDto)));
        // Use CountDownLatch to ensure both threads start simultaneously
        final CountDownLatch startLatch = new CountDownLatch(1);
        final Supplier<Boolean> supplier = () -> {
            try {
                startLatch.await(); // Both threads wait here
                return service.launchJob(timestamp, List.of());
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        };
        final CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(supplier);
        final CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(supplier);
        // Release both threads at once
        startLatch.countDown();
        // Get results and verify one succeeded and one failed
        final Boolean result1 = future1.get();
        final Boolean result2 = future2.get();
        // Either result1 is true and result2 is false, or vice versa
        Assertions.assertThat(result1).isNotEqualTo(result2);
        Assertions.assertThat(result1 || result2).isTrue(); // At least one should succeed
        Assertions.assertThat(result1 && result2).isFalse(); // Both cannot succeed
        // Ensure that timestamp has been cleared and can be started again
        Assertions.assertThat(service.launchJob(timestamp, List.of())).isTrue();
    }

    @Test
    @DisplayName("Testing that a timestamp is properly cleaned up after an exception occurs.")
    void testException() {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), TaskStatus.ERROR, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp))
                // crashes on first run
                .thenThrow(new RuntimeException())
                // then succeeds on second
                .thenReturn(Optional.of(taskDto));
        final List<TaskParameterDto> emptyList = List.of();
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> service.launchJob(timestamp, emptyList));
        Assertions.assertThat(service.launchJob(timestamp, emptyList)).isTrue();
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
        final ArgumentCaptor<List<TaskParameterDto>> parametersCaptor = ArgumentCaptor.forClass(List.class);
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

        final boolean stopJobResult = service.stopJob(timestamp, UUID.randomUUID());

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

        final boolean stopJobResult = service.stopJob(timestamp, runId);

        Assertions.assertThat(stopJobResult).isTrue();
        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).stopJob(Mockito.eq(runId), Mockito.eq(taskDto), Mockito.anyString());
    }
}
