/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SuppressWarnings("hideutilityclassconstructor")
@EnableConfigurationProperties(JobLauncherConfigurationProperties.class)
@EnableWebMvc
@SpringBootApplication
@EnableScheduling
@EnableRetry
public class JobLauncherApplication {
    public static void main(String[] args) {
        SpringApplication.run(JobLauncherApplication.class, args);
    }
}
