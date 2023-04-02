package com.autonomouslogic.everef.esi;

import com.autonomouslogic.everef.cli.publiccontracts.ContractAbyssalFetcher;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.Rx;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.jsoup.Jsoup;

/**
 * Loads the type IDs for a given meta group by scraping the main EVE Ref website.
 * The results are cached in memory, so repeated calls will return immediately.
 *
 * This is a temporary solution for {@link ContractAbyssalFetcher} until the
 * <a href="https://github.com/autonomouslogic/eve-ref/issues/11">reference dataset is ported</a>.
 */
@Log4j2
@Singleton
public class MetaGroupScraper {
	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	private final String eveRefBasePath = Configs.EVE_REF_BASE_PATH.getRequired();
	private List<Integer> typeIds;

	@Inject
	protected MetaGroupScraper() {}

	public Flowable<Integer> scrapeTypeIds(int metaGroupId) {
		return Flowable.defer(() -> {
			if (typeIds != null) {
				return Flowable.fromIterable(typeIds);
			}
			return loadHtml(metaGroupId)
					.map(html -> {
						var ids = parseHtml(html);
						log.debug(String.format("Scraped %s types from meta group %s", ids.size(), metaGroupId));
						typeIds = ids;
						return ids;
					})
					.flatMapPublisher(Flowable::fromIterable)
					.compose(Rx.offloadFlowable());
		});
	}

	private Single<String> loadHtml(int metaGroupId) {
		var url = eveRefBasePath + "/meta-groups/" + metaGroupId;
		log.debug(String.format("Scraping types from meta group %s: %s", metaGroupId, url));
		return okHttpHelper.get(url, okHttpClient).map(response -> {
			if (response.code() != 200) {
				throw new RuntimeException("Scrape failed: " + response.code());
			}
			var body = Optional.ofNullable(response.body())
					.map(b -> {
						try {
							return b.string();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					})
					.filter(b -> b.length() > 0)
					.orElseThrow();
			return body;
		});
	}

	private List<Integer> parseHtml(String html) {
		var doc = Jsoup.parse(html);
		var links = doc.select("a");
		return links.stream()
				.map(link -> link.attr("href"))
				.filter(href -> href.startsWith("/type/"))
				.map(href -> href.substring(6))
				.map(Integer::parseInt)
				.toList();
	}
}
