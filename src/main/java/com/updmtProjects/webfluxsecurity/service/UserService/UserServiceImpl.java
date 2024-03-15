package com.updmtProjects.webfluxsecurity.service.UserService;

import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<User> createUser(User user) {
        return userRepository.save(
                user.toBuilder()
                        .password(passwordEncoder.encode(user.getPassword()))
                        .role(user.getRole())
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        ).doOnSuccess(u -> log.info("In registerUser - user: {} created", u));
    }

    public Mono<User> updateUser(Long id, UserUpdateDto userUpdateDto) {
        return userRepository.findById(id)
                .flatMap(user -> {
                            if (Objects.nonNull(userUpdateDto.getUsername())) {
                                user.setUsername(userUpdateDto.getUsername());
                            }
                            if (Objects.nonNull(userUpdateDto.getFirstName())) {
                                user.setFirstName(userUpdateDto.getFirstName());
                            }
                            if (Objects.nonNull(userUpdateDto.getLastName())) {
                                user.setLastName(userUpdateDto.getLastName());
                            }
                            if (Objects.nonNull(userUpdateDto.getRole())) {
                                user.setRole(userUpdateDto.getRole());
                            }
                            user.setUpdatedAt(LocalDateTime.now());
                            return userRepository.save(user);
                        }
                )
                .doOnSuccess(u -> log.info("In updateUser - user: {} updated", u))
                .switchIfEmpty(Mono.error(new CustomNotFoundException("User not found")));
    }

    public Mono<User> deleteUser(Long id) {
        return userRepository.findById(id)
                .flatMap(user -> {
                            user.setUpdatedAt(LocalDateTime.now());
                            user.setDeleted(Boolean.TRUE);
                            return userRepository.save(user);
                        }
                )
                .doOnSuccess(user -> log.info("In deletedUser - user: {} deleted", user))
                .switchIfEmpty(Mono.error(new CustomNotFoundException("User not found")));
    }

    public Mono<User> getUserById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomNotFoundException("User not found")));
    }

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Mono<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
