package com.faraocommunity.farao.gridcapajoblauncher;

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
