package com.updmtProjects.webfluxsecurity.IntegrationTest;

import com.updmtProjects.webfluxsecurity.AbstractIntegrationTest;
import com.updmtProjects.webfluxsecurity.TestUtils;
import com.updmtProjects.webfluxsecurity.dto.FileDto;
import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.repository.EventRepository;
import com.updmtProjects.webfluxsecurity.repository.FileRepository;
import com.updmtProjects.webfluxsecurity.repository.UserRepository;
import com.updmtProjects.webfluxsecurity.rest.FileRestControllerV1;
import com.updmtProjects.webfluxsecurity.security.SecurityService;
import com.updmtProjects.webfluxsecurity.security.TokenDetails;
import com.updmtProjects.webfluxsecurity.service.StorageService;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;

public class FileControllerV1IntegrationTest extends AbstractIntegrationTest {

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

    @MockBean
    private StorageService storageService;

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
    public void uploadFile_200() {
        Mockito.when(storageService.uploadFile(any(FilePart.class))).thenReturn(Mono.just("File uploaded successfully"));

        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "Hello World")
                .header("Content-Disposition", "form-data; name=file; filename=testfile.txt");

        String actualResponse = webTestClient
                .post()
                .uri(FileRestControllerV1.ROOT_URL + "/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + bearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("FileControllerResponses/getFile.json");

        assertThatJson(actualResponse)
                .whenIgnoringPaths("id", "created")
                .isEqualTo(expectedResponse);
    }


    @Test
    public void getFile_200() {
        File testFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(testFile).block();

        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        webTestClient
                .get()
                .uri(FileRestControllerV1.ROOT_URL + "/{fileId}", testFile.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void tryToGetNonExistedFile_404() {
        File testFile = new File(null, "testFileName", "testStorageLink", null, false);
        fileRepository.save(testFile).block();

        String username = "testUser";
        String password = "testPassword";

        TokenDetails tokenDetails = securityService.authenticate(username, password).block();
        String bearerToken = tokenDetails.getToken();

        webTestClient
                .get()
                .uri(FileRestControllerV1.ROOT_URL + "/{fileId}", 100)
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void uploadAndGetFileForUserRoleUser_200() {
        Mockito.when(storageService.uploadFile(any(FilePart.class))).thenReturn(Mono.just("File uploaded successfully"));

        String password = "testPassword";

        User testUser = new User(null, "testUserRoleUser", passwordEncoder.encode(password), UserRole.USER, "Test", "User", true, null, null, false);
        userRepository.save(testUser).block();

        TokenDetails tokenDetails = securityService.authenticate(testUser.getUsername(), password).block();
        String bearerToken = tokenDetails.getToken();

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "Hello World")
                .header("Content-Disposition", "form-data; name=file; filename=testfile.txt");

        FileDto fileDto = webTestClient
                .post()
                .uri(FileRestControllerV1.ROOT_URL + "/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + bearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto.class).returnResult().getResponseBody();

        String actualResponse = webTestClient
                .get()
                .uri(FileRestControllerV1.ROOT_URL + "/{fileId}", fileDto.getId())
                .header("Authorization", "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("FileControllerResponses/getFile.json");
        assertThatJson(actualResponse)
                .whenIgnoringPaths("id", "created")
                .isEqualTo(expectedResponse);
    }

    @Test
    public void tryToUploadAndGetFileForAdminRoleUser_403() {
        Mockito.when(storageService.uploadFile(any(FilePart.class))).thenReturn(Mono.just("File uploaded successfully"));

        String adminUsername = "testUser";
        String adminPassword = "testPassword";

        TokenDetails adminTokenDetails = securityService.authenticate(adminUsername, adminPassword).block();
        String adminBearerToken = adminTokenDetails.getToken();

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", "Hello World")
                .header("Content-Disposition", "form-data; name=file; filename=testfile.txt");

        //создаем файл для админа
        FileDto fileDtoForAdmin = webTestClient
                .post()
                .uri(FileRestControllerV1.ROOT_URL + "/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + adminBearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileDto.class).returnResult().getResponseBody();

        String userPassword = "testPassword";

        User testUser = new User(null, "testUserRoleUser", passwordEncoder.encode(userPassword), UserRole.USER, "Test", "User", true, null, null, false);
        userRepository.save(testUser).block();

        TokenDetails userTokenDetails = securityService.authenticate(testUser.getUsername(), userPassword).block();
        String userBearerToken = userTokenDetails.getToken();

        //создаем файл для юзера
        webTestClient
                .post()
                .uri(FileRestControllerV1.ROOT_URL + "/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + userBearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk();

        //пытаемся получить файл админ, зайдя под пользователем с ролью USER
        webTestClient
                .get()
                .uri(FileRestControllerV1.ROOT_URL + "/{fileId}", fileDtoForAdmin.getId())
                .header("Authorization", "Bearer " + userBearerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    public void createAdminAndUserFilesAndGet_200() {
        Mockito.when(storageService.uploadFile(any(FilePart.class))).thenReturn(Mono.just("File uploaded successfully"));

        String adminUsername = "testUser";
        String adminPassword = "testPassword";

        TokenDetails adminTokenDetails = securityService.authenticate(adminUsername, adminPassword).block();
        String adminBearerToken = adminTokenDetails.getToken();

        MultipartBodyBuilder bodyBuilderFirst = new MultipartBodyBuilder();
        bodyBuilderFirst.part("file", "Hello World")
                .header("Content-Disposition", "form-data; name=file; filename=firstTestFile.txt");

        //создаем первый файл для админа
        webTestClient
                .post()
                .uri(FileRestControllerV1.ROOT_URL + "/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + adminBearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilderFirst.build()))
                .exchange()
                .expectStatus().isOk();

        MultipartBodyBuilder bodyBuilderSecond = new MultipartBodyBuilder();
        bodyBuilderSecond.part("file", "Hello World")
                .header("Content-Disposition", "form-data; name=file; filename=secondTestFile.txt");

        //создаем второй файл для админа
        webTestClient
                .post()
                .uri(FileRestControllerV1.ROOT_URL + "/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + adminBearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilderSecond.build()))
                .exchange()
                .expectStatus().isOk();

        User currentUser = userRepository.findByUsername(adminUsername).block();
        String actualResponse = webTestClient
                .get()
                .uri(FileRestControllerV1.ROOT_URL + "/all/{userId}", currentUser.getId())
                .header("Authorization", "Bearer " + adminBearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        String expectedResponse = TestUtils.getExpectedResponse("FileControllerResponses/getAllFiles.json");

        assertThatJson(actualResponse)
                .when(Option.IGNORING_ARRAY_ORDER)
                .whenIgnoringPaths("[*].id", "[*].created")
                .isEqualTo(expectedResponse);
    }
}
