package com.autonomouslogic.everef.dataserver;

import com.google.common.net.InetAddresses;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import java.net.Inet6Address;
import java.util.Optional;

/**
 * Resolves the client IP of a request. In order: the first value of {@code CF-Connecting-IP}, the first value of
 * {@code X-Forwarded-For}, then the network peer address. A header value that isn't an IP literal is skipped.
 */
public final class ClientIps {
	private static final HeaderName CF_CONNECTING_IP = HeaderNames.create("CF-Connecting-IP");

	private ClientIps() {}

	/** CF-Connecting-IP, then X-Forwarded-For, then the network peer. Also the rate limiting key. */
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

	/**
	 * The key a client is rate limited under. IPv6 addresses are reduced to their /64 so one host can't rotate through
	 * its address block. Anything that isn't an IP literal is used as is.
	 */
	public static String rateLimitKey(String ip) {
		if (ip == null || !InetAddresses.isInetAddress(ip)) {
			return ip;
		}
		var address = InetAddresses.forString(ip);
		if (address instanceof Inet6Address v6) {
			var bytes = v6.getAddress();
			for (int i = 8; i < bytes.length; i++) {
				bytes[i] = 0;
			}
			try {
				return InetAddresses.toAddrString(java.net.InetAddress.getByAddress(bytes)) + "/64";
			} catch (java.net.UnknownHostException e) {
				throw new IllegalStateException(e);
			}
		}
		return InetAddresses.toAddrString(address);
	}
}
