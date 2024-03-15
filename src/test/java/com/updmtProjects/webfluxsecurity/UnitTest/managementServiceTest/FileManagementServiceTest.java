package com.updmtProjects.webfluxsecurity.UnitTest.managementServiceTest;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.updmtProjects.webfluxsecurity.dto.FileDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.EventService.EventService;
import com.updmtProjects.webfluxsecurity.service.FileManagementService;
import com.updmtProjects.webfluxsecurity.service.FileService.FileService;
import com.updmtProjects.webfluxsecurity.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

@SpringBootTest
public class FileManagementServiceTest {

    @SpyBean
    private FileManagementService fileManagementService;
    @MockBean
    private StorageService storageService;
    @MockBean
    private FileService fileService;
    @MockBean
    private EventService eventService;

    @Test
    void uploadAndRegisterFile_ok() {
        FilePart filePartMock = mock(FilePart.class);
        when(filePartMock.filename()).thenReturn("testFile.txt");

        File file = new File();
        file.setFileName(filePartMock.filename());
        file.setCreated(LocalDateTime.now());

        Event event = new Event();
        event.setCreated(LocalDateTime.now());
        event.setFileId(1L);
        event.setUserId(1L);

        when(fileService.createFile(any(File.class))).thenReturn(Mono.just(file));
        when(storageService.uploadFile(any(FilePart.class))).thenReturn(Mono.just("File uploaded"));
        when(eventService.createEvent(any(Event.class))).thenReturn(Mono.just(event));

        Mono<FileDto> result = fileManagementService.uploadAndRegisterFile(filePartMock, 1L);

        StepVerifier.create(result)
                .assertNext(uploadDto -> assertEquals("testFile.txt", uploadDto.getFileName()))
                .verifyComplete();
    }

