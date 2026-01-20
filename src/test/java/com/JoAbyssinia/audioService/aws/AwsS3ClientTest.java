package com.JoAbyssinia.audioService.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Yohannes k Yimam
 */
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AwsS3ClientTest {

  @Mock private S3AsyncClient s3AsyncClient;

  @Mock private S3Presigner s3Presigner;

  private Vertx vertx;
  private AwsS3Client awsS3Client;

  @BeforeEach
  void setUp() {
    vertx = Vertx.vertx();
    awsS3Client = new AwsS3Client(vertx, s3AsyncClient, s3Presigner);
  }

  @AfterEach
  void cleanUp() {
    File testFolder = new File("testFolder");
    File testFile = new File(testFolder, "testFile.txt");
    if (testFile.exists()) {
      testFile.delete();
    }
    if (testFolder.exists()) {
      testFolder.delete();
    }
  }

  @Test
  void uploadFolderToS3Success(VertxTestContext testContext) {
    // create a temporary test folder with a file
    File testFolder = new File("testFolder");
    testFolder.mkdir();
    File testFile = new File(testFolder, "testFile.txt");

    try {
      testFile.createNewFile();

      // Mock s3 client response
      CompletableFuture<PutObjectResponse> future = new CompletableFuture<>();
      future.complete(PutObjectResponse.builder().build());
      when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
          .thenReturn(future);

      vertx
          .fileSystem()
          .writeFile(testFile.getAbsolutePath(), Buffer.buffer("test data"))
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  // test the upload
                  awsS3Client
                      .uploadFolderToS3(testFolder, "testFile/")
                      .onComplete(
                          testContext.succeeding(
                              result -> {
                                // verify an aws client is called
                                verify(s3AsyncClient, atLeastOnce())
                                    .putObject(
                                        any(PutObjectRequest.class), any(AsyncRequestBody.class));

                                testFile.deleteOnExit();
                                testFolder.deleteOnExit();

                                testContext.completeNow();
                              }));
                } else {
                  testContext.failNow(ar.cause());
                }
              });

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void downloadFileSuccess(VertxTestContext testContext) {
    // create mock response time
    byte[] mockFileContent = "test content".getBytes();
    ResponseBytes<GetObjectResponse> mockResponse =
        ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), mockFileContent);

    // Mock S3 client response for download
    CompletableFuture<ResponseBytes<GetObjectResponse>> future = new CompletableFuture<>();
    future.complete(mockResponse);

    when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(future);

    vertx
        .fileSystem()
        .createTempFile("file", ".txt")
        .onComplete(
            tempFile -> {
              // Test the download
              awsS3Client
                  .downloadFile("text/file.mp3", "test-file")
                  .onComplete(
                      testContext.succeeding(
                          result -> {
                            // Verify file exists and S3 client was called
                            verify(s3AsyncClient)
                                .getObject(
                                    any(GetObjectRequest.class),
                                    any(AsyncResponseTransformer.class));

                            vertx
                                .fileSystem()
                                .delete(result.getAbsolutePath())
                                .onComplete(ar -> testContext.completeNow());
                          }));
            })
        .onFailure(testContext::failNow);
  }

  @Test
  void createFolderToS3Success(VertxTestContext testContext) {
    // mock client response for create folder
    CompletableFuture<PutObjectResponse> future = new CompletableFuture<>();
    future.complete(PutObjectResponse.builder().build());
    when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
        .thenReturn(future);

    // test the folder create a function
    awsS3Client
        .createFolder("test-folder")
        .onComplete(
            testContext.succeeding(
                result -> {
                  // verify the client is called
                  verify(s3AsyncClient, atLeastOnce())
                      .putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
                  testContext.completeNow();
                }));
  }
}
