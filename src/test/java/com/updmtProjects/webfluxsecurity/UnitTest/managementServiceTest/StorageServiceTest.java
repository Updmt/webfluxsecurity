package com.updmtProjects.webfluxsecurity.UnitTest.managementServiceTest;

import com.updmtProjects.webfluxsecurity.service.StorageService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {

    @InjectMocks
    private StorageService storageService;

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Test
    void uploadFile_ok() {

        //мок FilePart
        FilePart filePartMock = mock(FilePart.class);
        when(filePartMock.filename()).thenReturn("testFile.txt");
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap("Hello, world!".getBytes(StandardCharsets.UTF_8));
        Flux<DataBuffer> contentFlux = Flux.just(dataBuffer);
        when(filePartMock.content()).thenReturn(contentFlux);

        //мок S3AsyncClient
        PutObjectResponse response = PutObjectResponse.builder().build();
        CompletableFuture<PutObjectResponse> futureResponse = CompletableFuture.completedFuture(response);

        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class))).thenReturn(futureResponse);

        Mono<String> resultMono = storageService.uploadFile(filePartMock);

        StepVerifier.create(resultMono)
                .expectNext("File uploaded: testFile.txt")
                .verifyComplete();
    }

}
