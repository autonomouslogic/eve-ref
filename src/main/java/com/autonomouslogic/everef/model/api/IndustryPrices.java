package com.autonomouslogic.everef.model.api;

import com.autonomouslogic.everef.openapi.api.invoker.ApiClient;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperties;
import lombok.experimental.Delegate;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@Schema
public class IndustryPrices {
	@Delegate
	private final Map<String, BigDecimal> prices = new HashMap<>();

	public String toUrlQueryString(@NonNull String prefix) {
		// Based on the generated code for deep objects in URLs, which appear to not be properly supported.
		// https://github.com/OpenAPITools/openapi-generator/issues/19142
		// https://github.com/OpenAPITools/openapi-generator/issues/16183
		// https://swagger.io/docs/specification/v3_0/data-models/dictionaries/

		String suffix = "";
		String containerSuffix = "";
		String containerPrefix = "";
		if (prefix == null) {
			// style=form, explode=true, e.g. /pet?name=cat&type=manx
			prefix = "";
		} else {
			// deepObject style e.g. /pet?id[name]=cat&id[type]=manx
			prefix = prefix + "[";
			suffix = "]";
			containerSuffix = "]";
			containerPrefix = "[";
		}

		var joiner = new StringJoiner("&");

		// add `alpha_clone_tax` to the URL query string
//		if (getAlphaCloneTax() != null) {
//			joiner.add(String.format("%salpha_clone_tax%s=%s", prefix, suffix, ApiClient.urlEncode(ApiClient.valueToString(getAlphaCloneTax()))));
//		}

		return joiner.toString();
	}
}
