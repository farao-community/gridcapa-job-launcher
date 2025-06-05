/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.job_launcher.JobLauncherConfigurationProperties;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@SpringBootTest
class JobLauncherAutoServiceTest {

    @Autowired
    private StreamBridge streamBridge;
    @Autowired
    private JobLauncherAutoService service;

    @MockitoBean
    private JobLauncherService jobLauncherService;
    @MockitoBean
    private JobLauncherCommonService jobLauncherCommonService;
    @MockitoBean
    private JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    @MockitoBean
    private RestTemplateBuilder restTemplateBuilder;
    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void runReadyTasksWithNullPointerException() {
        Mockito.when(restTemplateBuilder.build()).thenThrow(RuntimeException.class);

        service.runReadyTasks(null);

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "PENDING", "RUNNING", "SUCCESS", "ERROR", "STOPPING", "INTERRUPTED"})
    void runReadyTasksWithTaskNotReady(final TaskStatus taskStatus) {
        final TaskDto taskDto = new TaskDto(null, null, taskStatus, null, null, null, null, null, null);

        service.runReadyTasks(taskDto);

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void runReadyTasksWithAllTriggerFilesAlreadyUsed() {
        final ProcessFileDto raoRequestFile = new ProcessFileDto(
                "path/to/raorequest.xml",
                "RAOREQUEST",
                ProcessFileStatus.VALIDATED,
                "raorequest.xml",
                "docId1",
                OffsetDateTime.now());
        final ProcessFileDto cracFile = new ProcessFileDto(
                "path/to/crac.xml",
                "CRAC",
                ProcessFileStatus.VALIDATED,
                "crac.xml",
                "docId2",
                OffsetDateTime.now());
        final ProcessRunDto processRunForCrac = new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now().minusHours(2), List.of(cracFile));
        final ProcessRunDto processRunForRaoRequest = new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now().minusHours(1), List.of(raoRequestFile));
        final TaskDto taskDto = new TaskDto(
                UUID.randomUUID(),
                OffsetDateTime.parse("2022-04-27T10:10Z"),
                TaskStatus.READY,
                List.of(raoRequestFile, cracFile),
                List.of(),
                List.of(),
                List.of(),
                List.of(processRunForCrac, processRunForRaoRequest),
                List.of());
        Mockito.when(jobLauncherConfigurationProperties.autoTriggerFiletypes()).thenReturn(List.of("RAOREQUEST", "CRAC"));

        service.runReadyTasks(taskDto);

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void runReadyTasksWithSomeTriggerFilesAlreadyUsedButNotAll() {
        final ProcessFileDto raoRequestFile = new ProcessFileDto(
                "path/to/raorequest.xml",
                "RAOREQUEST",
                ProcessFileStatus.VALIDATED,
                "raorequest.xml",
                "docId1",
                OffsetDateTime.now());
        final ProcessFileDto cracFile = new ProcessFileDto(
                "path/to/crac.xml",
                "CRAC",
                ProcessFileStatus.VALIDATED,
                "crac.xml",
                "docId2",
                OffsetDateTime.now());
        final ProcessRunDto processRunForRaoRequest = new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now().minusHours(1), List.of(raoRequestFile));
        final TaskDto taskDto = new TaskDto(
                UUID.randomUUID(),
                OffsetDateTime.parse("2022-04-27T10:10Z"),
                TaskStatus.READY,
                List.of(raoRequestFile, cracFile),
                List.of(),
                List.of(),
                List.of(),
                List.of(processRunForRaoRequest),
                List.of());
        Mockito.when(jobLauncherConfigurationProperties.autoTriggerFiletypes()).thenReturn(List.of("RAOREQUEST", "CRAC"));

        service.runReadyTasks(taskDto);

        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).launchJob(Mockito.eq(taskDto), Mockito.anyString());
    }

    @Test
    void runReadyTasksWithTriggerFilesFeatureDisabled() {
        final ProcessFileDto raoRequestFile = new ProcessFileDto(
                "path/to/raorequest.xml",
                "RAOREQUEST",
                ProcessFileStatus.VALIDATED,
                "raorequest.xml",
                "docId1",
                OffsetDateTime.now());
        final ProcessFileDto cracFile = new ProcessFileDto(
                "path/to/crac.xml",
                "CRAC",
                ProcessFileStatus.VALIDATED,
                "crac.xml",
                "docId2",
                OffsetDateTime.now());
        final ProcessRunDto processRunForCrac = new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now().minusHours(2), List.of(cracFile));
        final ProcessRunDto processRunForRaoRequest = new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now().minusHours(1), List.of(raoRequestFile));
        final TaskDto taskDto = new TaskDto(
                UUID.randomUUID(),
                OffsetDateTime.parse("2022-04-27T10:10Z"),
                TaskStatus.READY,
                List.of(raoRequestFile, cracFile),
                List.of(),
                List.of(),
                List.of(),
                List.of(processRunForCrac, processRunForRaoRequest),
                List.of());
        Mockito.when(jobLauncherConfigurationProperties.autoTriggerFiletypes()).thenReturn(List.of());

        service.runReadyTasks(taskDto);

        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).launchJob(Mockito.eq(taskDto), Mockito.anyString());
    }

    @Test
    void whenSendMessages() {
        final TaskDto taskDto1 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.READY, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        final TaskDto taskDto2 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f7"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.READY, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        final TaskDto taskDto3 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f8"), OffsetDateTime.parse("2022-04-27T10:12Z"), TaskStatus.READY, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.doNothing().when(restTemplate).put("http://test-uri/2022-04-27T10:10Z/status?status=PENDING", TaskDto.class);
        Mockito.doThrow(RuntimeException.class).when(restTemplate).put("http://test-uri/2022-04-27T10:11Z/status?status=PENDING", TaskDto.class);
        Mockito.doNothing().when(restTemplate).put("http://test-uri/2022-04-27T10:12Z/status?status=PENDING", TaskDto.class);
        assertTrue(streamBridge.send(
            "consumeTaskDtoUpdate-in-0",
            MessageBuilder.withPayload(taskDto1).build()
        ));
        /* if we remove the new catch exception block in JobLauncherAutoService.runReadyTasks(), then this next line fails ! */
        assertTrue(streamBridge.send(
            "consumeTaskDtoUpdate-in-0",
            MessageBuilder.withPayload(taskDto2).build()
        ));
        /* if we remove the new catch exception block in JobLauncherAutoService.runReadyTasks(), then this next line fails ! */
        assertTrue(streamBridge.send(
            "consumeTaskDtoUpdate-in-0",
            MessageBuilder.withPayload(taskDto3).build()
        ));
    }
}
