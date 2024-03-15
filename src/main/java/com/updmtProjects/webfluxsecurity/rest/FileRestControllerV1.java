package com.updmtProjects.webfluxsecurity.rest;

import com.updmtProjects.webfluxsecurity.dto.FileDto;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.FileManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(FileRestControllerV1.ROOT_URL)
@RequiredArgsConstructor
public class FileRestControllerV1 {

    public static final String ROOT_URL = "/api/v1/files";

    private final FileManagementService fileManagementService;

    @PostMapping("/upload")
    public Mono<FileDto> uploadFile(@RequestPart("file") FilePart filePart, Authentication authentication) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long userId = customPrincipal.getId();
        return fileManagementService.uploadAndRegisterFile(filePart, userId);
    }

    @GetMapping("/{fileId}")
    public Mono<FileDto> getFile(Authentication authentication, @PathVariable Long fileId) {
        return fileManagementService.getFileDependingOnUserRole(authentication, fileId);
    }

    @GetMapping("all/{userId}")
    public Flux<FileDto> getUserFiles(Authentication authentication, @PathVariable Long userId) {
        return fileManagementService.getUserFilesDependingOnRole(authentication, userId);
    }
}
