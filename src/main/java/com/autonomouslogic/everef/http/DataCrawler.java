package com.autonomouslogic.everef.http;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.url.UrlParser;
import com.google.common.base.Strings;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Setter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.jsoup.Jsoup;

/**
 * Crawls the index pages on the data site and returns the available files.
 */
@Log4j2
public class DataCrawler {
	@Inject
	protected UrlParser urlParser;

	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	private final URI dataBaseUrl = Configs.DATA_BASE_URL.getRequired();

	@Setter
	private String prefix = null;

	@Inject
	protected DataCrawler() {}

	public Flowable<DataUrl> crawl() {
		return Flowable.defer(() -> {
			var url = urlParser.parse(dataBaseUrl);
			if (!url.getProtocol().equals("http")) {
				throw new RuntimeException(Configs.DATA_BASE_URL.getName() + " must be an HTTP URL");
			}
			return crawl(url);
		});
	}

	private Flowable<DataUrl> crawl(DataUrl url) {
		return Flowable.defer(() -> {
			return okHttpHelper
					.get(url.toString(), okHttpClient)
					.flatMapMaybe(response -> {
						if (response.code() != 200) {
							log.warn("Failed fetching {} - code: {}", url, response.code());
							return Maybe.empty();
						}
						return Maybe.just(response.body().string());
					})
					.flatMapPublisher(html -> Flowable.fromIterable(parseEntries(html, url)))
					.filter(this::filterPrefix)
					.flatMap(entry -> {
						if (entry.file) {
							return Flowable.just(entry.url);
						}
						return crawl(entry.url);
					});
		});
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
