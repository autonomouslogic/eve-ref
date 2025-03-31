package com.autonomouslogic.everef.http;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.url.UrlParser;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;

/**
 * Crawls the index pages on the data site and returns the available files.
 */
@Log4j2
public class DataCrawler {
	@Inject
	protected UrlParser urlParser;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Setter
	private String prefix = null;

	@Inject
	protected DataCrawler() {}

	public List<DataUrl> crawl() {
		var url = urlParser.parse(dataBaseUrl);
		if (!url.getProtocol().equals("http")) {
			throw new RuntimeException(Configs.DATA_BASE_URL.getName() + " must be an HTTP URL");
		}
		return crawl(url);
	}

	@SneakyThrows
	private List<DataUrl> crawl(DataUrl url) {
		try (var response = okHttpWrapper.get(url.toString())) {
			if (response.code() != 200) {
				log.warn("Failed fetching {} - code: {}", url, response.code());
				return List.of();
			}
			var html = response.body().string();
			return parseEntries(html, url).stream()
					.filter(this::filterPrefix)
					.flatMap(entry -> {
						if (entry.file) {
							return Stream.of(entry.url);
						}
						return crawl(entry.url).stream();
					})
					.toList();
		}
	}

	private List<Entry> parseEntries(String html, DataUrl baseUrl) {
		var doc = Jsoup.parse(html);
		var dirs = doc.select("tr.data-dir a").stream().map(e -> e.attr("href")).toList();
		var files =
				doc.select("tr.data-file a").stream().map(e -> e.attr("href")).toList();
		var dirUrls = prependBase(dirs, baseUrl);
		var fileUrls = prependBase(files, baseUrl);
		return Stream.concat(
						dirUrls.stream().map(url -> new Entry(false, url)),
						fileUrls.stream().map(url -> new Entry(true, url)))
				.toList();
	}

	private List<DataUrl> prependBase(List<String> urls, DataUrl base) {
		return urls.stream().map(url -> prependBase(url, base)).toList();
	}

	private DataUrl prependBase(String url, DataUrl base) {
		if (url.contains("://")) {
			return urlParser.parse(url);
		}
		var sub = base.resolve(url);
		return sub;
	}

	private boolean filterPrefix(Entry entry) {
		var prefix = getPrefix();
		if (prefix.isEmpty()) {
			return true;
		}
		var path = entry.getUrl().getPath();
		return path.startsWith(prefix) || prefix.startsWith(path);
	}

	private String getPrefix() {
		var prefix = this.prefix;
		if (Strings.isNullOrEmpty(prefix)) {
			return dataBaseUrl.getPath();
		} else {
			if (prefix.startsWith("/")) {
				prefix = prefix.substring(1);
			}
			var resolved = dataBaseUrl.resolve(prefix).getPath();
			if (!resolved.startsWith("/")) {
				resolved = "/" + resolved;
			}
			return resolved;
		}
	}

	@Value
	private static class Entry {
		boolean file;
		DataUrl url;
	}
}
