package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.hoboleaks.DynamicAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.rxjava3.core.Single;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;

public class HoboleaksHelper {
	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected HoboleaksHelper() {}

	public Single<Map<Long, DynamicAttributes>> fetchDynamicAttributes() {
		return okHttpHelper.get(Configs.HOBOLEAKS_DYNAMIC_ATTRIBUTES.getRequired().toString(), okHttpClient)
			.map(response -> {
				if (response.code() != 200) {
					throw new RuntimeException("Failed to fetch dynamic attributes from Hoboleaks: " + response.code());
				}
				return response.body().string();
			})
			.map(body -> {
				var type = objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, Long.class, DynamicAttributes.class);
				return objectMapper.readValue(body, type);
			});
	}
}
