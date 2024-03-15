package com.updmtProjects.webfluxsecurity.UnitTest.serviceImplTest;

import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.repository.FileRepository;
import com.updmtProjects.webfluxsecurity.service.FileService.FileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileServiceImplTest {

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private FileRepository fileRepository;

    @Test
    void createFile_ok() {
        File fileToSave = File.builder()
                .fileName("testFile.txt")
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        File savedFile = File.builder()
                .id(1L)
                .fileName(fileToSave.getFileName())
                .created(fileToSave.getCreated())
                .deleted(fileToSave.isDeleted())
                .build();

        when(fileRepository.save(any(File.class))).thenReturn(Mono.just(savedFile));

        Mono<File> resultMono = fileService.createFile(fileToSave);

        StepVerifier
                .create(resultMono)
                .expectNextMatches(savedFileResult ->
                        savedFileResult.getId().equals(1L) &&
                                savedFileResult.getFileName().equals(fileToSave.getFileName()) &&
                                !savedFileResult.isDeleted())
                .verifyComplete();
    }

    @Test
    void getFileById_ok() {
        File file = File.builder()
                .id(1L)
                .fileName("testFile.txt")
                .created(LocalDateTime.now())
                .deleted(false)
                .build();

        when(fileRepository.findById(1L)).thenReturn(Mono.just(file));

        Mono<File> resultMono = fileService.getFileById(1L);

        StepVerifier
                .create(resultMono)
                .expectNextMatches(fileResult ->
                        fileResult.getId().equals(1L) &&
                                fileResult.getFileName().equals("testFile.txt"))
                .verifyComplete();
    }

    @Test
    void getFileById_throwException() {
        Long fileId = 1L;

        when(fileRepository.findById(fileId)).thenReturn(Mono.empty());

        Mono<File> resultMono = fileService.getFileById(fileId);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof CustomNotFoundException)
                .verify();
    }
}
