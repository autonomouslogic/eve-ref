package com.autonomouslogic.everef.cli;

import com.autonomouslogic.commons.rxjava3.Rx3Util;
import com.autonomouslogic.everef.cli.structures.StructureScrapeHelper;
import com.autonomouslogic.everef.cli.structures.StructureStore;
import com.autonomouslogic.everef.cli.structures.source.Adam4EveBackfillStructureSource;
import com.autonomouslogic.everef.cli.structures.source.BackfillPublicStructureSource;
import com.autonomouslogic.everef.cli.structures.source.MarketOrdersStructureSource;
import com.autonomouslogic.everef.cli.structures.source.OldStructureSource;
import com.autonomouslogic.everef.cli.structures.source.PublicContractsStructureSource;
import com.autonomouslogic.everef.cli.structures.source.PublicStructureSource;
import com.autonomouslogic.everef.cli.structures.source.SirSmashAlotBackfillStructureSource;
import com.autonomouslogic.everef.cli.structures.source.SovereigntyStructureSource;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiAuthHelper;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.esi.LocationPopulator;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.mvstore.MVStoreUtil;
import com.autonomouslogic.everef.openapi.esi.apis.CharacterApi;
import com.autonomouslogic.everef.openapi.esi.apis.CorporationApi;
import com.autonomouslogic.everef.openapi.esi.apis.UniverseApi;
import com.autonomouslogic.everef.openapi.esi.apis.WalletApi;
import com.autonomouslogic.everef.openapi.esi.models.GetCharactersCharacterIdWalletJournal200Ok;
import com.autonomouslogic.everef.openapi.refdata.apis.RefdataApi;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.JsonUtil;
import com.autonomouslogic.everef.util.ProgressReporter;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.h2.mvstore.MVStore;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.autonomouslogic.everef.util.ArchivePathFactory.STRUCTURES;
import static com.autonomouslogic.everef.util.EveConstants.STANDARD_MARKET_HUB_I_TYPE_ID;

/**
 * Fetches donations to EVE Ref from the ESI.
 */
@Log4j2
public class FetchDonations implements Command {
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

	private final String eveRefOwnerHash = Configs.EVE_REF_CHARACTER_OWNER_HASH.getRequired();

	@Inject
	protected FetchDonations() {}

	@Inject
	protected void init() {
	}

	public Completable run() {
		return Completable.fromAction(() -> {
			var accessToken = getAccessToken().blockingGet();
			var verified = esiAuthHelper.verify(accessToken).blockingGet();

			log.info("Using character '{}' [{}]", verified.getCharacterName(), verified.getCharacterId());
			var characterJournal = walletApi.getCharactersCharacterIdWalletJournal((int) verified.getCharacterId(), WalletApi.DatasourceGetCharactersCharacterIdWalletJournal.tranquility, null, 1, accessToken);
			log.info("Found {} character journal entries", characterJournal.size());
			for (var entry : characterJournal) {
				log.info("{} {} {} {} {}", entry.getId(), entry.getDate(), entry.getRefType(), entry.getFirstPartyId(), entry.getAmount());
			}

			var corporationId = characterApi.getCharactersCharacterId((int) verified.getCharacterId(), CharacterApi.DatasourceGetCharactersCharacterId.tranquility, null).getCorporationId();
			var corporation = corporationApi.getCorporationsCorporationId(corporationId, CorporationApi.DatasourceGetCorporationsCorporationId.tranquility, null);
			log.info("Using corporation '{}' [{}]", corporation.getName(), corporationId);
			var corporationJournal = walletApi.getCorporationsCorporationIdWalletsDivisionJournal((int) corporationId, 1, WalletApi.DatasourceGetCorporationsCorporationIdWalletsDivisionJournal.tranquility, null, 1, accessToken);
			log.info("Found {} corporation journal entries", corporationJournal.size());
			for (var entry : corporationJournal) {
				log.info("{} {} {} {} {}", entry.getId(), entry.getDate(), entry.getRefType(), entry.getFirstPartyId(), entry.getAmount());
			}
		});
	}

	private Maybe<String> getAccessToken() {
		return esiAuthHelper.getTokenStringForOwnerHash(eveRefOwnerHash).toMaybe();
	}
}
