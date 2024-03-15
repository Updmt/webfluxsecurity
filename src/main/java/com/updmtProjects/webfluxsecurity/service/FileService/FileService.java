package com.updmtProjects.webfluxsecurity.service.FileService;

import com.updmtProjects.webfluxsecurity.entity.File;
import reactor.core.publisher.Mono;

public interface FileService {

    Mono<File> createFile(File file);
    Mono<File> getFileById(Long id);
}
