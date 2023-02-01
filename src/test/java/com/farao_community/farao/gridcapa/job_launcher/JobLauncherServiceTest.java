/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
public class JobLauncherServiceTest {

    @Autowired
    private JobLauncherService jobLauncherService;

    @Mock
    private MinioAdapter minioAdapter;

    @Test
    void testReplaceFilenameWithPresignedUrls() {
        TaskDto taskDto = new TaskDto(UUID.randomUUID(),
                OffsetDateTime.now(),
                TaskStatus.CREATED,
                null,
                Arrays.asList(new ProcessFileDto("filetype",
                        ProcessFileStatus.VALIDATED,
                        "file1.txt",
                        OffsetDateTime.now(),
                        "s3://bucket/file1.txt")),
                null,
                null);

        when(minioAdapter.generatePreSignedUrlFromFullMinioPath("s3://bucket/file1.txt", 1))
                .thenReturn("https://signed-url");

        TaskDto result = jobLauncherService.replaceFilenameWithPresignedUrls(taskDto);

        assertNotNull(result);
        assertEquals(TaskStatus.CREATED, result.getStatus());
        assertEquals(1, result.getInputs().size());
        assertEquals("filetype", result.getInputs().get(0).getFileType());
        assertEquals(ProcessFileStatus.VALIDATED, result.getInputs().get(0).getProcessFileStatus());
        assertEquals("file1.txt", result.getInputs().get(0).getFilename());
        assertEquals("https://signed-url", result.getInputs().get(0).getFileUrl());
    }

}
