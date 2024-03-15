package com.updmtProjects.webfluxsecurity.UnitTest.serviceImplTest;

import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.repository.UserRepository;
import com.updmtProjects.webfluxsecurity.service.UserService.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createUserTest_ok() {
        User userToSave = new User();
        userToSave.setUsername("testUser");
        userToSave.setPassword("password");
        userToSave.setFirstName("firstName");
        userToSave.setLastName("lastName");
        userToSave.setRole(UserRole.USER);

        User savedUser = new User();
        savedUser.setUsername(userToSave.getUsername());
        savedUser.setPassword("encodedPassword");
        savedUser.setFirstName(userToSave.getFirstName());
        savedUser.setLastName(userToSave.getLastName());
        savedUser.setRole(userToSave.getRole());
        savedUser.setEnabled(true);
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());

        when(passwordEncoder.encode(userToSave.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));

        Mono<User> resultMono = userService.createUser(userToSave);

        StepVerifier
                .create(resultMono)
                .expectNextMatches(createdUser ->
                        createdUser.getPassword().equals("encodedPassword") &&
                                createdUser.isEnabled() &&
                                Objects.nonNull(createdUser.getCreatedAt()) &&
                                Objects.nonNull(createdUser.getUpdatedAt()))
                .verifyComplete();
    }

    @Test
    void createUser_failure() {
        User user = new User();
        user.setUsername("testUser");
        user.setPassword("testPass");
        user.setRole(UserRole.USER);

        when(passwordEncoder.encode(any(String.class))).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenReturn(Mono.error( new RuntimeException("Database error")));

        Mono<User> resultMono = userService.createUser(user);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Database error"))
                .verify();
    }

    @Test
    void updateUserTest_ok() {
        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setUsername("testUser");
        userUpdateDto.setFirstName("firstName");
        userUpdateDto.setLastName("lastName");
        userUpdateDto.setRole(UserRole.USER);

        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Mono.just(user));

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(userUpdateDto.getUsername());
        updatedUser.setFirstName(userUpdateDto.getFirstName());
        updatedUser.setLastName(userUpdateDto.getLastName());
        updatedUser.setRole(userUpdateDto.getRole());
        updatedUser.setEnabled(user.isEnabled());
        updatedUser.setCreatedAt(user.getCreatedAt());
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        Mono<User> userMono = userService.updateUser(1L, userUpdateDto);

        StepVerifier
                .create(userMono)
                .expectNextMatches(renewedUser ->
                        renewedUser.getUsername().equals(userUpdateDto.getUsername()) &&
                                renewedUser.getFirstName().equals(userUpdateDto.getFirstName()) &&
                                renewedUser.getLastName().equals(userUpdateDto.getLastName()) &&
                                renewedUser.getRole().equals(userUpdateDto.getRole())
                )
                .verifyComplete();
    }

    @Test
    void updateUserTest_throwException() {
        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setUsername("testUser");
        userUpdateDto.setFirstName("firstName");
        userUpdateDto.setLastName("lastName");
        userUpdateDto.setRole(UserRole.USER);

        when(userRepository.findById(1L)).thenReturn(Mono.empty());

        Mono<User> userMono = userService.updateUser(1L, userUpdateDto);

        StepVerifier
                .create(userMono)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void deleteUser_ok() {
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Mono.just(user));

        User deletedUser = new User();
        deletedUser.setId(user.getId());
        deletedUser.setUsername(user.getUsername());
        deletedUser.setFirstName(user.getFirstName());
        deletedUser.setLastName(user.getLastName());
        deletedUser.setRole(user.getRole());
        deletedUser.setEnabled(user.isEnabled());
        deletedUser.setCreatedAt(user.getCreatedAt());
        deletedUser.setUpdatedAt(LocalDateTime.now());
        deletedUser.setDeleted(Boolean.TRUE);

        when(userRepository.save(any(User.class))).thenReturn(Mono.just(deletedUser));

        Mono<User> userMono = userService.deleteUser(1L);

        StepVerifier
                .create(userMono)
                .expectNextMatches(eliminatedUser ->
                        !eliminatedUser.getUpdatedAt().equals(user.getUpdatedAt()) &&
                        eliminatedUser.isDeleted() == Boolean.TRUE)
                .verifyComplete();
    }

    @Test
    void deleteUser_throwException() {
        when(userRepository.findById(1L)).thenReturn(Mono.empty());

        Mono<User> userMono = userService.deleteUser(1L);

        StepVerifier
                .create(userMono)
                .expectErrorMatches(throwable -> throwable instanceof CustomNotFoundException &&
                        throwable.getMessage().equals("User not found"))
                .verify();
    }

    @Test
    void getUserById_ok() {
        User user = createUser();

        when(userRepository.findById(1L)).thenReturn(Mono.just(user));

        Mono<User> userMono = userService.getUserById(1L);

        StepVerifier.create(userMono)
                .expectNextMatches(foundUser ->
                        foundUser.getId().equals(user.getId()) &&
                                foundUser.getUsername().equals(user.getUsername()) &&
                                foundUser.getFirstName().equals(user.getFirstName()) &&
                                foundUser.getLastName().equals(user.getLastName()) &&
                                foundUser.getRole().equals(user.getRole()) &&
                                foundUser.isEnabled() == user.isEnabled() &&
                                foundUser.getCreatedAt().equals(user.getCreatedAt()) &&
                                foundUser.getUpdatedAt().equals(user.getUpdatedAt()))
                .verifyComplete();
    }

    @Test
    void getUserById_throwException() {
        when(userRepository.findById(1L)).thenReturn(Mono.empty());

        Mono<User> userMono = userService.getUserById(1L);

        StepVerifier
                .create(userMono)
                .expectError(CustomNotFoundException.class)
                .verify();
    }

    @Test
    void getAllUsers_ok() {
        User user1 = createUser();

        User user2 = createUser();
        user2.setId(2L);
        user2.setUsername("existingUser2");
        user2.setFirstName("existingFirstName2");
        user2.setLastName("existingLastName2");

        when(userRepository.findAll()).thenReturn(Flux.just(user1, user2));

        Flux<User> userFlux = userService.getAllUsers();

        StepVerifier.create(userFlux)
                .expectNextMatches(user ->
                        user.getId() == 1L &&
                                user.getUsername().equals("existingUser") &&
                                user.getFirstName().equals("existingFirstName") &&
                                user.getLastName().equals("existingLastName") &&
                                user.getRole() == UserRole.ADMIN)
                .expectNextMatches(user ->
                        user.getId() == 2L &&
                                user.getUsername().equals("existingUser2") &&
                                user.getFirstName().equals("existingFirstName2") &&
                                user.getLastName().equals("existingLastName2") &&
                                user.getRole() == UserRole.ADMIN)
                .verifyComplete();
    }

    @Test
    void getUserByUsername() {
        User user = createUser();

        when(userRepository.findByUsername("existingUser")).thenReturn(Mono.just(user));

        Mono<User> userMono = userService.getUserByUsername("existingUser");

        StepVerifier
                .create(userMono)
                .expectNextMatches(exisitngUser ->
                        exisitngUser.getId() == 1L &&
                                exisitngUser.getUsername().equals("existingUser") &&
                                exisitngUser.getFirstName().equals("existingFirstName") &&
                                exisitngUser.getLastName().equals("existingLastName") &&
                                exisitngUser.getRole() == UserRole.ADMIN)
                .verifyComplete();
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("existingUser");
        user.setFirstName("existingFirstName");
        user.setLastName("existingLastName");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
