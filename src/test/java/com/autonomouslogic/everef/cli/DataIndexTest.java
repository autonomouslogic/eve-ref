package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.inject.AwsModule;
import com.autonomouslogic.everef.inject.S3Module;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.async.ByteArrayAsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
public class DataIndexTest {
	@Mock
	AwsCredentialsProvider dataCredentialsProvider;

	@Mock
	S3AsyncClient dataClient;

	@Inject
	S3Adapter s3Adapter;

	@Inject
	DataIndex dataIndex;

	@BeforeEach
	void before() {
		DaggerTestComponent.builder()
				.awsModule(new AwsModule().setDataCredentialsProvider(dataCredentialsProvider))
				.s3Module(new S3Module().setDataClient(dataClient))
				.build()
				.inject(this);
	}

	@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://data-bucket/")
	@Test
	void shouldGenerateIndexPages() {
		// Rig listing.
		var files = List.of(
				"index.html",
				"data.zip",
				"dir/",
				"dir/index.html",
				"dir/more-data.zip",
				"dir2/",
				"dir2/more-data2.zip");
		when(s3Adapter.listObjects(any(ListObjectsV2Request.class), eq(dataClient)))
				.thenReturn(Flowable.fromIterable(List.of(ListObjectsV2Response.builder()
						.contents(files.stream()
								.map(f -> S3Object.builder()
										.key(f)
										.size(f.endsWith("/") ? 0L : 1L)
										.lastModified(Instant.now())
										.build())
								.collect(Collectors.toList()))
						.build())));
		// Rig puts.
		var pages = new HashMap<String, String>();
		when(s3Adapter.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class), any(S3AsyncClient.class)))
				.thenAnswer(invocation -> {
					var request = invocation.getArgument(0, PutObjectRequest.class);
					var key = request.key();
					var body = invocation.getArgument(1, ByteArrayAsyncRequestBody.class);
					var bytes = ByteArrayAsyncRequestBody.class.getDeclaredField("bytes");
					bytes.setAccessible(true);
					var html = new String((byte[]) bytes.get(body));
					pages.put(key, html);
					return Single.just(PutObjectResponse.builder().build());
				});
		// Run.
		dataIndex.run().blockingAwait();
		// Assert initial list.
		var listCaptor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
		verify(s3Adapter).listObjects(listCaptor.capture(), eq(dataClient));
		assertEquals("data-bucket", listCaptor.getValue().bucket());
		assertEquals("", listCaptor.getValue().prefix());
		// Assert puts.
		var putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		var bodyCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
		verify(s3Adapter, atLeastOnce()).putObject(putCaptor.capture(), bodyCaptor.capture(), eq(dataClient));
		var puts =
				putCaptor.getAllValues().stream().collect(Collectors.toMap(PutObjectRequest::key, Function.identity()));
		System.out.println(puts.keySet());
		assertTrue(puts.containsKey("index.html"));
		assertTrue(puts.containsKey("dir/index.html"));
		assertTrue(puts.containsKey("dir2/index.html"));
		assertEquals(3, puts.size());
		// Parse main index page.
		var mainLinks = Jsoup.parse(pages.get("index.html")).select("a.url");
		assertEquals("/dir/", mainLinks.get(0).attr("href"));
		assertEquals("/dir2/", mainLinks.get(1).attr("href"));
		// Parse dir index page.
		var dirLinks = Jsoup.parse(pages.get("dir/index.html")).select("a.data-file-url");
		System.out.println(pages.get("dir/index.html"));
		assertEquals("/dir/more-data.zip", dirLinks.get(0).attr("href"));
	}
}
