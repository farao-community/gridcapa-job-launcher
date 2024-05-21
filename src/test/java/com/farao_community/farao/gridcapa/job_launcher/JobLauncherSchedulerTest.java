/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class JobLauncherSchedulerTest {
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;
    @MockBean
    private JobLauncherCommonService jobLauncherCommonService;

    @Autowired
    private JobLauncherScheduler jobLauncherScheduler;

    @Test
    void automaticTaskStartWithException() {
        Mockito.when(restTemplateBuilder.build()).thenThrow(RuntimeException.class);

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void automaticTaskStartWithEmptyResponseBody() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ResponseEntity responseEntity = Mockito.mock(ResponseEntity.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any())).thenReturn(responseEntity);

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void automaticTaskStartWithResponseStatusNotOk() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ResponseEntity responseEntity = Mockito.mock(ResponseEntity.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any())).thenReturn(responseEntity);
        Mockito.when(responseEntity.getBody()).thenReturn(new TaskDto[]{});
        Mockito.when(responseEntity.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT);

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "PENDING", "RUNNING", "SUCCESS", "ERROR", "STOPPING", "INTERRUPTED"})
    void automaticTaskStartWithNotReadyTask(TaskStatus taskStatus) {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        TaskDto taskDto = new TaskDto(null, null, taskStatus, null, null, null, null, null, null);
        TaskDto[] body = {taskDto};
        ResponseEntity responseEntity = ResponseEntity.ok(body);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any())).thenReturn(responseEntity);

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verifyNoInteractions(jobLauncherCommonService);
    }

    @Test
    void automaticTaskStartWithReadyTask() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), null, TaskStatus.READY, null, null, null, null, null, null);
        TaskDto[] body = {taskDto};
        ResponseEntity responseEntity = ResponseEntity.ok(body);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.any())).thenReturn(responseEntity);

        jobLauncherScheduler.automaticTaskStart();

        Mockito.verify(jobLauncherCommonService, Mockito.times(1)).launchJob(Mockito.eq(taskDto), Mockito.anyString());
    }
}
