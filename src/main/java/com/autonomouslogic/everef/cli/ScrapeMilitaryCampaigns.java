package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.archive.ArchivePathFactories.MILITARY_CAMPAIGNS;

import com.autonomouslogic.commons.concurrent.VirtualThreads;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Scrapes military campaigns data from the ESI API.
 */
@Log4j2
public class ScrapeMilitaryCampaigns implements Command {
	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected S3Util s3Util;

	@Inject
	@Named("data")
	protected S3AsyncClient s3Client;

	@Inject
	protected UrlParser urlParser;

	@Setter
	private ZonedDateTime scrapeTime;

	@Inject
	protected ScrapeMilitaryCampaigns() {}

	@Override
	@SneakyThrows
	public void run() {
		if (scrapeTime == null) {
			scrapeTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
		}
		var dataPath = (S3Url) urlParser.parse(Configs.DATA_PATH.getRequired());
		log.info("Starting military campaigns scrape");

		// Fetch campaign list
		JsonNode campaignsListNode;
		try (var resp =
				esiHelper.fetch(EsiUrl.modern().urlPath("/military-campaigns").build())) {
			campaignsListNode = esiHelper.decodeResponse(resp);
		}
		var campaigns = new LinkedHashMap<String, ObjectNode>();
		campaignsListNode
				.get("campaigns")
				.forEach(c -> campaigns.put(c.get("id").asText(), (ObjectNode) c.deepCopy()));

		// Fetch objectives per campaign in parallel
		var tasks = campaigns.entrySet().stream()
				.map(entry -> (Callable<Void>) () -> {
					var id = entry.getKey();
					try (var resp = esiHelper.fetch(EsiUrl.modern()
							.urlPath("/military-campaigns/" + id + "/objectives")
							.build())) {
						var node = esiHelper.decodeResponse(resp);
						var objectivesById = objectMapper.createObjectNode();
						node.get("objectives")
								.forEach(obj -> objectivesById.set(obj.get("id").asText(), obj));
						entry.getValue().set("objectives", objectivesById);
					}
					return null;
				})
				.toList();
		if (!tasks.isEmpty()) {
			VirtualThreads.callAll(tasks, tasks.size());
		}

		log.info("Fetched {} campaigns", campaigns.size());

		// Write plain JSON, compress, upload latest (plain JSON) and archive (bz2)
		var jsonFile = tempFiles.tempFile("military-campaigns", ".json").toFile();
		objectMapper.writeValue(jsonFile, campaigns);
		var compressedFile = CompressUtil.compressBzip2(jsonFile);
		s3Util.uploadLatestAndArchive(
				jsonFile,
				compressedFile,
				dataPath,
				MILITARY_CAMPAIGNS,
				scrapeTime,
				"application/json",
				"application/x-bzip2",
				s3Client);

		log.info("Completed military campaigns scrape");
	}
}
