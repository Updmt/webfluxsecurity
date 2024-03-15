package com.updmtProjects.webfluxsecurity.service;

import com.updmtProjects.webfluxsecurity.dto.EventDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.mapper.EventMapper;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.EventService.EventService;
import com.updmtProjects.webfluxsecurity.util.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EventManagementService {

    private final EventService eventService;
    private final EventMapper eventMapper;
    private final FileManagementService fileManagementService;

    public Mono<EventDto> getEventDependingOnUserRole(Authentication authentication, Long eventId) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long userId = customPrincipal.getId();
        String userRole = customPrincipal.getRole();

        if (RoleConstants.ADMIN.equals(userRole) || RoleConstants.MODERATOR.equals(userRole)) {
            return getEventById(eventId);
        } else {
            return getEventByUserIdAndEventId(userId, eventId);
        }
    }

    public Mono<EventDto> getEventById(Long id) {
        return eventService.getEventById(id)
                .flatMap(this::mapEventToDtoWithFile);
    }

    public Mono<EventDto> getEventByUserIdAndEventId(Long userId, Long eventId) {
        return eventService.getEventById(eventId)
                .flatMap(event -> {
                    // Проверяем, принадлежит ли событие пользователю
                    if (!event.getUserId().equals(userId)) {
                        // Если событие существует, но принадлежит другому пользователю
                        return Mono.error(new CustomAccessDeniedException("Access Denied. Event belongs to another user"));
                    }
                    // Если событие принадлежит пользователю, возвращаем DTO
                    return mapEventToDtoWithFile(event);
                })
                .switchIfEmpty(Mono.error(new CustomNotFoundException("Event not found")));
    }

    public Flux<EventDto> getAllEvents() {
        return eventService.getAllEvents()
                .flatMap(this::mapEventToDtoWithFile);
    }

    public Flux<EventDto> getUserEventsDependingOnRole(Authentication authentication, Long userId) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long authorisedUserId = customPrincipal.getId();
        String userRole = customPrincipal.getRole();

        if (!authorisedUserId.equals(userId) && RoleConstants.USER.equals(userRole)) {
            return Flux.error(new CustomAccessDeniedException("You cannot get other user events"));
        }
        return getEventsByUserId(userId);
    }

    public Flux<EventDto> getEventsByUserId(Long userId) {
        return eventService.findByUserId(userId)
                .flatMap(this::mapEventToDtoWithFile)
                .switchIfEmpty(Mono.error(new CustomNotFoundException("No files found for user " + userId)));
    }

    //Переиспользуем метод гетФайл из файлМенеджментСервис
    private Mono<EventDto> mapEventToDtoWithFile(Event event) {
        return fileManagementService.getFile(event.getFileId())
                .map(fileGetDto -> {
                    EventDto eventDto = eventMapper.map(event);
                    eventDto.setFileGetDto(fileGetDto);
                    return eventDto;
                });
    }
}
