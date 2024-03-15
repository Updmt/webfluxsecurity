package com.updmtProjects.webfluxsecurity.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class File {

    @Id
    private Long id;
    private String fileName;
    private String storageLink;
    private LocalDateTime created;
    private boolean deleted;
}
