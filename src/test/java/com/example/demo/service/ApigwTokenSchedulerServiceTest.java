package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    classes = ApigwTokenSchedulerServiceTest.TestConfig.class,
    properties = "fixedDelayForApigwToken.in.milliseconds=2000"
)
public class ApigwTokenSchedulerServiceTest {

    @Configuration
    @EnableScheduling
    @Import(ApigwTokenSchedulerService.class)
    @ImportAutoConfiguration({
        PropertyPlaceholderAutoConfiguration.class,
        TaskSchedulingAutoConfiguration.class
    })
    static class TestConfig {
    }

    @MockitoSpyBean
    private ApigwTokenSchedulerService apigwTokenSchedulerService;

    @Test
    public void testScheduledTaskRunsPeriodically() {
        // Wait up to 6 seconds to ensure the task (configured for 2 seconds) runs at least twice.
        await()
            .atMost(Duration.ofSeconds(6))
            .untilAsserted(() -> verify(apigwTokenSchedulerService, atLeast(2)).updateApigwToken());
    }
}
