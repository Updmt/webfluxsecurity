package com.updmtProjects.webfluxsecurity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.core.io.buffer.DataBuffer;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.ByteBuffer;
import java.util.Map;

@Service
@Slf4j
public class StorageService {

    @Value("${yandex.bucket-name}")
    private String bucketName;

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;


    public StorageService(S3AsyncClient s3AsyncClient, S3Presigner s3Presigner) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Presigner = s3Presigner;
    }

    public Mono<String> uploadFile(FilePart filePart) {
        String fileName = filePart.filename();

        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    long fileSize = dataBuffer.readableByteCount();
                    DataBufferUtils.release(dataBuffer);
                    return fileSize;
                })
                .flatMap(fileSize -> {
                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .contentLength(fileSize)
                            .contentType("application/octet-stream")
                            .metadata(Map.of("Content-Disposition", "inline"))
                            .build();

                    Flux<ByteBuffer> byteBufferFlux = filePart.content().map(DataBuffer::asByteBuffer);

                    return Mono.fromFuture(() ->
                            s3AsyncClient.putObject(putObjectRequest,
                                    AsyncRequestBody.fromPublisher(byteBufferFlux))
                    ).doOnSubscribe(subscription -> log.info("Начало загрузки файла на S3"))
                            .thenReturn("File uploaded: " + fileName);
                });
    }
}
