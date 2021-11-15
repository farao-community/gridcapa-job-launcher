package com.faraocommunity.farao.gridcapajoblauncher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@RestController
public class JobController {

    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobController.class);

    public JobController(JobLauncherConfigurationProperties jobLauncherConfigurationProperties) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
    }

    @PostMapping(value = "/start/{id}")
    public void launchJob(@PathVariable String id) throws IOException {
        LOGGER.info("Received order to launch task {}", id);
        WebClient client = WebClient.create(jobLauncherConfigurationProperties.getTaskManagerUrlProperties().getTaskManagerUrl());
        WebClient.UriSpec<WebClient.RequestBodySpec> uriSpec = client.post();
        WebClient.RequestBodySpec bodySpec = uriSpec.uri("tasks/");
        WebClient.RequestHeadersSpec<?> headersSpec = bodySpec.bodyValue(id);
        WebClient.ResponseSpec responseSpec = headersSpec.header(
                        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .acceptCharset(StandardCharsets.UTF_8)
                .ifNoneMatch("*")
                .ifModifiedSince(ZonedDateTime.now())
                .retrieve();
        responseSpec.bodyToMono(String.class).subscribe();
        LOGGER.info("Post request sent");
    }

}
