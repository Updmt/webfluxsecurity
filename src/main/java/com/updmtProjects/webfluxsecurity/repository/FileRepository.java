package com.updmtProjects.webfluxsecurity.repository;

import com.updmtProjects.webfluxsecurity.entity.File;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface FileRepository extends R2dbcRepository<File, Long> {

}
