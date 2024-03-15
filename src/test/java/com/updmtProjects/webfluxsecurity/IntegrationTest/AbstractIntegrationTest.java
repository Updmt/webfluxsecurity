package com.updmtProjects.webfluxsecurity.IntegrationTest;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.33");

    static {
        mySQLContainer.start();
    }

    @BeforeAll
    static void setup() {
        System.setProperty("spring.r2dbc.url", String.format("r2dbc:mysql://%s:%s/%s",
                mySQLContainer.getHost(),
                mySQLContainer.getFirstMappedPort(),
                mySQLContainer.getDatabaseName()));
        System.setProperty("spring.r2dbc.username", mySQLContainer.getUsername());
        System.setProperty("spring.r2dbc.password", mySQLContainer.getPassword());

        Flyway flyway = Flyway.configure()
                .dataSource(mySQLContainer.getJdbcUrl(), mySQLContainer.getUsername(), mySQLContainer.getPassword())
                .load();
        flyway.migrate();
    }
}
