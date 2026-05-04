package com.autonomouslogic.everef.cli.wars;

import com.autonomouslogic.everef.http.OkHttpWrapper;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

/**
 * Handles hash corrections for killmails using Zkillboard data.
 * Searches the last 30 days of Zkillboard history to find corrected hashes for failed killmail fetches.
 */
@Log4j2
public class ZkillboardHashCorrector {
	private static final Pattern ESI_KILL_PATTERN =
			Pattern.compile("esi\\.evetech\\.net\\/latest\\/killmails\\/[0-9]+\\/[0-9a-f]+\\/");

	@Inject
	@Named("esi")
	protected OkHttpWrapper okHttpWrapper;

	@Inject
	protected ZkillboardHashCorrector() {}

	/**
	 * Fetches the killmail hash directly from the zkillboard website.
	 *
	 * @param killmailId the killmail ID
	 * @param originalHash the original hash that failed
	 * @return the corrected hash, or empty if not found or CCP VERIFIED
	 */
	@SneakyThrows
	public Optional<String> correctHash(long killmailId, String originalHash) {
		var url = String.format("https://zkillboard.com/kill/%s/", killmailId);
		var response = okHttpWrapper.get(url);
		if (response.code() != 200) {
			throw new RuntimeException(String.format("Zkillboard returned status %s for url %s", response.code(), url));
		}
		String body = response.body().string();
		Matcher matcher = ESI_KILL_PATTERN.matcher(body);
		if (!matcher.find()) {
			throw new RuntimeException(String.format("No matching killmail URL found on %s", url));
		}
		String esiUrl = matcher.group();
		List<String> parts =
				Stream.of(esiUrl.split("/")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
		String hash = parts.get(parts.size() - 1);
		return Optional.of(hash);
	}
}
