package com.updmtProjects.webfluxsecurity.service.FileService;

import com.updmtProjects.webfluxsecurity.entity.File;
import com.updmtProjects.webfluxsecurity.exception.CustomNotFoundException;
import com.updmtProjects.webfluxsecurity.repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    public FileServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public Mono<File> createFile(File file) {
        return fileRepository.save(file)
                .doOnSuccess(f -> log.info("Created file: {} ", f.getFileName()));
    }

    @Override
    public Mono<File> getFileById(Long id) {
        return fileRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomNotFoundException("File with ID " + id + " not found")));
    }

}
