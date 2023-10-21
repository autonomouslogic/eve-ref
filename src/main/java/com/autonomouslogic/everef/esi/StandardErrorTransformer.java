package com.autonomouslogic.everef.esi;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.Response;
import org.reactivestreams.Publisher;

@RequiredArgsConstructor
@Log4j2
public class StandardErrorTransformer implements FlowableTransformer<Response, Response> {
	private final EsiUrl url;

	@Override
	public @NonNull Publisher<Response> apply(@NonNull Flowable<Response> upstream) {
		return upstream.flatMap(response -> {
			var status = response.code();
			if (status == 401 || status == 403) {
				return Flowable.error(new RuntimeException(String.format("Received %s for %s", status, url)));
			}
			if (status == 204
					|| status == 404
					|| (status / 100 == 5 && status != 520) // 520 is specially used for dynamic items handling.
			) {
				log.debug("Ignoring {} response received for {}", status, url);
				return Flowable.empty();
			} else if (status / 100 == 4) {
				log.warn("Ignoring {} response received for {}", status, url);
				return Flowable.empty();
			}
			return Flowable.just(response);
		});
	}
}
