package com.updmtProjects.webfluxsecurity.mapper;

import com.updmtProjects.webfluxsecurity.dto.EventDto;
import com.updmtProjects.webfluxsecurity.entity.Event;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventDto map(Event event);
}
