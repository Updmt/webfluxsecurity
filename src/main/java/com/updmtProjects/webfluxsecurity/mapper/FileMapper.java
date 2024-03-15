package com.updmtProjects.webfluxsecurity.mapper;

import com.updmtProjects.webfluxsecurity.dto.FileDto;
import com.updmtProjects.webfluxsecurity.entity.File;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper {

    FileDto mapToFileDto(File file);
}
