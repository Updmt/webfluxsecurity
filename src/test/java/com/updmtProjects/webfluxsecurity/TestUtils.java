package com.updmtProjects.webfluxsecurity;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestUtils {

    public static String getExpectedResponse(String pathToFile) {
        try {
            return new String(Files.readAllBytes(Paths.get(new ClassPathResource(pathToFile).getURI())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
