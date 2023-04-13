package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.config.Configs;
import java.util.Objects;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Utility class for dealing with URLs on the API without the Swagger client.
 */
@Value
@Builder(toBuilder = true)
public class EsiUrl {
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
		sb.append(Configs.ESI_BASE_URL.getRequired());
		Objects.requireNonNull(urlPath);
		sb.append(urlPath);
		Objects.requireNonNull(datasource);
		if (!urlPath.contains("?")) {
			sb.append("?");
		} else {
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
