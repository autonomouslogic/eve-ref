package com.autonomouslogic.everef.url;

import java.net.URI;

public interface DataUrl {
	String getProtocol();

	String getPath();

	String toString();

	default URI toUri() {
		return URI.create(toString());
	}
}
