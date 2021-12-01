/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@ConstructorBinding
@ConfigurationProperties("job-launcher")
public class JobLauncherConfigurationProperties {
    private final TaskManagerUrlProperties taskManagerUrl;

    public JobLauncherConfigurationProperties(TaskManagerUrlProperties taskManagerUrl) {
        this.taskManagerUrl = taskManagerUrl;
    }

    public TaskManagerUrlProperties getTaskManagerUrlProperties() {
        return taskManagerUrl;
    }

    public static final class TaskManagerUrlProperties {
        private final String taskManagerUrl;

        public TaskManagerUrlProperties(String taskManagerUrl) {
            this.taskManagerUrl = taskManagerUrl;
        }

        public String getTaskManagerUrl() {
            return taskManagerUrl;
        }
    }
}
