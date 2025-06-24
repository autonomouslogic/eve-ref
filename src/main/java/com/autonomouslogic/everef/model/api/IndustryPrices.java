package com.autonomouslogic.everef.model.api;

import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperties;
import lombok.experimental.Delegate;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

@Schema
public class IndustryPrices {
	private final Map<String, BigDecimal> prices = new HashMap<>();

	public String toUrlQueryString(@NonNull String prefix) {
		// Based on the generated code for deep objects in URLs, which appear to not be properly supported.
		// https://github.com/OpenAPITools/openapi-generator/issues/19142
		// https://github.com/OpenAPITools/openapi-generator/issues/16183
		// https://swagger.io/docs/specification/v3_0/data-models/dictionaries/

		if (StringUtils.isEmpty(prefix)) {
			throw new IllegalArgumentException("prefix must not be null or empty");
		}

		var joiner = new StringJoiner("&");
		for (var entry : entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue().toString();
			joiner.add(String.format("%s[%s]=%s", prefix,

				ApiClient.urlEncode(ApiClient.valueToString(key)),
				ApiClient.urlEncode(ApiClient.valueToString(value))

			));
		}

		return joiner.toString();
	}

	public BigDecimal get(Object key) {
		return prices.get(key);
	}

	public BigDecimal put(String key, BigDecimal value) {
		return prices.put(key, value);
	}

	public Set<Map.Entry<String, BigDecimal>> entrySet() {
		return prices.entrySet();
	}
}
