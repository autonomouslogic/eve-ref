package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiConstants;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.openapi.esi.api.CharacterApi;
import com.autonomouslogic.everef.openapi.esi.api.CorporationApi;
import com.autonomouslogic.everef.openapi.esi.api.WalletApi;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.openapi.esi.model.GetCharactersCharacterIdOk;
import com.autonomouslogic.everef.openapi.esi.model.GetCorporationsCorporationIdOk;
import com.autonomouslogic.everef.pug.NumberFormats;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.VirtualThreads;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.Ordering;
import com.google.common.hash.Hashing;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Fetches donations to EVE Ref from the ESI.
 */
@Log4j2
public class FetchDonations implements Command {
	private static final double MIN_RECENT_DONATION_AMOUNT = 10_000_000;
	private static final Duration RECENT_LOOKBACK = Duration.ofDays(1);
	private static final Duration TOP_LOOKBACK = Duration.ofDays(90);
	private static final int RECENT_LIMIT = 3;
	private static final int TOP_LIMIT = 15;

	private static final List<String> DONATION_REF_TYPES = List.of("player_donation", "corporation_account_withdrawal");
	public static final String DONATIONS_LIST_FILE = "donations-all.json";
	public static final String DONATIONS_SUMMARY_FILE = "donations.json";
	private static final Pattern DISALLOWED_DISCORD_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_\\- ]");
	private static final List<String> EMOJIS = List.of(
			":money_with_wings:",
			":dollar:",
			":moneybag:",
			":money_mouth:",
			":trophy:",
			":gem:",
			":sunglasses:",
			":yen:",
			":gift:",
			":atm:",
			":pound:",
			":person_bowing:",
			":partying_face:",
			":beers:",
			":thumbsup:");

	@Inject
	protected EsiAuthHelper esiAuthHelper;

	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected WalletApi walletApi;

	@Inject
	protected CharacterApi characterApi;

	@Inject
	protected CorporationApi corporationApi;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected UrlParser urlParser;

	@Inject
	protected S3Util s3Util;

	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	@Named("static")
	protected S3AsyncClient s3Client;

	@Inject
	protected OkHttpWrapper okHttpWrapper;

	private final String eveRefOwnerHash = Configs.EVE_REF_CHARACTER_OWNER_HASH.getRequired();

	private S3Url staticUrl;

	private final Duration cacheControlMaxAge = Configs.STATIC_CACHE_CONTROL_MAX_AGE.getRequired();

	private final Optional<URL> discordUrl;

	@Inject
	protected FetchDonations() {
		discordUrl = Configs.DONATIONS_DISCORD_WEBHOOK_URL.get().map(spec -> {
			try {
				return new URL(spec);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Inject
	protected void init() {
		staticUrl = (S3Url) urlParser.parse(Configs.STATIC_PATH.getRequired());
	}

	@SneakyThrows
	public void run() {
		var accessToken = getAccessToken();
		var verified = esiAuthHelper.verify(accessToken);
		var characterId = (int) verified.getCharacterId();
		var character = getCharacter(characterId);
		var corporationId = character.getCorporationId();
		var corporation = getCorporation(corporationId);
		log.info(
				"Using character '{}' [{}] of '{}' [{}]",
				verified.getCharacterName(),
				characterId,
				corporation.getName(),
				corporationId);

		var previous = downloadFileJsonList(DONATIONS_LIST_FILE, DonationEntry.class);
		log.info("Loaded {} previous donations", previous.size());

		var donations = getDonations(characterId, corporationId, accessToken);
		log.info("Fetched {} donations from ESI", donations.size());

		var newDonations = resolveNewDonations(previous, donations);
		log.info("Found {} new donations", newDonations.size());

		var allDonations = Stream.concat(previous.stream(), newDonations.stream())
				.collect(Collectors.toMap(
						DonationEntry::getId, donationEntry -> donationEntry, (a, b) -> b, TreeMap::new));
		uploadFile(objectMapper.writeValueAsBytes(allDonations.values()), DONATIONS_LIST_FILE);

		var summary = buildSummary(allDonations.values());
		uploadFile(objectMapper.writeValueAsBytes(summary), DONATIONS_SUMMARY_FILE);

		notifyDiscord(newDonations);
	}

	private List<DonationEntry> resolveNewDonations(List<DonationEntry> previous, List<DonationEntry> donations) {
		var previousIds = previous.stream().map(DonationEntry::getId).toList();
		return donations.stream().filter(d -> !previousIds.contains(d.getId())).toList();
	}

	private List<DonationEntry> getDonations(int characterId, int corporationId, String accessToken) {
		return Stream.concat(
						getCharacterDonations(characterId, accessToken).stream(),
						getCorporationDonations(corporationId, accessToken).stream())
				.filter(d -> d.getSecondPartyId() == characterId || d.getSecondPartyId() == corporationId)
				.filter(d -> d.getFirstPartyId() != characterId && d.getFirstPartyId() != corporationId)
				.map(d -> {
					return resolveDonorName(d);
				})
				.sorted(Ordering.natural().onResultOf(DonationEntry::getDate))
				.toList();
	}

	@SneakyThrows
	private List<DonationEntry> getCharacterDonations(int characterId, String accessToken) {
		var journals = esiHelper
				.fetchPages(page ->
						VirtualThreads.offload(() -> walletApi.getCharactersCharacterIdWalletJournalWithHttpInfo(
								characterId, EsiConstants.Datasource.tranquility.toString(), null, page, accessToken)))
				.doOnNext(e -> log.debug("Character journal: {}", e))
				.filter(e -> DONATION_REF_TYPES.contains(e.getRefType().toString()))
				.map(e -> DonationEntry.builder()
						.id(e.getId())
						.date(e.getDate().toInstant())
						.firstPartyId(e.getFirstPartyId())
						.secondPartyId(e.getSecondPartyId())
						.amount(e.getAmount())
						.build())
				.toList()
				.blockingGet();
		return journals;
	}

	@SneakyThrows
	private List<DonationEntry> getCorporationDonations(int corporationId, String accessToken) {
		var journals = esiHelper
				.fetchPages(page -> VirtualThreads.offload(
						() -> walletApi.getCorporationsCorporationIdWalletsDivisionJournalWithHttpInfo(
								corporationId,
								1,
								EsiConstants.Datasource.tranquility.toString(),
								null,
								page,
								accessToken)))
				.doOnNext(e -> log.debug("Corporation journal: {}", e))
				.filter(e -> DONATION_REF_TYPES.contains(e.getRefType().toString()))
				.map(e -> DonationEntry.builder()
						.id(e.getId())
						.date(e.getDate().toInstant())
						.firstPartyId(e.getFirstPartyId())
						.secondPartyId(e.getSecondPartyId())
						.amount(e.getAmount())
						.build())
				.toList()
				.blockingGet();
		return journals;
	}

	private String getAccessToken() {
		return esiAuthHelper.getTokenStringForOwnerHash(eveRefOwnerHash);
	}

	private void uploadFile(byte[] contents, String name) {
		var path = staticUrl.resolve(name);
		var put = s3Util.putObjectRequest(contents.length, path, "application/json", cacheControlMaxAge);
		log.debug(String.format("Uploading %s to %s", name, path));
		s3Adapter.putObject(put, contents, s3Client).ignoreElement().blockingAwait();
	}

	@SneakyThrows
	private byte[] downloadFile(String name) {
		var path = staticUrl.resolve(name);
		var get = s3Util.getObjectRequest(path);
		log.debug(String.format("Downloading %s", path));
		var p = s3Adapter.getObject(get, s3Client).blockingGet();
		var b = IOUtils.toByteArray(new FileInputStream(p.getRight().toFile()));
		p.getRight().toFile().delete();
		return b;
	}

	@SneakyThrows
	private <T> List<T> downloadFileJsonList(String name, Class<T> clazz) {
		try {
			return objectMapper.readValue(
					downloadFile(name), objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz));
		} catch (CompletionException e) {
			if (e.getCause() instanceof NoSuchKeyException) {
				log.warn("Error downloading {}", name);
				return List.of();
			}
			throw e;
		}
	}

	@SneakyThrows
	private @NotNull GetCorporationsCorporationIdOk getCorporation(int corporationId) throws ApiException {
		return VirtualThreads.offload(() -> corporationApi.getCorporationsCorporationId(
				corporationId, EsiConstants.Datasource.tranquility.toString(), null));
	}

	@SneakyThrows
	private @NotNull GetCharactersCharacterIdOk getCharacter(int characterId) throws ApiException {
		return VirtualThreads.offload(() -> characterApi.getCharactersCharacterId(
				characterId, EsiConstants.Datasource.tranquility.toString(), null));
	}

	private static @NotNull SummaryFile buildSummary(Collection<DonationEntry> donations) {
		return SummaryFile.builder()
				.recent(summarise(donations, RECENT_LOOKBACK, MIN_RECENT_DONATION_AMOUNT, RECENT_LIMIT))
				.top(summarise(donations, TOP_LOOKBACK, 0.0, TOP_LIMIT))
				.build();
	}

	private static @NotNull List<SummaryEntry> summarise(Collection<DonationEntry> donations) {
		return summarise(donations, null, 0.0, Integer.MAX_VALUE);
	}

	private static @NotNull List<SummaryEntry> summarise(
			Collection<DonationEntry> donations, Duration lookback, double minAmount, int limit) {
		var now = Instant.now();
		var totals = new HashMap<String, SummaryEntry>();
		for (var entry : donations) {
			if (lookback != null && entry.getDate().isBefore(now.minus(lookback))) {
				continue;
			}
			if (entry.getAmount() < minAmount) {
				continue;
			}
			var donor = entry.getDonorName();
			var sum = totals.get(donor);
			if (sum == null) {
				sum = SummaryEntry.builder()
						.donorName(donor)
						.amount(entry.getAmount())
						.characterId(entry.getCharacterId())
						.corporationId(entry.getCorporationId())
						.build();
			} else {
				sum = sum.toBuilder()
						.amount(sum.getAmount() + entry.getAmount())
						.build();
			}
			totals.put(donor, sum);
		}
		return totals.values().stream()
				.sorted(Ordering.natural().reverse().onResultOf(SummaryEntry::getAmount))
				.limit(limit)
				.toList();
	}

	private void notifyDiscord(List<DonationEntry> donations) {
		if (donations.isEmpty()) {
			return;
		}
		var totals = summarise(donations);
		var summary = totals.stream()
				.sorted(Ordering.natural().reverse().onResultOf(e -> e.getAmount()))
				.map(d -> String.format(
						"**%s** donated %s ISK %s",
						d.getDonorName().replaceAll(DISALLOWED_DISCORD_CHARACTERS.pattern(), "?"),
						NumberFormats.formatMoney(d.getAmount()),
						emoji(d)))
				.collect(Collectors.joining("\n"));
		notifyDiscord(summary);
	}

	@SneakyThrows
	private void notifyDiscord(String message) {
		if (discordUrl.isEmpty()) {
			log.debug("No Discord webhook URL configured");
			return;
		}
		log.debug("Notifying Discord");
		var body = objectMapper.createObjectNode();
		body.put("content", message);
		log.trace("Discord notification: {}", body);
		try (var response = okHttpWrapper.post(
				discordUrl.get().toString(),
				objectMapper.writeValueAsBytes(body),
				r -> r.header("Content-Type", "application/json"))) {
			if (response.code() < 200 || response.code() >= 300) {
				log.warn("Error notifying Discord: {}", response);
			} else {
				log.debug("Discord notified: {}", response);
			}
		}
	}

	@SneakyThrows
	private @Nullable DonationEntry resolveDonorName(DonationEntry entry) {
		var id = entry.getFirstPartyId();
		try {
			var character = getCharacter(id);
			return entry.toBuilder()
					.donorName(character.getName())
					.characterId(id)
					.build();
		} catch (Exception e) {
			var apiException = ExceptionUtils.throwableOfType(e, ApiException.class);
			if (apiException == null || apiException.getCode() != 404) {
				throw e;
			}
			try {
				var corporation = getCorporation(id);
				return entry.toBuilder()
						.donorName(corporation.getName())
						.corporationId(id)
						.build();
			} catch (ApiException e2) {
				var apiException2 = ExceptionUtils.throwableOfType(e, ApiException.class);
				if (apiException2 == null || apiException2.getCode() != 404) {
					throw e;
				}
			}
		}
		return null;
	}

	private String emoji(SummaryEntry entry) {
		var hash = Math.abs(Hashing.murmur3_128()
				.newHasher()
				.putString(entry.getDonorName(), StandardCharsets.UTF_8)
				.putDouble(entry.getAmount())
				.hash()
				.asInt());
		var idx = (hash % EMOJIS.size());
		return EMOJIS.get(idx);
	}

	@Value
	@Builder(toBuilder = true)
	@Jacksonized
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class DonationEntry {
		@JsonProperty
		long id;

		@JsonProperty
		Instant date;

		@JsonProperty
		Integer firstPartyId;

		@JsonProperty
		Integer secondPartyId;

		@JsonProperty
		Integer characterId;

		@JsonProperty
		Integer corporationId;

		@JsonProperty
		String donorName;

		@JsonProperty
		double amount;
	}

	@Value
	@Builder(toBuilder = true)
	@Jacksonized
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class SummaryFile {
		@JsonProperty
		List<SummaryEntry> recent;

		@JsonProperty
		List<SummaryEntry> top;
	}

	@Value
	@Builder(toBuilder = true)
	@Jacksonized
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public static class SummaryEntry {
		@JsonProperty
		String donorName;

		@JsonProperty
		double amount;

		@JsonProperty
		Integer characterId;

		@JsonProperty
		Integer corporationId;
	}
}
