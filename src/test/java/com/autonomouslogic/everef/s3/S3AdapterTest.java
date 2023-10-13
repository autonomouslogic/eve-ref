package com.autonomouslogic.everef.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.TempFiles;
import io.reactivex.rxjava3.core.Flowable;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

public class S3AdapterTest {
	TempFiles tempFiles = new TempFiles();

	S3Adapter adapter;

	S3AsyncClient client;

	@BeforeEach
	void setup() {
		adapter = new S3Adapter();
		client = Mockito.mock(S3AsyncClient.class);
	}

	@Test
	void shouldListRecursively() {
		mockListObjects(List.of(ListObjectsV2Response.builder()
				.contents(
						S3Object.builder().key("dir/").size(0L).build(),
						S3Object.builder()
								.key("file1")
								.size(1000L)
								.eTag("etag1")
								.build())
				.build()));
		var objs = adapter.listObjects(S3Url.builder().bucket("bucket").build(), true, client)
				.toList()
				.blockingGet();
		assertEquals(
				List.of(ListedS3Object.create(
						S3Object.builder()
								.key("file1")
								.size(1000L)
								.eTag("etag1")
								.build(),
						"bucket")),
				objs);
		verifyListRequest(ListObjectsV2Request.builder().bucket("bucket").build());
	}

	@Test
	void shouldListRecursivelyWithPrefix() {
		mockListObjects(List.of(ListObjectsV2Response.builder()
				.contents(
						S3Object.builder().key("prefix/dir/").size(0L).build(),
						S3Object.builder()
								.key("prefix/file1")
								.size(1000L)
								.eTag("etag1")
								.build(),
						S3Object.builder()
								.key("prefix/dir/file2")
								.size(2000L)
								.eTag("etag2")
								.build())
				.build()));
		var objs = adapter.listObjects(
						S3Url.builder().bucket("bucket").path("prefix/").build(), true, client)
				.toList()
				.blockingGet();
		assertEquals(
				List.of(
						ListedS3Object.create(
								S3Object.builder()
										.key("prefix/file1")
										.size(1000L)
										.eTag("etag1")
										.build(),
								"bucket"),
						ListedS3Object.create(
								S3Object.builder()
										.key("prefix/dir/file2")
										.size(2000L)
										.eTag("etag2")
										.build(),
								"bucket")),
				objs);
		verifyListRequest(ListObjectsV2Request.builder()
				.bucket("bucket")
				.prefix("prefix/")
				.build());
	}

	@Test
	void shouldListNonRecursively() {
		mockListObjects(List.of(ListObjectsV2Response.builder()
				.contents(
						S3Object.builder().key("dir/").size(0L).build(),
						S3Object.builder()
								.key("file1")
								.size(1000L)
								.eTag("etag1")
								.build())
				.commonPrefixes(List.of(CommonPrefix.builder().prefix("dir/").build()))
				.build()));
		var objs = adapter.listObjects(S3Url.builder().bucket("bucket").build(), false, client)
				.toList()
				.blockingGet();
		assertEquals(
				List.of(
						ListedS3Object.create(
								CommonPrefix.builder().prefix("dir/").build(), "bucket"),
						ListedS3Object.create(
								S3Object.builder()
										.key("file1")
										.size(1000L)
										.eTag("etag1")
										.build(),
								"bucket")),
				objs);
		verifyListRequest(
				ListObjectsV2Request.builder().bucket("bucket").delimiter("/").build());
	}

	@Test
	void shouldListNonRecursivelyWithPrefix() {
		mockListObjects(List.of(ListObjectsV2Response.builder()
				.contents(
						S3Object.builder().key("prefix/dir/").size(0L).build(),
						S3Object.builder()
								.key("prefix/file1")
								.size(1000L)
								.eTag("etag1")
								.build())
				.commonPrefixes(
						List.of(CommonPrefix.builder().prefix("prefix/dir/").build()))
				.build()));
		var objs = adapter.listObjects(
						S3Url.builder().bucket("bucket").path("prefix/").build(), false, client)
				.toList()
				.blockingGet();
		assertEquals(
				List.of(
						ListedS3Object.create(
								CommonPrefix.builder().prefix("prefix/dir/").build(), "bucket"),
						ListedS3Object.create(
								S3Object.builder()
										.key("prefix/file1")
										.size(1000L)
										.eTag("etag1")
										.build(),
								"bucket")),
				objs);
		verifyListRequest(ListObjectsV2Request.builder()
				.bucket("bucket")
				.prefix("prefix/")
				.delimiter("/")
				.build());
	}

	private void mockListObjects(List<ListObjectsV2Response> responses) {
		when(client.listObjectsV2Paginator((ListObjectsV2Request) Mockito.any()))
				.thenAnswer(invocation1 -> {
					var flowable = Flowable.fromIterable(responses);
					var publisher = Mockito.mock(ListObjectsV2Publisher.class);
					Mockito.doAnswer(invocation2 -> {
								var subscriber = invocation2.getArgument(0, Subscriber.class);
								flowable.subscribe(subscriber);
								return null;
							})
							.when(publisher)
							.subscribe(Mockito.any(Subscriber.class));
					return publisher;
				});
	}

	private ListObjectsV2Request verifyListRequest(ListObjectsV2Request expected) {
		var captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
		Mockito.verify(client).listObjectsV2Paginator(captor.capture());
		assertEquals(expected, captor.getValue());
		return captor.getValue();
	}

	@Test
	@SneakyThrows
	void shouldUploadFilesWithCorrectTimestamps() {
		var lastModified = Instant.ofEpochMilli(822966041000L);
		when(client.putObject(Mockito.any(PutObjectRequest.class), Mockito.any(AsyncRequestBody.class)))
				.thenReturn(CompletableFuture.completedFuture(
						PutObjectResponse.builder().build()));
		var file = tempFiles.tempFile(S3AdapterTest.class.getSimpleName(), ".test");
		Files.setLastModifiedTime(file, FileTime.from(lastModified));
		adapter.putObject(
						PutObjectRequest.builder()
								.metadata(Map.of("foo", "bar"))
								.build(),
						file.toFile(),
						client)
				.blockingGet();
		var captor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(client).putObject(captor.capture(), Mockito.any(AsyncRequestBody.class));
		var req = captor.getValue();
		assertEquals(
				Long.toString(lastModified.toEpochMilli()), req.metadata().get(S3HeaderNames.SRC_LAST_MODIFIED_MILLIS));
		assertEquals("bar", req.metadata().get("foo"));
	}
}
