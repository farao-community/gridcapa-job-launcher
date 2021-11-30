package com.farao_community.farao.gridcapa.job_launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SuppressWarnings("hideutilityclassconstructor")
@EnableConfigurationProperties(JobLauncherConfigurationProperties.class)
@SpringBootApplication
public class GridcapaJobLauncherApplication {

    public static void main(String[] args) {
        SpringApplication.run(GridcapaJobLauncherApplication.class, args);
    }

}
