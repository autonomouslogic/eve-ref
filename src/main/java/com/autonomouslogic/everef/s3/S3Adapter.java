package com.autonomouslogic.everef.s3;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Singleton
@Log4j2
public class S3Adapter {
	@Inject
	protected S3Adapter() {}

	public Flowable<ListObjectsV2Response> listObjects(ListObjectsV2Request req, S3AsyncClient client) {
		return Flowable.fromPublisher(client.listObjectsV2Paginator(req))
				.observeOn(Schedulers.computation())
				.onErrorResumeNext(e -> Flowable.error(new RuntimeException(
						String.format("Error listing bucket %s at prefix %s", req.bucket(), req.prefix()), e)));
	}

	public Single<PutObjectResponse> putObject(PutObjectRequest req, AsyncRequestBody body, S3AsyncClient client) {
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

	public Single<PutObjectResponse> putObject(PutObjectRequest req, byte[] bytes, S3AsyncClient client) {
		return putObject(req, AsyncRequestBody.fromBytes(bytes), client);
	}

	public Single<PutObjectResponse> putObject(PutObjectRequest req, File file, S3AsyncClient client) {
		return putObject(req, AsyncRequestBody.fromFile(file), client)
			.onErrorResumeNext(e -> Single.error(new RuntimeException(
				String.format("Error putting object from file %s to s3://%s/%s", file.getAbsolutePath(), req.bucket(), req.key()), e)));
	}
}
