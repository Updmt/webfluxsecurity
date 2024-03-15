package com.updmtProjects.webfluxsecurity.rest;

import com.updmtProjects.webfluxsecurity.dto.UserRequestDto;
import com.updmtProjects.webfluxsecurity.dto.UserResponseDto;
import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.mapper.UserMapper;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.UserManagementService;
import com.updmtProjects.webfluxsecurity.service.UserService.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(UserRestControllerV1.ROOT_URL)
public class UserRestControllerV1 {

    public static final String ROOT_URL = "/api/v1/users";

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserManagementService userManagementService;

    @GetMapping
    public Mono<UserResponseDto> getUserInfo(Authentication authentication) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long userId = customPrincipal.getId();
        return userManagementService.getUserInfo(userId);
    }

    @GetMapping("/{id}")
    public Mono<UserResponseDto> getUser(@PathVariable Long id) {
        return userManagementService.getUserInfo(id);
    }

    @GetMapping("/all")
    public Flux<UserResponseDto> getAllUsers() {
        return userManagementService.getAllUsersInfo();
    }

    @PostMapping
    public Mono<UserResponseDto> create(@RequestBody UserRequestDto userRequestDto) {
        User user = userMapper.map(userRequestDto);
        return userService.createUser(user)
                .map(userMapper::map);
    }

    @PutMapping("/{id}")
    public Mono<UserResponseDto> update(Authentication authentication, @PathVariable Long id,
                                        @RequestBody UserUpdateDto userUpdateDto) {
        return userManagementService.updateUserDependingOnRole(authentication, userUpdateDto, id);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(Authentication authentication, @PathVariable Long id) {
        return userManagementService.deleteUserDependingInRole(authentication, id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
