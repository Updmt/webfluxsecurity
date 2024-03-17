package com.updmtProjects.webfluxsecurity.IntegrationTest;

import com.updmtProjects.webfluxsecurity.AbstractIntegrationTest;
import com.updmtProjects.webfluxsecurity.TestUtils;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.repository.EventRepository;
import com.updmtProjects.webfluxsecurity.repository.FileRepository;
import com.updmtProjects.webfluxsecurity.repository.UserRepository;
import com.updmtProjects.webfluxsecurity.rest.EventRestControllerV1;
import com.updmtProjects.webfluxsecurity.security.SecurityService;
import com.updmtProjects.webfluxsecurity.security.TokenDetails;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class EventControllerV1IntegrationTest extends AbstractIntegrationTest {

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
    }

    @AfterEach
    public void afterEach() {
        eventRepository.deleteAll().block();
        fileRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    public void getEvent_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        File testFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(testFile).block();

        Event testEvent = new Event(null, null, testFile.getId(), currentUser.getId());
        eventRepository.save(testEvent).block();

        String actualResponse = webTestClient
                .get()
                .uri(EventRestControllerV1.ROOT_URL + "/{eventId}", testEvent.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("EventControllerResponses/getEvent.json");
        assertThatJson(actualResponse)
                .whenIgnoringPaths("id", "fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToGetNonExistentEvent_404() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        File testFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(testFile).block();

        Event testEvent = new Event(null, null, testFile.getId(), currentUser.getId());
        eventRepository.save(testEvent).block();

        webTestClient
                .get()
                .uri(EventRestControllerV1.ROOT_URL + "/{eventId}", 100)
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void tryToGetEventsOfAnotherUserHavingRoleUser_403() {
        String usernameUserRole = "testUserRoleUser";
        String passwordUserRole = "testPassword";
        String usernameAdminRole = "testUser";

        User testRoleUser = new User(null, "testUserRoleUser", passwordEncoder.encode("testPassword"), UserRole.USER, "Test", "User", true, null, null, false);
        userRepository.save(testRoleUser).block();

        TokenDetails tokenDetails = securityService.authenticate(usernameUserRole, passwordUserRole).block();
        String bearerToken = tokenDetails.getToken();

        User testRoleAdmin = userRepository.findByUsername(usernameAdminRole).block();

        File testFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(testFile).block();

        Event testEvent = new Event(null, null, testFile.getId(), testRoleAdmin.getId());
        eventRepository.save(testEvent).block();

        webTestClient
                .get()
                .uri(EventRestControllerV1.ROOT_URL + "/{eventId}", testEvent.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isForbidden();

    }

    @Test
    public void getAllEventsFromDifferentUsers_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        File firstTestFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(firstTestFile).block();

        Event firstTestEvent = new Event(null, null, firstTestFile.getId(), currentUser.getId());
        eventRepository.save(firstTestEvent).block();

        User testRoleUser = new User(null, "testUserRoleUser", passwordEncoder.encode("testPassword"), UserRole.USER, "Test", "User", true, null, null, false);
        userRepository.save(testRoleUser).block();

        File secondTestFile = new File(null, "secondFileName", "SecondTestStorageLink", null, false);
        fileRepository.save(secondTestFile).block();

        Event secondTestEvent = new Event(null, null, secondTestFile.getId(), currentUser.getId());
        eventRepository.save(secondTestEvent).block();

        String actualResponse = webTestClient
                .get()
                .uri(EventRestControllerV1.ROOT_URL + "/all")
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("EventControllerResponses/getAllEvents.json");
        assertThatJson(actualResponse)
                .when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths("[*].id", "[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void getAllEventsFromOneUser_200() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        User currentUser = userRepository.findByUsername(username).block();

        File firstTestFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(firstTestFile).block();

        Event firstTestEvent = new Event(null, null, firstTestFile.getId(), currentUser.getId());
        eventRepository.save(firstTestEvent).block();

        File secondTestFile = new File(null, "secondFileName", "SecondTestStorageLink", null, false);
        fileRepository.save(secondTestFile).block();

        Event secondTestEvent = new Event(null, null, secondTestFile.getId(), currentUser.getId());
        eventRepository.save(secondTestEvent).block();

        String actualResponse = webTestClient
                .get()
                .uri(EventRestControllerV1.ROOT_URL + "/all/{userId}", currentUser.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("EventControllerResponses/getAllEvents.json");
        assertThatJson(actualResponse)
                .when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths("[*].id", "[*].fileGetDto.id")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToGetAllEventsFromDifferentUsers_404() {
        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        webTestClient
                .get()
                .uri(EventRestControllerV1.ROOT_URL + "/all")
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

}
