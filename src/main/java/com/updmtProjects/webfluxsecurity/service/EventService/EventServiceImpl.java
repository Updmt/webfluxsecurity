package com.updmtProjects.webfluxsecurity.service.EventService;

import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.repository.EventRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    public EventServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Event> createEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public Mono<Event> getEventById(Long id) {
        return eventRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomNotFoundException("Event not found")));
    }

    @Override
    public Flux<Event> getAllEvents() {
        return eventRepository.findAll()
                .switchIfEmpty(Mono.error(new CustomNotFoundException("Events not found")));
    }

    @Override
    public Flux<Event> findByUserId(Long userId) {
        return eventRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new CustomNotFoundException("User with id " + userId + " not found")));
    }
}
