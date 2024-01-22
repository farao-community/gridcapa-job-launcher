/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class JobLauncherServiceTest {

    @Autowired
    private JobLauncherService service;

    @MockBean
    private RestTemplateBuilder restTemplateBuilder;
    @MockBean
    private Logger jobLauncherEventsLogger;

    @Test
    void launchJobWithNoTaskDtoTest() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        boolean launchJobResult = service.launchJob("", List.of());

        Assertions.assertFalse(launchJobResult);
    }

    @Test
    void launchJobWithRunningTask() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.RUNNING, null, null, null, null);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(ResponseEntity.ok(taskDto));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        boolean launchJobResult = service.launchJob("", List.of());

        Assertions.assertTrue(launchJobResult);
        Mockito.verify(jobLauncherEventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @Test
    void launchJobWithPendingTask() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.PENDING, null, null, null, null);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(ResponseEntity.ok(taskDto));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        boolean launchJobResult = service.launchJob("", List.of());

        Assertions.assertTrue(launchJobResult);
        Mockito.verify(jobLauncherEventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @Test
    void stopJobWithNoTaskDtoTest() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        boolean launchJobResult = service.stopJob("");

        Assertions.assertFalse(launchJobResult);
    }

    @Test
    void stopJobWithErrorInTaskManagerRequestTest() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        boolean launchJobResult = service.stopJob("");

        Assertions.assertFalse(launchJobResult);
    }

    @Test
    void stopJobWithPendingTask() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.PENDING, null, null, null, null);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(ResponseEntity.ok(taskDto));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        boolean launchJobResult = service.stopJob("");

        Assertions.assertTrue(launchJobResult);
        Mockito.verify(jobLauncherEventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }
}
