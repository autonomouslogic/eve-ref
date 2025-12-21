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

	@lombok.Builder.Default
	String datasource = "tranquility";

	@lombok.Builder.Default
	String language = "en";

	@lombok.Builder.Default
	String basePath = "latest";

	Integer page;

	String before;

	String after;

	public String toString() {
		var sb = new StringBuilder();
		String baseUrl = Configs.ESI_BASE_URL.getRequired().toString();
		sb.append(baseUrl);

		if (basePath != null) {
			// Add separator between baseUrl and basePath if needed
			if (!baseUrl.endsWith("/") && !basePath.startsWith("/")) {
				sb.append("/");
			} else if (baseUrl.endsWith("/") && basePath.startsWith("/")) {
				sb.append(basePath.substring(1));
			} else {
				sb.append(basePath);
			}
		}

		Objects.requireNonNull(urlPath);

		// Add separator between previous part and urlPath if needed
		String currentUrl = sb.toString();
		if (!currentUrl.endsWith("/") && !urlPath.startsWith("/")) {
			sb.append("/");
		} else if (currentUrl.endsWith("/") && urlPath.startsWith("/")) {
			sb.append(urlPath.substring(1));
		} else {
			sb.append(urlPath);
		}

		// Determine if we need to start query params
		boolean hasExistingQuery = urlPath.contains("?");
		boolean needsQueryParams =
				datasource != null || language != null || page != null || before != null || after != null;

		if (!needsQueryParams) {
			return sb.toString();
		}

		// Add query string separator if there's no existing query
		if (!hasExistingQuery) {
			sb.append("?");
		}

		// Add query parameters - first should be false if there's an existing query
		boolean first = !hasExistingQuery;

		if (datasource != null) {
			if (!first) sb.append("&");
			sb.append("datasource=").append(datasource);
			first = false;
		}
		if (language != null) {
			if (!first) sb.append("&");
			sb.append("language=").append(language);
			first = false;
		}
		if (page != null) {
			if (page < 1) {
				throw new IllegalArgumentException("Page must be >= 1");
			}
			if (!first) sb.append("&");
			sb.append("page=").append(page);
			first = false;
		}
		if (after != null) {
			if (!first) sb.append("&");
			sb.append("after=").append(after);
			first = false;
		}
		if (before != null) {
			if (!first) sb.append("&");
			sb.append("before=").append(before);
			first = false;
		}

		return sb.toString();
	}

	public static EsiUrl.Builder modern() {
		return EsiUrl.builder().datasource(null).language(null).basePath(null);
	}
}
