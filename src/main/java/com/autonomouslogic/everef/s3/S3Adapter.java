package com.autonomouslogic.everef.s3;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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
				.timeout(5, TimeUnit.SECONDS)
				.retry(3)
				.observeOn(Schedulers.computation())
				.onErrorResumeNext(e -> Single.error(new RuntimeException(
						String.format("Error putting object into bucket %s at key %s", req.bucket(), req.key()), e)));
	}
}
