package com.updmtProjects.webfluxsecurity.UnitTest.managementServiceTest;

import com.updmtProjects.webfluxsecurity.AbstractIntegrationTest;
import com.updmtProjects.webfluxsecurity.dto.EventDto;
import com.updmtProjects.webfluxsecurity.dto.UserResponseDto;
import com.updmtProjects.webfluxsecurity.dto.UserUpdateDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.entity.User;
import com.updmtProjects.webfluxsecurity.entity.UserRole;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.EventManagementService;
import com.updmtProjects.webfluxsecurity.service.UserManagementService;
import com.updmtProjects.webfluxsecurity.service.UserService.UserService;
import com.updmtProjects.webfluxsecurity.util.RoleConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class UserManagementTest extends AbstractIntegrationTest {

    @Autowired
    private UserManagementService userManagementService;
    @MockBean
    private UserService userService;
    @MockBean
    private EventManagementService eventManagementService;

    @Test
    void getUserInfo_ok() {
        Long eventId = 1L;
        Long userId = 1L;
        Long fileId = 1L;

        User user = createUser();

        Event event = new Event();
        event.setId(eventId);
        event.setCreated(LocalDateTime.now());
        event.setFileId(fileId);
        event.setUserId(user.getId());

        EventDto eventDto = new EventDto();
        eventDto.setId(event.getId());
        eventDto.setCreated(event.getCreated());
        eventDto.setFileGetDto(null);

        when(userService.getUserById(any(Long.class))).thenReturn(Mono.just(user));
        when(eventManagementService.getEventsByUserId((any(Long.class)))).thenReturn(Flux.just(eventDto));

        Mono<UserResponseDto> userResponseDtoMono = userManagementService.getUserInfo(userId);

        StepVerifier
                .create(userResponseDtoMono)
                .expectNextMatches(userDto ->
                        userDto.getId().equals(user.getId()) &&
                                userDto.getUsername().equals(user.getUsername()))
                .verifyComplete();
    }

    @Test
    void getAllUserInfoTest_ok() {
        Long firstEventId = 1L;
        Long firstUserId = 1L;

        Long secondEventId = 2L;
        Long secondUserId = 2L;

        User firstUser = createUser();
        User secondUser = createUser();
        secondUser.setId(secondUserId);
        secondUser.setUsername("existingSecondUser");
        secondUser.setFirstName("existingFirstNameUser");
        secondUser.setLastName("existingLastNameUser");

        Event firstEventUser = new Event();
        firstEventUser.setId(firstEventId);
        firstEventUser.setCreated(LocalDateTime.now());
        firstEventUser.setFileId(null);
        firstEventUser.setUserId(firstUser.getId());

        EventDto firstEventUserDto = new EventDto();
        firstEventUserDto.setId(firstEventUser.getId());
        firstEventUserDto.setCreated(firstEventUser.getCreated());
        firstEventUserDto.setFileGetDto(null);

        Event secondEventUser = new Event();
        secondEventUser.setId(secondEventId);
        secondEventUser.setCreated(LocalDateTime.now());
        secondEventUser.setFileId(null);
        secondEventUser.setUserId(secondUser.getId());

        EventDto secondEventUserDto = new EventDto();
        secondEventUserDto.setId(secondEventUser.getId());
        secondEventUserDto.setCreated(secondEventUser.getCreated());
        secondEventUserDto.setFileGetDto(null);

        when(userService.getAllUsers()).thenReturn(Flux.just(firstUser, secondUser));
        when(eventManagementService.getEventsByUserId(firstUserId)).thenReturn(Flux.just(firstEventUserDto));
        when(eventManagementService.getEventsByUserId(secondUserId)).thenReturn(Flux.just(secondEventUserDto));

        Flux<UserResponseDto> userResponseDtoMono = userManagementService.getAllUsersInfo();

        StepVerifier
                .create(userResponseDtoMono)
                .expectNextMatches(userDto ->
                        userDto.getId().equals(firstUser.getId()) &&
                                userDto.getUsername().equals(firstUser.getUsername()) &&
                                userDto.getEvents().get(0).equals(firstEventUserDto))
                .expectNextMatches(userDto ->
                        userDto.getId().equals(secondUser.getId()) &&
                                userDto.getUsername().equals(secondUser.getUsername()) &&
                                userDto.getEvents().get(0).equals(secondEventUserDto))
                .verifyComplete();
    }

    @Test
    void updateUserProfile_ok() {
        Long userId = 1L;
        User user = createUser();
        user.setId(userId);

        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setUsername("existingUser");

        Authentication authenticationMock = mock(Authentication.class);

        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.ADMIN));
        customPrincipal.setId(userId);
        customPrincipal.setName("UserAdminTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        when(userService.updateUser(userId, userUpdateDto)).thenReturn(Mono.just(user));
        when(eventManagementService.getEventsByUserId(userId)).thenReturn(Flux.empty());

        StepVerifier.create(userManagementService.updateUserDependingOnRole(authenticationMock, userUpdateDto, userId))
                .expectNextMatches(userResponseDto ->
                        userResponseDto.getId().equals(user.getId()) &&
                                userResponseDto.getUsername().equals("existingUser"))
                .verifyComplete();
    }

    @Test
    void updateUserOtherProfile_throwException() {
        Long userId = 1L;
        Long otherUserId = 2L;
        String userRole = RoleConstants.USER;

        UserUpdateDto userUpdateDto = new UserUpdateDto();
        userUpdateDto.setUsername("updatedUsername");

        Authentication authentication = Mockito.mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(userRole);
        customPrincipal.setId(otherUserId);
        customPrincipal.setName("username");

        when(authentication.getPrincipal()).thenReturn(customPrincipal);

        StepVerifier.create(userManagementService.updateUserDependingOnRole(authentication, userUpdateDto, userId))
                .expectErrorMatches(throwable -> throwable instanceof CustomAccessDeniedException &&
                        throwable.getMessage().contains("You cannot update data for another user"))
                .verify();
    }

    @Test
    void deleteUserWithProperRoleOrSameUserId_ok() {
        Long userId = 1L;
        Authentication authenticationMock = mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.ADMIN));
        customPrincipal.setId(userId);
        customPrincipal.setName("UserAdminTest");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);
        when(userService.deleteUser(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userManagementService.deleteUserDependingInRole(authenticationMock, userId))
                .verifyComplete();
    }

    @Test
    void deleteUserWithoutProperRole_throwException() {
        Long userIdToDelete = 2L;
        Long requesterUserId = 1L;
        Authentication authenticationMock = mock(Authentication.class);
        CustomPrincipal customPrincipal = new CustomPrincipal();
        customPrincipal.setRole(String.valueOf(UserRole.USER));
        customPrincipal.setId(requesterUserId);
        customPrincipal.setName("NonAdminUser");

        when(authenticationMock.getPrincipal()).thenReturn(customPrincipal);

        StepVerifier.create(userManagementService.deleteUserDependingInRole(authenticationMock, userIdToDelete))
                .expectErrorMatches(throwable -> throwable instanceof CustomAccessDeniedException &&
                        throwable.getMessage().equals("You cannot delete data for another user"))
                .verify();
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
