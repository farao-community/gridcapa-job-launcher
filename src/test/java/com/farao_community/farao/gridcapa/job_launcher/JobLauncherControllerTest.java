package com.farao_community.farao.gridcapa.job_launcher;

import com.farao_community.farao.gridcapa.job_launcher.service.JobLauncherService;
import com.farao_community.farao.gridcapa.task_manager.api.ParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class JobLauncherControllerTest {

    @Autowired
    private JobLauncherController jobLauncherController;

    @MockitoBean
    private JobLauncherService jobLauncherService;

    @Test
    void testLaunchJobWithParametersOk() {
        final String timestamp = "2021-12-09T21:30";
        final List<ParameterDto> parameterDtoList = List.of(new ParameterDto("id", "name", 1, "type", "section", 1, "value", "default"));
        final ArgumentCaptor<List<TaskParameterDto>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.when(jobLauncherService.launchJob(Mockito.eq(timestamp), listArgumentCaptor.capture())).thenReturn(true);

        final ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, parameterDtoList);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(listArgumentCaptor.getValue()).isNotEmpty();
    }

    @Test
    void testLaunchJobWithoutParametersOk() {
        final String timestamp = "2021-12-09T21:30";
        Mockito.when(jobLauncherService.launchJob(timestamp, List.of())).thenReturn(true);

        final ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, List.of());

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testLaunchJobNotFound() {
        final String timestamp = "2021-12-09T21:30";
        final List<ParameterDto> parameterDtoList = null;
        Mockito.when(jobLauncherService.launchJob(timestamp, List.of())).thenReturn(false);

        final ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, parameterDtoList);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testStopJobOk() {
        final String timestamp = "2021-12-09T21:30";
        final UUID runId = UUID.randomUUID();
        Mockito.when(jobLauncherService.stopJob(timestamp, runId)).thenReturn(true);

        final ResponseEntity<Void> response = jobLauncherController.stopJob(timestamp, runId);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testStopJobNotFound() {
        final String timestamp = "2021-12-09T21:30";
        final UUID runId = UUID.randomUUID();
        Mockito.when(jobLauncherService.stopJob(timestamp, runId)).thenReturn(false);

        final ResponseEntity<Void> response = jobLauncherController.stopJob(timestamp, runId);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
