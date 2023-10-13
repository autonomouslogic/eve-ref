package com.autonomouslogic.everef.s3;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.url.S3Url;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Singleton
@Log4j2
public class S3Adapter {
	@Inject
	protected S3Adapter() {}

	public Flowable<ListedS3Object> listObjects(@NonNull S3Url url, boolean recursive, @NonNull S3AsyncClient client) {
		return Flowable.defer(() -> {
			var builder = ListObjectsV2Request.builder().bucket(url.getBucket()).prefix(url.getPath());
			if (!recursive) {
				builder = builder.delimiter("/");
			}
			return listObjects(builder.build(), client).flatMap(response -> {
				Flowable<ListedS3Object> commons = response.commonPrefixes() == null
						? Flowable.empty()
						: Flowable.fromIterable(response.commonPrefixes())
								.map(common -> ListedS3Object.create(common, url.getBucket()));
				Flowable<ListedS3Object> contents = response.contents() == null
						? Flowable.empty()
						: Flowable.fromIterable(response.contents())
								.filter(obj -> obj.size() != null
										&& obj.size() > 0) // S3 doesn't list directories, but Backblaze does.
								.map(obj -> ListedS3Object.create(obj, url.getBucket()));
				return Flowable.concatArray(commons, contents);
			});
		});
	}

	private Flowable<ListObjectsV2Response> listObjects(
			@NonNull ListObjectsV2Request req, @NonNull S3AsyncClient client) {
		var s3Url =
				S3Url.builder().bucket(req.bucket()).path(req.prefix()).build().toString();
		var count = new AtomicInteger();
		return Flowable.fromPublisher(client.listObjectsV2Paginator(req))
				.observeOn(Schedulers.computation())
				.onErrorResumeNext(e -> Flowable.error(new RuntimeException(
						String.format("Error listing bucket %s at prefix %s", req.bucket(), req.prefix()), e)))
				.doOnNext(response -> {
					var n = response.contents().size();
					var total = count.getAndAdd(n);
					log.debug(String.format("Listing %s: %s+%s objects", s3Url, total, n));
				});
	}

	public Single<PutObjectResponse> putObject(
			@NonNull PutObjectRequest req, @NonNull AsyncRequestBody body, @NonNull S3AsyncClient client) {
		return Rx3Util.toSingle(client.putObject(req, body))
				.timeout(120, TimeUnit.SECONDS)
				.retry(3, e -> {
					log.warn(String.format("Retrying put to %s", req.key()), e);
					return true;
				})
				.observeOn(Schedulers.computation())
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format("Error putting object to s3://%s/%s", req.bucket(), req.key()), e)));
	}

	public Single<PutObjectResponse> putObject(
			@NonNull PutObjectRequest req, @NonNull byte[] bytes, @NonNull S3AsyncClient client) {
		return putObject(req, AsyncRequestBody.fromBytes(bytes), client);
	}

	public Single<PutObjectResponse> putObject(
			@NonNull PutObjectRequest req, @NonNull File file, @NonNull S3AsyncClient client) {
		req = populateLastModified(req, file);
		var bucket = req.bucket();
		var key = req.key();
		return putObject(req, AsyncRequestBody.fromFile(file), client)
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format(
								"Error putting object from file %s to s3://%s/%s", file.getAbsolutePath(), bucket, key),
						e)));
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

	public Single<DeleteObjectResponse> deleteObject(@NonNull DeleteObjectRequest req, @NonNull S3AsyncClient client) {
		return Rx3Util.toSingle(client.deleteObject(req))
				.timeout(120, TimeUnit.SECONDS)
				.retry(3, e -> {
					log.warn(String.format("Retrying delete to %s", req.key()), e);
					return true;
				})
				.observeOn(Schedulers.computation())
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format("Error deleting object to s3://%s/%s", req.bucket(), req.key()), e)));
	}

	public FlowableTransformer<ListedS3Object, ListedS3Object> headLastModified(@NonNull S3AsyncClient client) {
		return new HeadObjectFlowableTransformer(client);
	}
}
