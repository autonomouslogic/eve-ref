package com.autonomouslogic.everef.http;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.url.DataUrl;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.OkHttpHelper;
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
	private String prefix = "";

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
					.filter(entry -> {
						log.info(entry);
						return true;
					})
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
		dirs = prependBase(dirs, baseUrl);
		files = prependBase(files, baseUrl);
		return Stream.concat(
						dirs.stream().map(url -> new Entry(false, urlParser.parse(url))),
						files.stream().map(url -> new Entry(true, urlParser.parse(url))))
				.toList();
	}

	private List<String> prependBase(List<String> urls, DataUrl base) {
		return urls.stream().map(url -> prependBase(url, base)).toList();
	}

	private String prependBase(String url, DataUrl base) {
		if (url.contains("://")) {
			return url;
		}
		if (url.startsWith("/")) {
			return dataBaseUrl + url;
		}
		return base.toString() + url;
	}

	private boolean filterPrefix(Entry entry) {
		if (prefix.isEmpty()) {
			return true;
		}
		var base = dataBaseUrl.toString();
		var path = entry.getUrl().toString();
		if (!path.startsWith(base)) {
			throw new RuntimeException("Invalid path, this shouldn't happen: " + path);
		}
		var cut = path.substring(base.length());
		return cut.startsWith(prefix);
	}

	@Value
	private static class Entry {
		boolean file;
		DataUrl url;
	}
}
