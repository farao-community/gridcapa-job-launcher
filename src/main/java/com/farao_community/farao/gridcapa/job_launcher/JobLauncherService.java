package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Objects;
import org.slf4j.Logger;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@Service
public class JobLauncherService {
    private static final String RUN_BINDING = "run-task";

    private final StreamBridge streamBridge;
    private final Logger jobLauncherEventsLogger;

    public JobLauncherService(StreamBridge streamBridge, Logger jobLauncherEventsLogger) {
        this.streamBridge = streamBridge;
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
    }

    public void launchJob(TaskDto taskDto) {
        if (taskDto.getStatus() == TaskStatus.READY
            || taskDto.getStatus() == TaskStatus.SUCCESS
            || taskDto.getStatus() == TaskStatus.ERROR) {
            jobLauncherEventsLogger.info("Task launched on TS {}", taskDto.getTimestamp());
            streamBridge.send(RUN_BINDING, Objects.requireNonNull(taskDto));
        } else {
            jobLauncherEventsLogger.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
        }
    }
}
