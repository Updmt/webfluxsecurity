package com.updmtProjects.webfluxsecurity.service;

import com.updmtProjects.webfluxsecurity.dto.UserResponseDto;
import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.mapper.UserMapper;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.UserService.UserService;
import com.updmtProjects.webfluxsecurity.util.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserService userService;
    private final EventManagementService eventManagementService;
    private final UserMapper userMapper;

    public Mono<UserResponseDto> getUserInfo(Long id) {
        return userService.getUserById(id)
                .flatMap(this::mapUserToDtoWithEvent);
    }

    public Flux<UserResponseDto> getAllUsersInfo() {
        return userService.getAllUsers()
                .flatMap(this::mapUserToDtoWithEvent);
    }

    public Mono<UserResponseDto> updateUserDependingOnRole(Authentication authentication, UserUpdateDto userUpdateDto, Long userId) {
        return canUserPerformAction(authentication, userId)
                .flatMap(canPerform -> {
                    if (!canPerform) {
                        return Mono.error(new CustomAccessDeniedException("You cannot update data for another user"));
                    }
                    return userService.updateUser(userId, userUpdateDto)
                            .flatMap(this::mapUserToDtoWithEvent);
                });
    }

    public Mono<User> deleteUserDependingInRole(Authentication authentication, Long id) {
        return canUserPerformAction(authentication, id)
                .flatMap(canPerform -> {
                    if (!canPerform) {
                        return Mono.error(new CustomAccessDeniedException("You cannot delete data for another user"));
                    }
                    return userService.deleteUser(id);
                });
    }

    private Mono<Boolean> canUserPerformAction(Authentication authentication, Long entityId) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long authorisedUserId = customPrincipal.getId();
        String userRole = customPrincipal.getRole();

        // Проверяем, имеет ли пользователь право на выполнение действия
        boolean isAdmin = RoleConstants.ADMIN.equals(userRole);
        boolean isOwner = authorisedUserId.equals(entityId);

        return Mono.just(isAdmin || isOwner);
    }

    private Mono<UserResponseDto> mapUserToDtoWithEvent(User user) {
        return eventManagementService.getEventsByUserId(user.getId())
                .collectList()
                .map(events -> {
                    UserResponseDto userResponseDto = userMapper.map(user);
                    userResponseDto.setEvents(events);
                    return userResponseDto;
                })
                .onErrorResume(e -> {
                    UserResponseDto userResponseDto = userMapper.map(user);
                    userResponseDto.setEvents(Collections.emptyList());
                    return Mono.just(userResponseDto);
                });
    }
}
