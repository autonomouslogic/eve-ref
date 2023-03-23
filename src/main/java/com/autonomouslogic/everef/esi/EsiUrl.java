package com.autonomouslogic.everef.esi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.net.URL;
import java.util.Objects;

/**
 * Utility class for dealing with URLs on the API without the Swagger client.
 */
@Value
@Builder(toBuilder = true)
public class EsiUrl {
	private static final String BASE_URL = "https://esi.evetech.net/latest";

	@NonNull
	String urlPath;
	@NonNull
	@lombok.Builder.Default
	String datasource = "tranquility";
	@NonNull
	@lombok.Builder.Default
	String language = "en";
	Integer page;

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(BASE_URL);
		Objects.requireNonNull(urlPath);
		sb.append(urlPath);
		Objects.requireNonNull(datasource);
		if (!urlPath.contains("?")) {
			sb.append("?");
		}
		else {
			sb.append("&");
		}
		sb.append("datasource=").append(datasource);
		if (language != null) {
			sb.append("&language=").append(language);
		}
		if (page != null) {
			if (page < 1) {
				throw new IllegalArgumentException("Page must be >= 1");
			}
			sb.append("&page=").append(page);
		}
		return sb.toString();
	}
}
