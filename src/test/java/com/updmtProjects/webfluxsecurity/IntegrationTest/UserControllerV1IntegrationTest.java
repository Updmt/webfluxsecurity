package com.updmtProjects.webfluxsecurity.IntegrationTest;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import com.updmtProjects.webfluxsecurity.TestUtils;
import com.updmtProjects.webfluxsecurity.dto.UserRequestDto;
import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.repository.EventRepository;
import com.updmtProjects.webfluxsecurity.repository.FileRepository;
import com.updmtProjects.webfluxsecurity.repository.UserRepository;
import com.updmtProjects.webfluxsecurity.rest.UserRestControllerV1;
import com.updmtProjects.webfluxsecurity.security.SecurityService;
import com.updmtProjects.webfluxsecurity.security.TokenDetails;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

public class UserControllerV1IntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    public void beforeEach() {
        User testUser = new User(null, "testUser", passwordEncoder.encode("testPassword"), UserRole.ADMIN, "Test", "User", true, null, null, false);
        userRepository.save(testUser).block();

        File testFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(testFile).block();

        Event testEvent = new Event(null, null, testFile.getId(), testUser.getId());
        eventRepository.save(testEvent).block();
    }

    @AfterEach
    public void afterEach() {
        eventRepository.deleteAll().block();
        fileRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    public void tryToGetUserWithEventAndFileWithoutId_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        String actualResponse = webTestClient
                .get()
                .uri(UserRestControllerV1.ROOT_URL)
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("UserControllerResponses/userWithEventAndFile.json");
        assertThatJson(actualResponse)
                .whenIgnoringPaths("id", "events[*].id", "events[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToGetNonExistedUser_404() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        webTestClient
                .get()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", 100)
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void tryToGetUserWithEventAndFileById_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        String actualResponse = webTestClient
                .get()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", currentUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("UserControllerResponses/userWithEventAndFile.json");
        assertThatJson(actualResponse)
                .whenIgnoringPaths("id", "events[*].id", "events[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToGetUserWithSeveralFiles_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        File testFile = new File(null, "testFileName2", "testStorageLink2", null, false);
        fileRepository.save(testFile).block();
        Event testEvent = new Event(null, null, testFile.getId(), currentUser.getId());
        eventRepository.save(testEvent).block();

        String actualResponse = webTestClient
                .get()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", currentUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("UserControllerResponses/userWithSeveralFiles.json");
        assertThatJson(actualResponse)
                .when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths("id", "events[*].id", "events[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToGetDifferentUserWithRoleUser_403() {
        String username = "testUserRoleUser";
        String password = "testPassword";

        User testUser = new User(null, "testUserRoleUser", passwordEncoder.encode("testPassword"), UserRole.USER, "Test", "User", true, null, null, false);
        userRepository.save(testUser).block();

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        webTestClient
                .get()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", testUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void tryToGetAllUsers_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User testUser = new User(null, "testUserSecond", passwordEncoder.encode("testPasswordSecond"), UserRole.USER, "TestSecond", "UserSecond", true, null, null, false);
        userRepository.save(testUser).block();
        File testFile = new File(null, "testFileName2", "testStorageLink2", null, false);
        fileRepository.save(testFile).block();
        Event testEvent = new Event(null, null, testFile.getId(), testUser.getId());
        eventRepository.save(testEvent).block();

        String actualResponse = webTestClient
                .get()
                .uri(UserRestControllerV1.ROOT_URL + "/all")
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("UserControllerResponses/getAllUsers.json");
        assertThatJson(actualResponse)
                .when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths("[*].id", "[*].events[*].id", "[*].events[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToCreateUser_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setPassword("password");
        userRequestDto.setUsername("testUserCreation");
        userRequestDto.setRole(UserRole.USER);
        userRequestDto.setFirstName("testUserCreation");
        userRequestDto.setLastName("testUserCreation");

        String actualResponse = webTestClient
                .post()
                .uri(UserRestControllerV1.ROOT_URL)
                .header("Authorization", "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRequestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("UserControllerResponses/createUser.json");
        assertThatJson(actualResponse)
                .whenIgnoringPaths("id", "created_at", "updated_at")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToUpdateUser_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setUsername("updatedUser");
        userUpdateDto.setRole(UserRole.USER);
        userUpdateDto.setFirstName("updatedUser");
        userUpdateDto.setLastName("updatedUser");

        User currentUser = userRepository.findByUsername(username).block();

        String actualResponse = webTestClient
                .put()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", currentUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userUpdateDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("UserControllerResponses/updateUser.json");
        assertThatJson(actualResponse)
                .when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths("id", "updated_at", "events[*].id", "events[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToUpdateUserWhichNonExist_404() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        UserUpdateDto userUpdateDto = new UserUpdateDto();

        webTestClient
                .put()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", 100)
                .header("Authorization", "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userUpdateDto)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void tryToUpdateAnotherUserWithUserRole_403() {
        String usernameAdmin = "testUser";
        String testUserPassword = "testPasswordSecond";

        User testUser = new User(null, "testUserSecond", passwordEncoder.encode(testUserPassword), UserRole.USER, "TestSecond", "UserSecond", true, null, null, false);
        userRepository.save(testUser).block();

        TokenDetails tokenDetails = securityService.authenticate(testUser.getUsername(), testUserPassword).block();
        String bearerToken = tokenDetails.getToken();

        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setUsername("updatedUserAdmin");
        userUpdateDto.setRole(UserRole.USER);
        userUpdateDto.setFirstName("updatedUserAdmin");
        userUpdateDto.setLastName("updatedUserAdmin");

        User currentUser = userRepository.findByUsername(usernameAdmin).block();

        webTestClient
                .put()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", currentUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userUpdateDto)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void tryToDeleteUser_204() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        webTestClient
                .delete()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", currentUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();

        User userAfterDelete = userRepository.findByUsername(username).block();
        Assertions.assertTrue(userAfterDelete.isDeleted());
    }

    @Test
    public void tryToDeleteNonExistentUser_404() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        webTestClient
                .delete()
                .uri(UserRestControllerV1.ROOT_URL + "/{id}", 100)
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }
}
