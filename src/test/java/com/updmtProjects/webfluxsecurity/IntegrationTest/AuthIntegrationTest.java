package com.updmtProjects.webfluxsecurity.IntegrationTest;

import com.updmtProjects.webfluxsecurity.dto.AuthRequestDto;
import com.updmtProjects.webfluxsecurity.dto.AuthResponseDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.repository.UserRepository;
import com.updmtProjects.webfluxsecurity.rest.AuthRestControllerV1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void beforeEach() {
        User testUser = new User(null, "testUser", passwordEncoder.encode("testPassword"), UserRole.ADMIN, "Test", "User", true, null, null, false);
        userRepository.save(testUser).block();
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll().block();
    }

    @Test
    public void tryToLoginWithValidUsername_200() {
        AuthRequestDto requestDto = new AuthRequestDto("testUser", "testPassword");

        webTestClient
                .post()
                .uri(AuthRestControllerV1.ROOT_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDto.class)
                .consumeWith(response -> {
                    AuthResponseDto responseBody = response.getResponseBody();
                    assertNotNull(responseBody.getToken());
                    assertFalse(responseBody.getToken().isEmpty());
                });
    }

    @Test
    public void tryToLoginWithInvalidUsername_400() {
        AuthRequestDto requestDto = new AuthRequestDto("invalidTestUser", "testPassword");

        webTestClient
                .post()
                .uri(AuthRestControllerV1.ROOT_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void tryToLoginWithInvalidPassword_400() {
        AuthRequestDto requestDto = new AuthRequestDto("testUser", "invalidPassword");

        webTestClient
                .post()
                .uri(AuthRestControllerV1.ROOT_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void tryToLoginWhenUserIdDisabled_400() {
        User testUser = new User(null, "testUserDisabled", passwordEncoder.encode("testPasswordDisabled"), UserRole.ADMIN, "Test", "User", false, LocalDateTime.now(), LocalDateTime.now(), false);
        userRepository.save(testUser).block();
        AuthRequestDto requestDto = new AuthRequestDto("testUserDisabled", "testPasswordDisabled");

        webTestClient
                .post()
                .uri(AuthRestControllerV1.ROOT_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

}