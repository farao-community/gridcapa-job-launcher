/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import org.awaitility.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class JobLauncherSchedulerTest {

    @SpyBean
    private JobLauncherScheduler jobLauncherScheduler;

    @Test
    void scheduledIsCalledAtLeastOneTime() {
        await()
                .atMost(Duration.FIVE_SECONDS)
                .untilAsserted(() -> verify(jobLauncherScheduler, atLeastOnce()).automaticTaskStart());
    }
}
