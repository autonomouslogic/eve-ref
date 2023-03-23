package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Single;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Log4j2
public class OkHttpHelper {
	@Inject
	protected OkHttpHelper() {}

	public Single<Response> get(String url, OkHttpClient client) {
		return Single.defer(() -> {
			var request = getRequest(url);
			return execute(request, client);
		});
	}

	public Single<Response> execute(Request request, OkHttpClient client) {
		return Single.fromCallable(() -> {
				log.trace(String.format("Requesting %s %s", request.method(), request.url());
				return client.newCall(request).execute();
			})
			.compose(Rx.offloadSingle())
			.onErrorResumeNext(e -> Single.error(new RuntimeException(String.format("Error requesting %s %s", request.method(), request.url()))));
	}

	private Request getRequest(String url) {
		return new Request.Builder().get().url(url).build();
	}
}
