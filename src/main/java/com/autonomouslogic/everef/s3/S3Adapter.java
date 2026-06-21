package com.autonomouslogic.everef.s3;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.TempFiles;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Singleton
@Log4j2
public class S3Adapter {
	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected S3Adapter() {}

	@SneakyThrows
	public List<ListedS3Object> listObjects(@NonNull S3Url url, boolean recursive, @NonNull S3AsyncClient client) {
		var builder = ListObjectsV2Request.builder().bucket(url.getBucket()).prefix(url.getPath());
		if (!recursive) {
			builder = builder.delimiter("/");
		}
		return listObjects(builder.build(), client);
	}

	@SneakyThrows
	private List<ListedS3Object> listObjects(@NonNull ListObjectsV2Request req, @NonNull S3AsyncClient client) {
		var s3Url =
				S3Url.builder().bucket(req.bucket()).path(req.prefix()).build().toString();
		var result = new ArrayList<ListedS3Object>();
		var latch = new CountDownLatch(1);
		var errorRef = new AtomicReference<Exception>();

		try {
			var paginator = client.listObjectsV2Paginator(req);
			var count = new int[] {0};

			paginator.subscribe(new Subscriber<ListObjectsV2Response>() {
				@Override
				public void onSubscribe(Subscription subscription) {
					subscription.request(Long.MAX_VALUE);
				}

				@Override
				public void onNext(ListObjectsV2Response response) {
					if (response.commonPrefixes() != null) {
						for (var common : response.commonPrefixes()) {
							result.add(ListedS3Object.create(common, req.bucket()));
						}
					}
					if (response.contents() != null) {
						for (var obj : response.contents()) {
							if (obj.size() != null && obj.size() > 0) {
								result.add(ListedS3Object.create(obj, req.bucket()));
							}
						}
					}
					var n = (response.contents() != null ? response.contents().size() : 0);
					count[0] += n;
					log.debug("Listing {}: {} objects", s3Url, count[0]);
				}

				@Override
				public void onError(Throwable throwable) {
					errorRef.set(new RuntimeException(
							String.format("Error listing bucket %s at prefix %s", req.bucket(), req.prefix()),
							throwable));
					latch.countDown();
				}

				@Override
				public void onComplete() {
					latch.countDown();
				}
			});

			latch.await();
			if (errorRef.get() != null) {
				throw errorRef.get();
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException && e.getMessage().contains("Error listing bucket")) {
				throw e;
			}
			throw new RuntimeException(
					String.format("Error listing bucket %s at prefix %s", req.bucket(), req.prefix()), e);
		}
		return result;
	}

	@SneakyThrows
	public Pair<GetObjectResponse, Path> getObject(GetObjectRequest get, S3AsyncClient s3Client) {
		var destination = tempFiles.tempFile("s3", ".tmp");
		var response = getObject(get, destination, s3Client);
		return Pair.of(response, destination);
	}

	@SneakyThrows
	public GetObjectResponse getObject(GetObjectRequest get, Path destination, S3AsyncClient s3Client) {
		try {
			return s3Client.getObject(get, destination).get();
		} catch (Exception e) {
			throw new RuntimeException("Error getting object from s3://" + get.bucket() + "/" + get.key(), e);
		}
	}