    @Test
    void getFileDependingOnRoleAdminTest_ok() {
        Long fileId = 1L;
        Long userId = 1L;

        Authentication authenticationMock = mock(Authentication.class);

        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.ADMIN));
        customPrincipal.setId(userId);
        customPrincipal.setName("UserAdminTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        doReturn(Mono.empty()).when(fileManagementService).getFile(fileId);

        // Вызываем метод на spy объекте
        fileManagementService.getFileDependingOnUserRole(authenticationMock, fileId).block();

        //был вызван метод getFile, а getFileByFileIdAndUserId - нет
        verify(fileManagementService).getFile(fileId);
        verify(fileManagementService, never()).getFileByFileIdAndUserId(userId, fileId);
    }

    @Test
    void getFileDependingOnRoleUserTest_ok() {
        Long fileId = 1L;
        Long userId = 1L;

        Authentication authenticationMock = mock(Authentication.class);

        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.USER));
        customPrincipal.setId(userId);
        customPrincipal.setName("UserUserTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);
        doReturn(Mono.empty()).when(fileManagementService).getFileByFileIdAndUserId(userId, fileId);

        StepVerifier
                .create(fileManagementService.getFileDependingOnUserRole(authenticationMock, fileId))
                .verifyComplete();

        verify(fileManagementService, never()).getFile(fileId);
        verify(fileManagementService).getFileByFileIdAndUserId(userId, fileId);
    }

    @Test
    void getFileTest_ok() {
        Long fileId = 1L;
        String presignedUrl = "https://example.com/presigned-url";
        String fileName = "testFile.txt";
        String bucketName = "testBucket";

        ReflectionTestUtils.setField(fileManagementService, "bucketName", bucketName);

        File file = File.builder()
                .id(fileId)
                .fileName(fileName)
                .storageLink(presignedUrl)
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        when(fileService.getFileById(fileId)).thenReturn(Mono.just(file));

        Mono<FileDto> fileGetDtoMono = fileManagementService.getFile(fileId);

        StepVerifier
                .create(fileGetDtoMono)
                .assertNext(dto -> {
                    assertEquals(fileName, dto.getFileName());
                    assertEquals(presignedUrl, dto.getStorageLink());
                })
                .verifyComplete();
    }

    @Test
    void getFileByFileIdAndUserIdTest_ok() {
        Long fileId = 1L;
        Long userId = 1L;
        String presignedUrl = "https://example.com/presigned-url";
        String fileName = "testFile.txt";
        String bucketName = "testBucket";

        ReflectionTestUtils.setField(fileManagementService, "bucketName", bucketName);

        File file = File.builder()
                .id(fileId)
                .fileName(fileName)
                .storageLink(presignedUrl)
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        Event event1 = new Event();
        event1.setCreated(LocalDateTime.now());
        event1.setFileId(file.getId());
        event1.setUserId(userId);

        Event event2 = new Event();
        event2.setCreated(LocalDateTime.now());
        event2.setFileId(2L);
        event2.setUserId(userId);

        FileDto fileGetDto = new FileDto();
        fileGetDto.setId(file.getId());
        fileGetDto.setFileName(file.getFileName());
        fileGetDto.setCreated(file.getCreated());
        fileGetDto.setDeleted(file.isDeleted());
        fileGetDto.setStorageLink(presignedUrl);

        when(fileService.getFileById(fileId)).thenReturn(Mono.just(file));
        when(eventService.findByUserId(userId)).thenReturn(Flux.just(event1, event2));

        Mono<FileDto> fileGetDtoMono = fileManagementService.getFileByFileIdAndUserId(userId, fileId);

        StepVerifier.create(fileGetDtoMono)
                .expectNextMatches(fileGetDtoResult ->
                        fileGetDtoResult.getFileName().equals(fileGetDto.getFileName()) &&
                                fileGetDtoResult.getStorageLink().equals(fileGetDto.getStorageLink()))
                .verifyComplete();
    }

    @Test
    void getFileByFileIdAndUserIdTest_throwException() {
        Long fileId = 1L;
        Long userId = 1L;
        String presignedUrl = "https://example.com/presigned-url";
        String fileName = "testFile.txt";
        String bucketName = "testBucket";

        ReflectionTestUtils.setField(fileManagementService, "bucketName", bucketName);

        File file = File.builder()
                .id(fileId)
                .fileName(fileName)
                .storageLink(presignedUrl)
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        Event event1 = new Event();
        event1.setCreated(LocalDateTime.now());
        event1.setFileId(3L);
        event1.setUserId(userId);

        Event event2 = new Event();
        event2.setCreated(LocalDateTime.now());
        event2.setFileId(2L);
        event2.setUserId(userId);

        when(fileService.getFileById(fileId)).thenReturn(Mono.just(file));
        when(eventService.findByUserId(userId)).thenReturn(Flux.just(event1, event2));

        Mono<FileDto> resultMono = fileManagementService.getFileByFileIdAndUserId(userId, fileId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof CustomAccessDeniedException &&
                        throwable.getMessage().equals("Access denied for file ID " + fileId + " for user ID " + userId))
                .verify();
    }

    @Test
    void getUserFilesDependingOnRoleWithRoleUserTest_ok() {
        Long fileId = 1L;
        Long userId = 1L;
        String presignedUrl = "https://example.com/presigned-url";
        String fileName = "testFile.txt";
        String bucketName = "testBucket";

        ReflectionTestUtils.setField(fileManagementService, "bucketName", bucketName);

        Authentication authenticationMock = mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.USER));
        customPrincipal.setId(userId);
        customPrincipal.setName("UserUserTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        Event event = new Event();
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(userId);

        File file = File.builder()
                .id(fileId)
                .fileName(fileName)
                .storageLink(presignedUrl)
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        when(eventService.findByUserId(userId)).thenReturn(Flux.just(event));
        when(fileService.getFileById(event.getFileId())).thenReturn(Mono.just(file));

        Flux<FileDto> fileGetDtoFlux = fileManagementService.getUserFilesDependingOnRole(authenticationMock, userId);

        StepVerifier
                .create(fileGetDtoFlux)
                .assertNext(dto -> {
                    assertEquals(fileName, dto.getFileName());
                    assertEquals(presignedUrl, dto.getStorageLink());
                })
                .verifyComplete();
    }

    @Test
    void getUserFilesDependingOnRoleWithRoleUserTest_throwException() {
        Long requestedUserId = 2L;
        Long authorisedUserId = 1L;
        String role = String.valueOf(UserRole.USER);

        Authentication authenticationMock = mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(role);
        customPrincipal.setId(authorisedUserId);
        customPrincipal.setName("UserUserTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        Flux<FileDto> resultFlux = fileManagementService.getUserFilesDependingOnRole(authenticationMock, requestedUserId);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(throwable -> throwable instanceof CustomAccessDeniedException &&
                        throwable.getMessage().equals("You cannot get other user files"))
                .verify();
    }
}
