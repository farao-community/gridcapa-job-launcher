/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@ConfigurationProperties("job-launcher")
public class JobLauncherConfigurationProperties {
    private String taskManagerTimestampUrl;
    private String taskManagerBusinessDateUrl;

    public String getTaskManagerTimestampUrl() {
        return taskManagerTimestampUrl;
    }

    public void setTaskManagerTimestampUrl(String taskManagerTimestampUrl) {
        this.taskManagerTimestampUrl = taskManagerTimestampUrl;
    }

    public String getTaskManagerBusinessDateUrl() {
        return taskManagerBusinessDateUrl;
    }

    public void setTaskManagerBusinessDateUrl(String taskManagerBusinessDateUrl) {
        this.taskManagerBusinessDateUrl = taskManagerBusinessDateUrl;
    }
}
