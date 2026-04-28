package com.autonomouslogic.everef.cli.wars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import lombok.extern.log4j.Log4j2;
import org.h2.mvstore.MVStore;

/**
 * One-off migration script to migrate wars data from MVStore to wars-current.json format.
 *
 * Usage:
 * java com.autonomouslogic.everef.cli.wars.MigrateWarsFromMvstore <mvstore_file_path> [output_path]
 *
 * Example:
 * java com.autonomouslogic.everef.cli.wars.MigrateWarsFromMvstore wars.mvstore wars-current.json
 */
@Log4j2
public class MigrateWarsFromMvstore {

	private ObjectMapper objectMapper = new ObjectMapper();

	public void migrate(String mvstoreFilePath, String outputPath) {
		log.info("Starting migration from MVStore to wars-current.json");
		log.info("Input file: {}", mvstoreFilePath);
		log.info("Output file: {}", outputPath);

		var mvstoreFile = new File(mvstoreFilePath);
		if (!mvstoreFile.exists()) {
			log.error("MVStore file not found: {}", mvstoreFilePath);
			throw new RuntimeException("MVStore file not found: " + mvstoreFilePath);
		}

		try {
			// Open MVStore
			var mvStore = new MVStore.Builder()
					.fileName(mvstoreFile.getAbsolutePath())
					.open();

			var warsMap = mvStore.openMap("wars");
			var killmailsMap = mvStore.openMap("kill_mails");

			log.info("Opened MVStore with {} wars and {} killmails", warsMap.size(), killmailsMap.size());

			// Build wars-current.json with last_killmail_id per war
			var root = objectMapper.createObjectNode();

			// For each unfinished war, find max killmail ID
			warsMap.entrySet().forEach(entry -> {
				Long warId = (Long) entry.getKey();
				JsonNode war = (JsonNode) entry.getValue();

				// Only include unfinished wars
				if (!war.has("finished") || war.get("finished").isNull()) {
					// Find max killmail ID for this war
					long maxKillmailId = killmailsMap.entrySet().stream()
							.map(kmEntry -> {
								JsonNode km = (JsonNode) kmEntry.getValue();
								if (km.has("war_id") && km.get("war_id").asLong() == warId) {
									return (Long) kmEntry.getKey();
								}
								return 0L;
							})
							.mapToLong(Long::longValue)
							.max()
							.orElse(0L);

					// Add last_killmail_id to war
					var warCopy = (ObjectNode) war.deepCopy();
					warCopy.put("last_killmail_id", maxKillmailId);
					root.set(warId.toString(), warCopy);

					log.debug("War {}: found {} killmails (max ID: {})", warId, killmailsMap.size(), maxKillmailId);
				}
			});

			// Write to file
			var outputFile = new File(outputPath);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, root);

			log.info("Migration complete. Created wars-current.json with {} wars", root.size());
			log.info("File saved to: {}", outputFile.getAbsolutePath());
			log.info("Next steps:");
			log.info("1. Upload this file to S3 at: wars/wars-current.json");
			log.info("2. Deploy the new code without MVStore dependencies");
			log.info("3. Delete the old MVStore file from S3");

			mvStore.close();
		} catch (Exception e) {
			log.error("Migration failed", e);
			throw new RuntimeException("Migration failed", e);
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java MigrateWarsFromMvstore <mvstore_file_path> [output_path]");
			System.err.println("Example: java MigrateWarsFromMvstore wars.mvstore wars-current.json");
			System.exit(1);
		}

		var mvstoreFile = args[0];
		var outputFile = args.length > 1 ? args[1] : "wars-current.json";

		var migrator = new MigrateWarsFromMvstore();
		migrator.migrate(mvstoreFile, outputFile);
	}
}
