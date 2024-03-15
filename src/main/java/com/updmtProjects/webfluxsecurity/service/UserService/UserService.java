package com.updmtProjects.webfluxsecurity.service.UserService;

import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<User> getUserById(Long id);
    Flux<User> getAllUsers();
    Mono<User> createUser(User user);
    Mono<User> updateUser(Long id, UserUpdateDto userUpdateDto);
    Mono<User> deleteUser(Long id);
    Mono<User> getUserByUsername(String username);
}
