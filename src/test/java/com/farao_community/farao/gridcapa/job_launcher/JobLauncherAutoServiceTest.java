package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JobLauncherAutoServiceTest {

    @Autowired
    private StreamBridge streamBridge;

    @MockBean
    private RestTemplateBuilder restTemplateBuilder;
    @MockBean
    private RestTemplate restTemplate;

    @Test
    void whenSendMessages() {
        TaskDto taskDto1 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f6"), OffsetDateTime.parse("2022-04-27T10:10Z"), TaskStatus.READY, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        TaskDto taskDto2 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f7"), OffsetDateTime.parse("2022-04-27T10:11Z"), TaskStatus.READY, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        TaskDto taskDto3 = new TaskDto(UUID.fromString("1fdda469-53e9-4d63-a533-b935cffdd2f8"), OffsetDateTime.parse("2022-04-27T10:12Z"), TaskStatus.READY, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.doNothing().when(restTemplate).put("http://test-uri/2022-04-27T10:10Z/status?status=PENDING", TaskDto.class);
        Mockito.doThrow(RuntimeException.class).when(restTemplate).put("http://test-uri/2022-04-27T10:11Z/status?status=PENDING", TaskDto.class);
        Mockito.doNothing().when(restTemplate).put("http://test-uri/2022-04-27T10:12Z/status?status=PENDING", TaskDto.class);
        assertTrue(streamBridge.send("consumeTaskDtoUpdate-in-0", MessageBuilder.withPayload(taskDto1)
                .build()));
        /* if we remove the new catch exception block in JobLauncherAutoService.runReadyTasks(), then this next line fails ! */
        assertTrue(streamBridge.send("consumeTaskDtoUpdate-in-0", MessageBuilder.withPayload(taskDto2)
                .build()));
        /* if we remove the new catch exception block in JobLauncherAutoService.runReadyTasks(), then this next line fails ! */
        assertTrue(streamBridge.send("consumeTaskDtoUpdate-in-0", MessageBuilder.withPayload(taskDto3)
                .build()));
    }
}
