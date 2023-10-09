package com.autonomouslogic.everef.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.url.S3Url;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

public class S3AdapterTest {
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
		Mockito.when(client.listObjectsV2Paginator((ListObjectsV2Request) Mockito.any()))
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
}
