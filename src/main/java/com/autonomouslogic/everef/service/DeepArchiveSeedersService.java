package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.model.api.deeparchive.DeepArchiveSeeders;
import com.autonomouslogic.everef.util.Bencode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Scrapes the trackers for seeder counts of every torrent listed in the Deep Archive RSS feed.
 * Nothing is fetched until a request needs it. Results are then cached in memory, so the feed and trackers are hit at
 * most once per {@link Configs#DEEP_ARCHIVE_SEEDERS_CACHE_DURATION}.
 * <p>
 * Every configured tracker is scraped. A tracker that fails, or returns a failure reason, is skipped. For each torrent
 * the lowest count reported by a responding tracker is used. A tracker that doesn't list a torrent doesn't count as
 * zero for it. Torrents no responding tracker lists get zero.
 */
@Singleton
@Log4j2
public class DeepArchiveSeedersService {
	public static final String FEED_PATH = "/deep-archive/everef-eve-online-deep-archive.xml";
	public static final String TORRENT_NAMESPACE = "http://xmlns.ezrss.it/0.1/";

	private static final Pattern INFO_HASH = Pattern.compile("[0-9a-f]{40}");
	private static final HexFormat HEX = HexFormat.of();

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	private final String feedUrl;
	private final List<String> scrapeUrls;

	private final Duration cacheDuration;

	private final Duration retryDuration;
	private final ReentrantLock lock = new ReentrantLock();
	private volatile DeepArchiveSeeders cached;
	private volatile Instant nextRefresh = Instant.EPOCH;

	@Inject
	protected DeepArchiveSeedersService() {
		feedUrl = Configs.DATA_BASE_URL.getRequired().toString().replaceAll("/+$", "") + FEED_PATH;
		scrapeUrls = Arrays.stream(
						Configs.DEEP_ARCHIVE_TRACKER_SCRAPE_URLS.getRequired().split(","))
				.map(String::trim)
				.filter(url -> !url.isEmpty())
				.toList();
		cacheDuration = Configs.DEEP_ARCHIVE_SEEDERS_CACHE_DURATION.getRequired();
		retryDuration = Configs.DEEP_ARCHIVE_SEEDERS_RETRY_DURATION.getRequired();
	}

	/**
	 * Returns the cached seeder counts, refreshing them if they're older than the cache duration.
	 * If a refresh fails, the last good result is returned and the refresh is retried after the retry duration.
	 */
	public DeepArchiveSeeders getSeeders() {
		if (cached != null && Instant.now().isBefore(nextRefresh)) {
			return cached;
		}
		lock.lock();
		try {
			if (cached != null && Instant.now().isBefore(nextRefresh)) {
				return cached;
			}
			try {
				cached = scrape(fetchInfoHashes());
				nextRefresh = Instant.now().plus(cacheDuration);
			} catch (Exception e) {
				if (cached == null) {
					throw e;
				}
				log.warn("Failed to refresh Deep Archive seeders, serving cached result", e);
				nextRefresh = Instant.now().plus(retryDuration);
			}
			return cached;
		} finally {
			lock.unlock();
		}
	}

	@SneakyThrows
	private List<String> fetchInfoHashes() {
		byte[] body;
		try (var response = okHttpWrapper.get(feedUrl)) {
			if (response.code() != 200) {
				throw new RuntimeException("Unexpected status " + response.code() + " fetching " + feedUrl);
			}
			body = response.body().bytes();
		}
		var factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		var doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
		var nodes = doc.getElementsByTagNameNS(TORRENT_NAMESPACE, "infoHash");
		var hashes = new ArrayList<String>();
		for (int i = 0; i < nodes.getLength(); i++) {
			var hash = nodes.item(i).getTextContent().trim().toLowerCase();
			if (!INFO_HASH.matcher(hash).matches()) {
				throw new RuntimeException("Invalid info-hash in feed: " + hash);
			}
			hashes.add(hash);
		}
		return hashes;
	}

	@SneakyThrows
	private DeepArchiveSeeders scrape(List<String> hashes) {
		var seeders = new LinkedHashMap<String, Integer>();
		var scrapedAt = Instant.now();
		if (hashes.isEmpty()) {
			return DeepArchiveSeeders.builder()
					.seeders(seeders)
					.scrapedAt(scrapedAt)
					.build();
		}
		var lowest = new HashMap<String, Integer>();
		int responded = 0;
		Exception lastError = null;
		for (var scrapeUrl : scrapeUrls) {
			try {
				var counts = scrapeTracker(scrapeUrl, hashes);
				responded++;
				counts.forEach((hash, count) -> lowest.merge(hash, count, Math::min));
			} catch (Exception e) {
				log.warn("Failed to scrape Deep Archive tracker {}", scrapeUrl, e);
				lastError = e;
			}
		}
		if (responded == 0) {
			throw new RuntimeException("No Deep Archive trackers responded", lastError);
		}
		for (var hash : hashes) {
			seeders.put(hash, lowest.getOrDefault(hash, 0));
		}
		return DeepArchiveSeeders.builder()
				.seeders(seeders)
				.scrapedAt(scrapedAt)
				.build();
	}

	/**
	 * Scrapes a single tracker. Returns counts only for the requested hashes that the tracker lists.
	 */
	@SneakyThrows
	@SuppressWarnings("unchecked")
	private Map<String, Integer> scrapeTracker(String scrapeUrl, List<String> hashes) {
		var url = new StringBuilder(scrapeUrl);
		var separator = scrapeUrl.contains("?") ? '&' : '?';
		for (var hash : hashes) {
			url.append(separator).append("info_hash=");
			for (var b : HEX.parseHex(hash)) {
				url.append('%').append(HEX.toHexDigits(b));
			}
			separator = '&';
		}
		byte[] body;
		try (var response = okHttpWrapper.get(url.toString())) {
			if (response.code() != 200) {
				throw new RuntimeException("Unexpected status " + response.code() + " scraping tracker");
			}
			body = response.body().bytes();
		}
		var decoded = (Map<String, Object>) Bencode.decode(body);
		if (decoded.get("failure reason") instanceof byte[] reason) {
			throw new RuntimeException("Tracker scrape failed: " + new String(reason, StandardCharsets.UTF_8));
		}
		var files = (Map<String, Object>) decoded.getOrDefault("files", Map.of());
		Set<String> requested = new HashSet<>(hashes);
		var counts = new HashMap<String, Integer>();
		for (var entry : files.entrySet()) {
			var hash = HEX.formatHex(entry.getKey().getBytes(StandardCharsets.ISO_8859_1));
			if (!requested.contains(hash)) {
				continue;
			}
			var stats = (Map<String, Object>) entry.getValue();
			counts.put(hash, ((Long) stats.getOrDefault("complete", 0L)).intValue());
		}
		return counts;
	}
}
