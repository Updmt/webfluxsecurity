package com.updmtProjects.webfluxsecurity;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.33");

    static {
        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:mysql://%s:%s/%s",
                mySQLContainer.getHost(),
                mySQLContainer.getFirstMappedPort(),
                mySQLContainer.getDatabaseName()));
        registry.add("spring.r2dbc.username", mySQLContainer::getUsername);
        registry.add("spring.r2dbc.password", mySQLContainer::getPassword);

        registry.add("spring.flyway.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.flyway.user", mySQLContainer::getUsername);
        registry.add("spring.flyway.password", mySQLContainer::getPassword);
    }
}