	@SneakyThrows
	public PutObjectResponse putObject(
			@NonNull PutObjectRequest req, @NonNull AsyncRequestBody body, @NonNull S3AsyncClient client) {
		var maxRetries = 3;
		var lastException = (Exception) null;

		for (var attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				var future = client.putObject(req, body);
				var response = future.get();
				return response;
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					log.warn("Retrying put to {} (attempt {}/{})", req.key(), attempt, maxRetries, e);
					Thread.sleep(1000); // Brief delay before retry
				}
			}
		}

		throw new RuntimeException(
				String.format(
						"Error putting object to s3://%s/%s after %d attempts", req.bucket(), req.key(), maxRetries),
				lastException);
	}

	@SneakyThrows
	public PutObjectResponse putObject(
			@NonNull PutObjectRequest req, @NonNull byte[] bytes, @NonNull S3AsyncClient client) {
		return putObject(req, AsyncRequestBody.fromBytes(bytes), client);
	}

	@SneakyThrows
	public PutObjectResponse putObject(
			@NonNull PutObjectRequest req, @NonNull File file, @NonNull S3AsyncClient client) {
		req = populateLastModified(req, file);
		try {
			return putObject(req, AsyncRequestBody.fromFile(file), client);
		} catch (Exception e) {
			throw new RuntimeException(
					String.format(
							"Error putting object from file %s to s3://%s/%s",
							file.getAbsolutePath(), req.bucket(), req.key()),
					e);
		}
	}

	@SneakyThrows
	private PutObjectRequest populateLastModified(@NonNull PutObjectRequest req, @NonNull File file) {
		var meta = Optional.ofNullable(req.metadata()).map(HashMap::new).orElseGet(HashMap::new);
		var current = Optional.ofNullable(meta.get(S3HeaderNames.SRC_LAST_MODIFIED_MILLIS));
		if (current.isPresent()) {
			return req;
		}
		var time = Files.getLastModifiedTime(file.toPath()).toInstant();
		meta.put(S3HeaderNames.SRC_LAST_MODIFIED_MILLIS, Long.toString(time.toEpochMilli()));
		return req.toBuilder().metadata(meta).build();
	}

	@SneakyThrows
	public DeleteObjectResponse deleteObject(@NonNull DeleteObjectRequest req, @NonNull S3AsyncClient client) {
		var maxRetries = 3;
		var lastException = (Exception) null;

		for (var attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				var future = client.deleteObject(req);
				var response = future.get();
				return response;
			} catch (Exception e) {
				lastException = e;
				if (attempt < maxRetries) {
					log.warn("Retrying delete to {} (attempt {}/{})", req.key(), attempt, maxRetries, e);
					Thread.sleep(1000);
				}
			}
		}

		throw new RuntimeException(
				String.format(
						"Error deleting object to s3://%s/%s after %d attempts", req.bucket(), req.key(), maxRetries),
				lastException);
	}

	@SneakyThrows
	public List<ListedS3Object> headLastModified(@NonNull List<ListedS3Object> objects, @NonNull S3AsyncClient client) {
		var tasks = objects.stream()
				.map(obj -> (Callable<ListedS3Object>) () -> {
					if (obj.isDirectory()) {
						return obj;
					}

					var lastModified = getObjectLastModified(obj, client);
					if (lastModified.isPresent()) {
						return obj.toBuilder().lastModified(lastModified.get()).build();
					} else {
						return obj;
					}
				})
				.toList();

		return VirtualThreads.callAll(tasks, 10);
	}

	@SneakyThrows
	private Optional<Instant> getObjectLastModified(@NonNull ListedS3Object obj, @NonNull S3AsyncClient client) {
		try {
			var req = HeadObjectRequest.builder()
					.bucket(obj.getUrl().getBucket())
					.key(obj.getUrl().getPath())
					.build();
			var response = client.headObject(req).get();

			if (response == null || !response.hasMetadata()) {
				return Optional.empty();
			}

			var metadata = response.metadata();

			// rclone header
			var srcLastModifiedMillis = Optional.ofNullable(metadata.get(S3HeaderNames.SRC_LAST_MODIFIED_MILLIS))
					.map(Long::parseLong)
					.map(Instant::ofEpochMilli);

			// S3 header
			var lastModified = Optional.ofNullable(obj.getLastModified());

			return srcLastModifiedMillis.isPresent() ? srcLastModifiedMillis : lastModified;
		} catch (Exception e) {
			log.warn("Failed to head object {}", obj.getUrl(), e);
			return Optional.empty();
		}
	}
}
