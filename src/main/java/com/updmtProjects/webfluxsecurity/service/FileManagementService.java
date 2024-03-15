package com.updmtProjects.webfluxsecurity.service;

import com.updmtProjects.webfluxsecurity.dto.FileDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.exception.CustomAccessDeniedException;
import com.updmtProjects.webfluxsecurity.mapper.FileMapper;
import com.updmtProjects.webfluxsecurity.security.CustomPrincipal;
import com.updmtProjects.webfluxsecurity.service.EventService.EventService;
import com.updmtProjects.webfluxsecurity.service.FileService.FileService;
import com.updmtProjects.webfluxsecurity.util.RoleConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileManagementService {

    private static final String storageLink = "https://storage.yandexcloud.net/%s/%s";

    @Value("${yandex.bucket-name}")
    private String bucketName;

    private final StorageService storageService;
    private final FileService fileService;
    private final EventService eventService;
    private final FileMapper fileMapper;

    @Transactional
    public Mono<FileDto> uploadAndRegisterFile(FilePart filePart, Long userId) {
        String encodedFileName = URLEncoder.encode(filePart.filename(), StandardCharsets.UTF_8);
        encodedFileName = encodedFileName.replace("+", "%20");
        String storageUrl = String.format(storageLink, bucketName, encodedFileName);

        File file = new File();
        file.setFileName(filePart.filename());
        file.setStorageLink(storageUrl);
        file.setCreated(LocalDateTime.now());

        Mono<FileDto> fileUploadDtoMono = fileService.createFile(file)
                .doOnSubscribe(subscription -> log.info("Начало записи данных в таблицу File."))
                .map(fileMapper::mapToFileDto)
                .doOnSuccess(createdFileDto -> log.info("Запись данных в таблицу File завершена."));

        storageService.uploadFile(filePart)
                .doOnSuccess(result -> log.info("Загрузка файла на S3 завершена."))
                .subscribe();

        return fileUploadDtoMono.flatMap(createdFileGetDto -> {
            Event event = new Event();
            event.setCreated(LocalDateTime.now());
            event.setFileId(createdFileGetDto.getId());
            event.setUserId(userId);
            return eventService.createEvent(event)
                    .then(Mono.just(createdFileGetDto));
        });
    }

    public Mono<FileDto> getFileDependingOnUserRole(Authentication authentication, Long fileId) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long userId = customPrincipal.getId();
        String userRole = customPrincipal.getRole();

        if (RoleConstants.ADMIN.equals(userRole) || RoleConstants.MODERATOR.equals(userRole)) {
            return getFile(fileId);
        } else {
            return getFileByFileIdAndUserId(userId, fileId);
        }
    }

    //метод для ROLE_ADMIN
    public Mono<FileDto> getFile(Long fileId) {
        return fileService.getFileById(fileId)
                .flatMap(file -> Mono.just(fileMapper.mapToFileDto(file)));
    }

    //находим все ивенты юзера -> находим определенный файл
    //метод для ROLE_USER
    public Mono<FileDto> getFileByFileIdAndUserId(Long userId, Long fileId) {
        return fileService.getFileById(fileId)
                .flatMap(file ->
                        // Принадлежит ли файл запрашивающему пользователю
                        eventService.findByUserId(userId)
                                .filter(event -> event.getFileId().equals(fileId))
                                .next()
                                .switchIfEmpty(Mono.error(new CustomAccessDeniedException("Access denied for file ID " + fileId + " for user ID " + userId)))
                                // Если файл принадлежит пользователю, возвращаем его DTO
                                .flatMap(event -> Mono.just(fileMapper.mapToFileDto(file))));
    }

    public Flux<FileDto> getUserFilesDependingOnRole(Authentication authentication, Long userId) {
        CustomPrincipal customPrincipal = (CustomPrincipal) authentication.getPrincipal();
        Long authorisedUserId = customPrincipal.getId();
        String userRole = customPrincipal.getRole();

        if (!authorisedUserId.equals(userId) && RoleConstants.USER.equals(userRole)) {
            return Flux.error(new CustomAccessDeniedException("You cannot get other user files"));
        }
        return eventService.findByUserId(userId)
                .flatMap(event -> fileService.getFileById(event.getFileId())
                        .flatMap(file -> Mono.just(fileMapper.mapToFileDto(file))));
    }
}
