package com.updmtProjects.webfluxsecurity.service.EventService;

import com.updmtProjects.webfluxsecurity.entity.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventService {

    Mono<Event> createEvent(Event event);
    Mono<Event> getEventById(Long id);
    Flux<Event> getAllEvents();
    Flux<Event> findByUserId(Long userId);
}
