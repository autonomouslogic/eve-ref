package com.autonomouslogic.everef.s3;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

/**
 * Heads objects in S3 for the purpose of populated last-modified time from metadata.
 */
@RequiredArgsConstructor
@Log4j2
class HeadObjectFlowableTransformer implements FlowableTransformer<ListedS3Object, ListedS3Object> {
	@NonNull
	private final S3AsyncClient client;

	@Override
	public Publisher<ListedS3Object> apply(@NonNull Flowable<ListedS3Object> upstream) {
		return upstream.filter(obj -> !obj.isDirectory())
				.flatMap(
						obj -> {
							var req = HeadObjectRequest.builder()
									.bucket(obj.getUrl().getBucket())
									.key(obj.getUrl().getPath())
									.build();
							return Rx3Util.toMaybe(client.headObject(req))
									.flatMap(head -> {
										if (head == null || !head.hasMetadata()) {
											return Maybe.empty();
										}
										var lastModified = getLastModified(obj, head.metadata());
										return Maybe.just(obj.toBuilder()
												.lastModified(lastModified.orElse(null))
												.build());
									})
									.switchIfEmpty(Single.just(obj))
									.toFlowable()
									.compose(Rx3Util.retryWithDelayFlowable(2, Duration.ofSeconds(5)));
						},
						false,
						32);
	}

	private Optional<Instant> getLastModified(@NonNull ListedS3Object obj, @NonNull Map<String, String> metadata) {
		// rclone header
		var srcLastModifiedMillis = Optional.ofNullable(metadata.get("src_last_modified_millis"))
				.map(Long::parseLong)
				.map(Instant::ofEpochMilli);

		// S3 header
		var lastModified = Optional.ofNullable(obj.getLastModified());

		return Stream.of(srcLastModifiedMillis, lastModified)
				.flatMap(Optional::stream)
				.findFirst();
	}
}
