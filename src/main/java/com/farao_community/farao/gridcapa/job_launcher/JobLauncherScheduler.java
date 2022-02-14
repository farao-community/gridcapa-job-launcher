/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
@ConditionalOnProperty(name = "scheduler.enable", havingValue = "true")
public class JobLauncherScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherScheduler.class);

    @Scheduled(cron = "${scheduler.cronjob}")
    public void automaticTaskStart() {
        LOGGER.info("automatic start");
    }
}
