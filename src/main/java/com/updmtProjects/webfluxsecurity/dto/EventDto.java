package com.updmtProjects.webfluxsecurity.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode
public class EventDto {

    private Long id;
    private LocalDateTime created;
    private FileDto fileGetDto;
}
