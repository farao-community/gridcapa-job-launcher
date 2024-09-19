/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class InterruptionServerServiceTest {
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;
    @Autowired
    private InterruptionServerService interruptionServerService;

    @Test
    void interruptRunNoRetry() {
        final UUID runId = UUID.randomUUID();
        final UUID taskId = UUID.randomUUID();
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(taskDto.getId()).thenReturn(taskId);
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(runId.toString()), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(Boolean.class)))
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        final Optional<Boolean> result = interruptionServerService.interruptRun(runId, taskDto);

        Assertions.assertThat(result).contains(true);
    }

    @Test
    void interruptRunWithErrorInInterruptionServer() {
        final UUID runId = UUID.randomUUID();
        final UUID taskId = UUID.randomUUID();
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(taskDto.getId()).thenReturn(taskId);
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(runId.toString()), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        final Optional<Boolean> result = interruptionServerService.interruptRun(runId, taskDto);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void interruptRunRetryOnce() {
        final UUID runId = UUID.randomUUID();
        final UUID taskId = UUID.randomUUID();
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(taskDto.getId()).thenReturn(taskId);
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(runId.toString()), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(Boolean.class)))
                .thenThrow(RestClientException.class)
                .thenReturn(new ResponseEntity<>(true, HttpStatus.OK));

        final Optional<Boolean> result = interruptionServerService.interruptRun(runId, taskDto);

        Assertions.assertThat(result).contains(true);
    }

    @Test
    void interruptRunAllRetry() {
        final UUID runId = UUID.randomUUID();
        final UUID taskId = UUID.randomUUID();
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(taskDto.getId()).thenReturn(taskId);
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(runId.toString()), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(Boolean.class)))
                .thenThrow(RestClientException.class);

        final Optional<Boolean> result = interruptionServerService.interruptRun(runId, taskDto);

        Assertions.assertThat(result).isEmpty();
    }
}
