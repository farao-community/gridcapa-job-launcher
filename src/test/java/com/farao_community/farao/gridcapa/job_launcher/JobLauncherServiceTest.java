package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class JobLauncherServiceTest {

    @Autowired
    private JobLauncherService jobLauncherService;

    @MockBean
    private MinioAdapter minioAdapter;

    @Test
    void testReplaceUrlWithPresignedUrl() {
        ProcessFileDto processFileDto = new ProcessFileDto("Type", ProcessFileStatus.VALIDATED, "filename", OffsetDateTime.now(), "fileUrl");
        List<ProcessFileDto> listInput = new ArrayList<>();
        listInput.add(processFileDto);
        TaskDto taskDto = new TaskDto(UUID.randomUUID(),
                OffsetDateTime.now(),
                TaskStatus.READY, null, listInput, null, null);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("fileUrl", 1)).thenReturn("newUrl");
        TaskDto taskDtoReplaced = jobLauncherService.replaceFilenameWithPresignedUrls(taskDto);
        assertEquals("newUrl", taskDtoReplaced.getInputs().get(0).getFileUrl());
    }

}
