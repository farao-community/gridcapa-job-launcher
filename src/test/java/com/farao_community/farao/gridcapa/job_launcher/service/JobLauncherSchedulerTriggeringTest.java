/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher.service;

import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SpringBootTest
class JobLauncherSchedulerTriggeringTest {

    @MockitoSpyBean
    private JobLauncherScheduler jobLauncherScheduler;

    @Test
    void scheduledIsCalledAtLeastOneTime() {
        await()
                .atMost(Durations.ONE_MINUTE)
                .untilAsserted(() -> verify(jobLauncherScheduler, atLeastOnce()).automaticTaskStart());
    }
}
