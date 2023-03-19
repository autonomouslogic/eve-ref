package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.inject.AwsModule;
import com.autonomouslogic.everef.inject.S3Module;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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

	@SetEnvironmentVariable(key = "DATA_S3_BUCKET", value = "data-bucket")
	@Test
	void shouldGenerateIndexPages() {
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
								.map(f -> S3Object.builder().key(f).build())
								.collect(Collectors.toList()))
						.build())));
		dataIndex.run().blockingAwait();
		var putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(dataClient, times(3)).putObject(putCaptor.capture(), any(AsyncRequestBody.class));
		var puts =
				putCaptor.getAllValues().stream().collect(Collectors.toMap(PutObjectRequest::key, Function.identity()));
		assertEquals(3, puts.size());
	}
}
