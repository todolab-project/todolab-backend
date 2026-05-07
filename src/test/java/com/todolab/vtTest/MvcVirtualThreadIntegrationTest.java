package com.todolab.vtTest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = MvcVirtualThreadIntegrationTest.TestApp.class,
        properties = "spring.threads.virtual.enabled=true"
)
class MvcVirtualThreadIntegrationTest {

    @LocalServerPort
    int port;

    @Test
    void mvc_request_thread_should_be_virtual_thread() {
        RestTemplate rt = new RestTemplate();

        String url = "http://localhost:" + port + "/__vt/probe";
        String threadInfo = rt.getForObject(url, String.class);

        System.out.println("threadInfo=" + threadInfo);
        assertThat(threadInfo).containsIgnoringCase("virtual");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceInitializationAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class
    })
    @Import(VtTestEndPointConfig.class)
    static class TestApp {
    }
}
