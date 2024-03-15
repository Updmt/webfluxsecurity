package com.updmtProjects.webfluxsecurity.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FileDto {

    private Long id;
    private String fileName;
    private String storageLink;
    private LocalDateTime created;
    private boolean deleted;
}
