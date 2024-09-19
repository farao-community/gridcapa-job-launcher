/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import com.farao_community.farao.gridcapa.job_launcher.JobLauncherConfigurationProperties;
import com.farao_community.farao.gridcapa.job_launcher.RetryException;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Marc Schwitzguébel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Service
public class InterruptionServerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterruptionServerService.class);
    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final RestTemplateBuilder restTemplateBuilder;

    public InterruptionServerService(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, RestTemplateBuilder restTemplateBuilder) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    @Retryable(retryFor = RetryException.class,
            backoff = @Backoff(delayExpression = "${retry.backoff.delay}", multiplierExpression = "${retry.backoff.multiplier}"),
            maxAttemptsExpression = "${retry.max-attempts}",
            recover = "fallbackInterruptRun")
    public Optional<Boolean> interruptRun(final UUID runId, final TaskDto taskDto) {
        try {
            final String interruptRunUrl = jobLauncherConfigurationProperties.url().interruptRunUrl() + taskDto.getId() + "?runId=" + runId;
            final ResponseEntity<Boolean> responseEntity = restTemplateBuilder.build().exchange(interruptRunUrl, HttpMethod.PUT, new HttpEntity<Object>(Map.of()), Boolean.class);
            return getOptionalFromResponseEntity(responseEntity);
        } catch (RestClientException e) {
            throw new RetryException("Exception occurred during request to interruption-server", e);
        }
    }

    @Recover
    public Optional<Boolean> fallbackInterruptRun(final Exception e, final UUID runId, final TaskDto taskDto) {
        LOGGER.error("Problem occurred while requesting interruption-server for timestamp {} and runId {}", taskDto.getTimestamp(), runId, e);
        return Optional.empty();
    }

    private static <T> Optional<T> getOptionalFromResponseEntity(final ResponseEntity<T> responseEntity) {
        if (responseEntity != null
                && responseEntity.getBody() != null
                && responseEntity.getStatusCode() == HttpStatus.OK) {
            return Optional.of(responseEntity.getBody());
        } else {
            throw new RetryException("Unexpected response from the interruption-server");
        }
    }
}
