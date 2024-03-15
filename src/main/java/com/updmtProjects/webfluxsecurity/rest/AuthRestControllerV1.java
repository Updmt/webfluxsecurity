package com.updmtProjects.webfluxsecurity.rest;

import com.updmtProjects.webfluxsecurity.dto.AuthRequestDto;
import com.updmtProjects.webfluxsecurity.dto.AuthResponseDto;
import com.updmtProjects.webfluxsecurity.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AuthRestControllerV1.ROOT_URL)
public class AuthRestControllerV1 {

    public static final String ROOT_URL = "/api/v1/auth";

    private final SecurityService securityService;

    @PostMapping("/login")
    public Mono<AuthResponseDto> login(@RequestBody AuthRequestDto dto) {
        return securityService.authenticate(dto.getUsername(), dto.getPassword())
                .flatMap(tokenDetails -> Mono.just(
                        AuthResponseDto.builder()
                                .userId(tokenDetails.getUserId())
                                .token(tokenDetails.getToken())
                                .issuedAt(tokenDetails.getIssuedAt())
                                .expiredAt(tokenDetails.getExpiresAt())
                                .build()
                ));
    }
}
