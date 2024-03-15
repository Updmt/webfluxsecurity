package com.updmtProjects.webfluxsecurity.mapper;

import com.updmtProjects.webfluxsecurity.dto.UserRequestDto;
import com.updmtProjects.webfluxsecurity.dto.UserResponseDto;
import com.updmtProjects.webfluxsecurity.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDto map(User user);

    User map(UserRequestDto userRequestDto);
}
