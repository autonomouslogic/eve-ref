package com.autonomouslogic.everef.dataserver;

import com.google.common.net.InetAddresses;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import java.util.Optional;

/**
 * Resolves the client IP of a request. In order: the first value of {@code CF-Connecting-IP}, the first value of
 * {@code X-Forwarded-For}, then the network peer address. A header value that isn't an IP literal is skipped.
 */
public final class ClientIps {
	private static final HeaderName CF_CONNECTING_IP = HeaderNames.create("CF-Connecting-IP");

	private ClientIps() {}

	/** CF-Connecting-IP, then X-Forwarded-For, then the network peer. */
	public static String fromRequest(ServerRequest req) {
		return resolve(
				req.headers().first(CF_CONNECTING_IP).orElse(null),
				req.headers().first(HeaderNames.X_FORWARDED_FOR).orElse(null),
				req.remotePeer().host());
	}

	public static String resolve(String cfConnectingIp, String forwardedFor, String remoteAddress) {
		return firstIp(cfConnectingIp).or(() -> firstIp(forwardedFor)).orElse(remoteAddress);
	}

	/** The first comma-separated value, trimmed, if it's an IP literal. No DNS lookups are made. */
	static Optional<String> firstIp(String headerValue) {
		if (headerValue == null) {
			return Optional.empty();
		}
		var first = headerValue.split(",", 2)[0].trim();
		return InetAddresses.isInetAddress(first) ? Optional.of(first) : Optional.empty();
	}
}
