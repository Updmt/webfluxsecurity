package com.updmtProjects.webfluxsecurity.UnitTest.serviceImplTest;

import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.repository.EventRepository;
import com.updmtProjects.webfluxsecurity.service.EventService.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplTest {

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private EventRepository eventRepository;

    @Test
    void createEvent_ok() {
        Event eventToSave = new Event();
        eventToSave.setCreated(LocalDateTime.now());
        eventToSave.setFileId(1L);
        eventToSave.setUserId(1L);

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setCreated(eventToSave.getCreated());
        savedEvent.setFileId(eventToSave.getFileId());
        savedEvent.setUserId(eventToSave.getUserId());

        when(eventRepository.save(any(Event.class))).thenReturn(Mono.just(savedEvent));

        Mono<Event> eventMono = eventService.createEvent(eventToSave);

        StepVerifier
                .create(eventMono)
                .expectNextMatches(savedEventResult ->
                        savedEventResult.getId().equals(1L) &&
                                savedEventResult.getFileId().equals(eventToSave.getFileId()) &&
                                savedEventResult.getUserId().equals(eventToSave.getUserId()))
                .verifyComplete();
    }

    @Test
    void createEvent_fail() {
        Event eventToSave = new Event();
        eventToSave.setCreated(LocalDateTime.now());
        eventToSave.setFileId(1L);
        eventToSave.setUserId(1L);

        when(eventRepository.save(any(Event.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<Event> eventMono = eventService.createEvent(eventToSave);

        StepVerifier
                .create(eventMono)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Database error"))
                .verify();
    }

    @Test
    void getEvent_ok() {
        Event event = new Event();
        event.setId(1L);
        event.setCreated(LocalDateTime.now());
        event.setFileId(1L);
        event.setUserId(1L);

        when(eventRepository.findById(1L)).thenReturn(Mono.just(event));

        Mono<Event> eventMono = eventService.getEventById(1L);

        StepVerifier
                .create(eventMono)
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void getEvent_throwException() {
        when(eventRepository.findById(1L)).thenReturn(Mono.empty());

        Mono<Event> eventMono = eventService.getEventById(1L);

        StepVerifier
                .create(eventMono)
                .expectError(CustomNotFoundException.class)
                .verify();
    }

    @Test
    void getAllEvents_ok() {
        Event event1 = new Event();
        event1.setId(1L);
        event1.setCreated(LocalDateTime.now());
        event1.setFileId(1L);
        event1.setUserId(1L);

        Event event2 = new Event();
        event2.setId(2L);
        event2.setCreated(LocalDateTime.now().minusDays(1));
        event2.setFileId(2L);
        event2.setUserId(1L);

        when(eventRepository.findAll()).thenReturn(Flux.just(event1, event2));

        StepVerifier
                .create(eventService.getAllEvents())
                .expectNext(event1)
                .expectNext(event2)
                .verifyComplete();
    }

    @Test
    void getAllEvents_empty() {
        when(eventRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier
                .create(eventService.getAllEvents())
                .expectError(CustomNotFoundException.class)
                .verify();
    }

    @Test
    void findByUserId_ok() {
        Long userId = 1L;
        Event event1 = new Event();
        event1.setId(1L);
        event1.setCreated(LocalDateTime.now());
        event1.setFileId(1L);
        event1.setUserId(userId);

        Event event2 = new Event();
        event2.setId(2L);
        event2.setCreated(LocalDateTime.now().minusDays(1));
        event2.setFileId(2L);
        event2.setUserId(userId);

        when(eventRepository.findByUserId(userId)).thenReturn(Flux.just(event1, event2));

        StepVerifier
                .create(eventService.findByUserId(userId))
                .expectNext(event1)
                .expectNext(event2)
                .verifyComplete();
    }

    @Test
    void findByUserId_throwException() {
        Long userId = 1L;
        when(eventRepository.findByUserId(userId)).thenReturn(Flux.empty());

        Flux<Event> eventFlux = eventService.findByUserId(userId);

        StepVerifier
                .create(eventFlux)
                .expectError(CustomNotFoundException.class)
                .verify();
    }
}
