package com.updmtProjects.webfluxsecurity.UnitTest.managementServiceTest;

import com.updmtProjects.webfluxsecurity.dto.EventDto;
import com.updmtProjects.webfluxsecurity.dto.FileDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.EventManagementService;
import com.updmtProjects.webfluxsecurity.service.EventService.EventService;
import com.updmtProjects.webfluxsecurity.service.FileManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class EventManagementTest {

    @SpyBean
    private EventManagementService eventManagementService;

    @MockBean
    private EventService eventService;

    @MockBean
    private FileManagementService fileManagementService;

    @Test
    void getEventDependingOnUserRoleTest_ok() {
        Long eventId = 1L;
        Long userId = 1L;
        Authentication authenticationMock = mock(Authentication.class);

        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.ADMIN));
        customPrincipal.setId(userId);
        customPrincipal.setName("UserAdminTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        doReturn(Mono.empty()).when(eventManagementService).getEventById(eventId);

        eventManagementService.getEventDependingOnUserRole(authenticationMock, eventId).block();

        verify(eventManagementService).getEventById(eventId);
        verify(eventManagementService, never()).getEventsByUserId(userId);
    }

    @Test
    void getEventByIdTest_ok() {
        Long eventId = 1L;
        Long userId = 1L;
        Long fileId = 1L;
        String fileName = "testFile.txt";
        String presignedUrl = "https://example.com/presigned-url";

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(userId);

        File file = new File();
        file.setId(fileId);
        file.setFileName(fileName);
        file.setCreated(LocalDateTime.now());

        FileDto fileGetDto = new FileDto();
        fileGetDto.setId(file.getId());
        fileGetDto.setFileName(file.getFileName());
        fileGetDto.setCreated(file.getCreated());
        fileGetDto.setDeleted(file.isDeleted());
        fileGetDto.setStorageLink(presignedUrl);

        when(eventService.getEventById(eventId)).thenReturn(Mono.just(event));
        when(fileManagementService.getFile(fileId)).thenReturn(Mono.just(fileGetDto));

        Mono<EventDto> eventDtoMono = eventManagementService.getEventById(eventId);

        StepVerifier
                .create(eventDtoMono)
                .expectNextMatches(eventDtoResult ->
                        eventDtoResult.getFileGetDto().getFileName().equals(file.getFileName()) &&
                                eventDtoResult.getFileGetDto().getId().equals(file.getId()) &&
                                eventDtoResult.getId().equals(eventId))
                .verifyComplete();
    }

    @Test
    void getEventByUserIdAndEventIdTest_ok() {
        Long eventId = 1L;
        Long userId = 1L;
        Long fileId = 1L;
        String fileName = "testFile.txt";
        String presignedUrl = "https://example.com/presigned-url";

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(userId);

        File file = new File();
        file.setId(fileId);
        file.setFileName(fileName);
        file.setCreated(LocalDateTime.now());

        FileDto fileGetDto = new FileDto();
        fileGetDto.setId(file.getId());
        fileGetDto.setFileName(file.getFileName());
        fileGetDto.setCreated(file.getCreated());
        fileGetDto.setDeleted(file.isDeleted());
        fileGetDto.setStorageLink(presignedUrl);

        when(eventService.getEventById(eventId)).thenReturn(Mono.just(event));
        when(fileManagementService.getFile(fileId)).thenReturn(Mono.just(fileGetDto));

        Mono<EventDto> eventDtoMono = eventManagementService.getEventById(eventId);

        StepVerifier
                .create(eventDtoMono)
                .expectNextMatches(eventDtoResult ->
                        eventDtoResult.getFileGetDto().getFileName().equals(file.getFileName()) &&
                                eventDtoResult.getFileGetDto().getId().equals(file.getId()) &&
                                eventDtoResult.getId().equals(eventId))
                .verifyComplete();
    }

    @Test
    void getEventByUserIdAndEventIdTest_eventBelongsAnotherUser_throwException() {
        Long eventId = 1L;
        Long userId = 1L;
        Long fileId = 1L;
        Long nonExistentEventUserId = 100L;
        String fileName = "testFile.txt";

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(nonExistentEventUserId);

        File file = new File();
        file.setId(fileId);
        file.setFileName(fileName);
        file.setCreated(LocalDateTime.now());

        when(eventService.getEventById(eventId)).thenReturn(Mono.just(event));

        Mono<EventDto> eventDtoMono = eventManagementService.getEventByUserIdAndEventId(eventId, userId);

        StepVerifier
                .create(eventDtoMono)
                .expectErrorMatches(throwable -> throwable instanceof CustomAccessDeniedException &&
                        throwable.getMessage().equals("Access Denied. Event belongs to another user"))
                .verify();
    }

    @Test
    void getEventByUserIdAndEventIdTest_noSuchEventInDB_throwException() {
        Long eventId = 1L;
        Long userId = 1L;
        Long fileId = 1L;

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(userId);

        when(eventService.getEventById(eventId)).thenReturn(Mono.empty());

        Mono<EventDto> eventDtoMono = eventManagementService.getEventByUserIdAndEventId(eventId, userId);

        StepVerifier
                .create(eventDtoMono)
                .expectErrorMatches(throwable -> throwable instanceof CustomNotFoundException &&
                        throwable.getMessage().equals("Event not found"))
                .verify();
    }

    @Test
    void getAllEventsTest_ok() {
        Long eventId1 = 1L;
        Long userId1 = 1L;
        Long fileId1 = 1L;
        String fileName1 = "testFile1.txt";
        String presignedUrl1 = "https://example.com/presigned-url1";

        Event event1 = new Event();
        event1.setId(eventId1);
        event1.setCreated(LocalDateTime.now());
        event1.setFileId(fileId1);
        event1.setUserId(userId1);

        FileDto fileGetDto1 = new FileDto();
        fileGetDto1.setId(fileId1);
        fileGetDto1.setFileName(fileName1);
        fileGetDto1.setCreated(LocalDateTime.now());
        fileGetDto1.setStorageLink(presignedUrl1);

        when(eventService.getAllEvents()).thenReturn(Flux.just(event1));
        when(fileManagementService.getFile(fileId1)).thenReturn(Mono.just(fileGetDto1));

        Flux<EventDto> eventDtoFlux = eventManagementService.getAllEvents();

        StepVerifier
                .create(eventDtoFlux)
                .expectNextMatches(eventDtoResult ->
                        eventDtoResult.getFileGetDto().getFileName().equals(fileName1) &&
                                eventDtoResult.getFileGetDto().getId().equals(fileId1) &&
                                eventDtoResult.getId().equals(eventId1))
                .verifyComplete();
    }

    @Test
    void getAllEventsTest_noEventsFound() {
        when(eventService.getAllEvents()).thenReturn(Flux.empty());

        Flux<EventDto> eventDtoFlux = eventManagementService.getAllEvents();

        StepVerifier
                .create(eventDtoFlux)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void getUserEventsDependingOnRole_AccessDenied() {
        Long requestedUserId = 2L;
        Long authorisedUserId = 1L;
        String role = String.valueOf(UserRole.USER);

        Authentication authenticationMock = mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(role);
        customPrincipal.setId(authorisedUserId);
        customPrincipal.setName("UserUserTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        Flux<EventDto> resultFlux = eventManagementService.getUserEventsDependingOnRole(authenticationMock, requestedUserId);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(throwable -> throwable instanceof CustomAccessDeniedException)
                .verify();
    }

    @Test
    void getUserEventsDependingOnRole_ok() {
        Long requestedUserId = 2L;
        Long authorisedUserId = 1L;
        String role = String.valueOf(UserRole.ADMIN);
        Long eventId = 1L;
        Long fileId = 1L;
        Long nonExistentEventUserId = 100L;
        String fileName = "testFile.txt";
        String presignedUrl = "https://example.com/presigned-url";

        Authentication authenticationMock = mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(role);
        customPrincipal.setId(authorisedUserId);
        customPrincipal.setName("UserUserTest");

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(nonExistentEventUserId);

        File file = new File();
        file.setId(fileId);
        file.setFileName(fileName);
        file.setCreated(LocalDateTime.now());

        FileDto fileGetDto = new FileDto();
        fileGetDto.setId(file.getId());
        fileGetDto.setFileName(file.getFileName());
        fileGetDto.setCreated(file.getCreated());
        fileGetDto.setDeleted(file.isDeleted());
        fileGetDto.setStorageLink(presignedUrl);

        EventDto eventDto = new EventDto();
        eventDto.setId(event.getId());
        eventDto.setCreated(event.getCreated());
        eventDto.setFileGetDto(fileGetDto);

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        doReturn(Flux.just(eventDto)).when(eventManagementService).getEventsByUserId(requestedUserId);

        eventManagementService.getUserEventsDependingOnRole(authenticationMock, requestedUserId).blockFirst();

        verify(eventManagementService).getEventsByUserId(requestedUserId);

    }

    @Test
    void getEventsByUserId_EventsFound() {
        Long userId = 1L;
        Long eventId = 1L;
        Long fileId = 1L;
        String fileName = "testFile.txt";
        String presignedUrl = "https://example.com/presigned-url";

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(userId);

        FileDto fileGetDto = new FileDto();
        fileGetDto.setId(fileId);
        fileGetDto.setFileName(fileName);
        fileGetDto.setCreated(LocalDateTime.now());
        fileGetDto.setStorageLink(presignedUrl);

        when(eventService.findByUserId(userId)).thenReturn(Flux.just(event));
        when(fileManagementService.getFile(fileId)).thenReturn(Mono.just(fileGetDto));

        Flux<EventDto> resultFlux = eventManagementService.getEventsByUserId(userId);

        StepVerifier.create(resultFlux)
                .expectNextMatches(eventDtoResult ->
                        eventDtoResult.getId().equals(eventId) &&
                                eventDtoResult.getFileGetDto().getFileName().equals(fileName) &&
                                eventDtoResult.getFileGetDto().getStorageLink().equals(presignedUrl))
                .verifyComplete();
    }

    @Test
    void getEventsByUserId_EventsNotFound() {
        Long userId = 2L;

        when(eventService.findByUserId(userId)).thenReturn(Flux.empty());

        Flux<EventDto> resultFlux = eventManagementService.getEventsByUserId(userId);

        StepVerifier.create(resultFlux)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomNotFoundException &&
                                throwable.getMessage().equals("No files found for user " + userId))
                .verify();
    }

}
