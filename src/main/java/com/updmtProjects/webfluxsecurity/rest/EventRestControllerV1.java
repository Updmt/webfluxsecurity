package com.updmtProjects.webfluxsecurity.rest;

import com.updmtProjects.webfluxsecurity.dto.EventDto;
import com.updmtProjects.webfluxsecurity.service.EventManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
@RequestMapping(EventRestControllerV1.ROOT_URL)
public class EventRestControllerV1 {

    public final static String ROOT_URL = "/api/v1/events";

    private final EventManagementService eventManagementService;

    @GetMapping("/{eventId}")
    public Mono<EventDto> getEvent(Authentication authentication, @PathVariable Long eventId) {
        return eventManagementService.getEventDependingOnUserRole(authentication, eventId);
    }

    @GetMapping("/all")
    public Flux<EventDto> getAllEvents() {
        return eventManagementService.getAllEvents();
    }

    @GetMapping("/all/{userId}")
    public Flux<EventDto> getUserEvents(Authentication authentication, @PathVariable Long userId) {
        return eventManagementService.getUserEventsDependingOnRole(authentication, userId);
    }

}
