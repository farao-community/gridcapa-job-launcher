/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@ConfigurationProperties("job-launcher")
public class JobLauncherConfigurationProperties {
    private final UrlProperties url;
    private final ProcessProperties process;
    private final List<String> autoTriggerFiletypes;

    public JobLauncherConfigurationProperties(UrlProperties url, ProcessProperties process, List<String> autoTriggerFiletypes) {
        this.url = url;
        this.process = process;
        this.autoTriggerFiletypes = autoTriggerFiletypes;
    }

    public UrlProperties getUrl() {
        return url;
    }

    public ProcessProperties getProcess() {
        return process;
    }

    public List<String> getAutoTriggerFiletypes() {
        return autoTriggerFiletypes;
    }

    public record UrlProperties(String taskManagerTimestampUrl, String taskManagerBusinessDateUrl) { }

    public record ProcessProperties(String tag, String timezone, int daysToAdd) { }
}
