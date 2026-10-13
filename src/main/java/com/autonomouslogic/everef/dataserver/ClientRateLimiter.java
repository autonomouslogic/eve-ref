package com.autonomouslogic.everef.dataserver;

import com.autonomouslogic.everef.config.Configs;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Per-client token bucket rate limiter for requests that fetch an object from B2. One bucket per client key, held in
 * a bounded cache so scanning many addresses can't grow memory without limit. Evicting an idle bucket never gives a
 * client extra tokens, because buckets are only evicted after they would have refilled completely.
 */
@Singleton
public class ClientRateLimiter {
	private final boolean enabled;
	private final int burst;
	private final Duration refillPeriod;
	private final Set<String> allowlist;
	private final TimeMeter timeMeter;
	private final Cache<String, Bucket> buckets;

	@Inject
	protected ClientRateLimiter() {
		this(
				Configs.DATA_SERVER_RATE_LIMIT_ENABLED.getRequired(),
				Configs.DATA_SERVER_RATE_LIMIT_BURST.getRequired(),
				Configs.DATA_SERVER_RATE_LIMIT_PER_S.getRequired(),
				Configs.DATA_SERVER_RATE_LIMIT_MAX_CLIENTS.getRequired(),
				Configs.DATA_SERVER_RATE_LIMIT_ALLOWLIST.getRequired(),
				TimeMeter.SYSTEM_NANOTIME);
	}

	public ClientRateLimiter(
			boolean enabled, int burst, double perSecond, int maxClients, String allowlist, TimeMeter timeMeter) {
		this.enabled = enabled;
		this.burst = burst;
		this.refillPeriod = Duration.ofNanos(Math.max(1, Math.round(1_000_000_000d / perSecond)));
		this.allowlist = Arrays.stream(allowlist.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(ClientIps::rateLimitKey)
				.collect(Collectors.toUnmodifiableSet());
		this.timeMeter = timeMeter;
		var fullRefill = refillPeriod.multipliedBy(burst);
		this.buckets = CacheBuilder.newBuilder()
				.maximumSize(maxClients)
				.expireAfterAccess(fullRefill.plusSeconds(60).toNanos(), TimeUnit.NANOSECONDS)
				.ticker(new com.google.common.base.Ticker() {
					@Override
					public long read() {
						return timeMeter.currentTimeNanos();
					}
				})
				.build();
	}

	/**
	 * Takes one token for the client, if it has any.
	 *
	 * @param ip the resolved client IP
	 */
	public Result tryConsume(String ip) {
		if (!enabled) {
			return Result.ALLOWED;
		}
		var key = ClientIps.rateLimitKey(ip);
		if (allowlist.contains(key)) {
			return Result.ALLOWED;
		}
		Bucket bucket;
		try {
			bucket = buckets.get(key, this::newBucket);
		} catch (java.util.concurrent.ExecutionException e) {
			throw new IllegalStateException(e);
		}
		var probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			return Result.ALLOWED;
		}
		var seconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
		return new Result(false, seconds);
	}

	private Bucket newBucket() {
		return Bucket.builder()
				.addLimit(Bandwidth.builder()
						.capacity(burst)
						.refillGreedy(1, refillPeriod)
						.build())
				.withCustomTimePrecision(timeMeter)
				.build();
	}

	/** @param retryAfterSeconds whole seconds until a token is available; 0 when allowed */
	public record Result(boolean allowed, long retryAfterSeconds) {
		static final Result ALLOWED = new Result(true, 0);
	}
}
