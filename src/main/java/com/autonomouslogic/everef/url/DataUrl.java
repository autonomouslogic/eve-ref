package com.autonomouslogic.everef.url;

import java.net.URI;
import lombok.NonNull;

/**
 * Generic interface for URLs for data sources.
 */
public interface DataUrl<T extends DataUrl> extends Comparable<DataUrl> {
	String getProtocol();

	String getPath();

	String toString();

	T resolve(String path);

	default URI toUri() {
		return URI.create(toString());
	}

	@Override
	default int compareTo(@NonNull DataUrl dataUrl) {
		return toUri().compareTo(dataUrl.toUri());
	}
}
