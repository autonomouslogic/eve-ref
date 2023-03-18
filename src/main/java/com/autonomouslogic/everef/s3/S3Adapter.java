package com.autonomouslogic.everef.s3;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

public class S3Adapter {
	public Flowable<ListObjectsV2Response> listObjects(ListObjectsV2Request req, S3AsyncClient client) {
		return Flowable.fromPublisher(client.listObjectsV2Paginator(req)).observeOn(Schedulers.computation());
	}
}
