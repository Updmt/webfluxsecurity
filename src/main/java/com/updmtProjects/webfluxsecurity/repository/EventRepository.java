package com.updmtProjects.webfluxsecurity.repository;

import com.updmtProjects.webfluxsecurity.entity.Event;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface EventRepository extends R2dbcRepository<Event, Long> {
    Flux<Event> findByUserId(Long userId);
}
