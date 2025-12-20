package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.esi.EsiHelper;
import com.autonomouslogic.everef.esi.EsiUrl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;

/**
 * Scrapes freelance jobs from the ESI API.
 */
@Log4j2
public class ScrapeFreelanceJobs implements Command {
	@Inject
	protected EsiHelper esiHelper;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected ScrapeFreelanceJobs() {}

	@Override
	@SneakyThrows
	public void run() {
		log.info("Starting freelance jobs scrape");

		var jobs = fetchIndex();

		var detailedJobs = new HashMap<String, JsonNode>();
		for (var job : jobs) {
			fetchJobDetail(job, detailedJobs);
		}

		log.info("Retrieved {} jobs", detailedJobs.size());

		System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detailedJobs));
	}

	private JsonNode fetchIndex() {
		var indexUrl = EsiUrl.modern().urlPath("/freelance-jobs").build();
		var indexResponse = esiHelper.fetch(indexUrl);
		var indexData = esiHelper.decodeResponse(indexResponse);

		var jobsArray = indexData.get("freelance_jobs");
		if (jobsArray == null || !jobsArray.isArray()) {
			throw new IllegalStateException("No freelance_jobs array found in response");
		}

		log.info("Retrieved {} jobs from index", jobsArray.size());
		return jobsArray;
	}

	private void fetchJobDetail(JsonNode job, Map<String, JsonNode> detailedJobs) {
		var jobId = job.get("id");
		if (jobId == null || jobId.isNull()) {
			log.warn("Job entry missing ID, skipping");
			return;
		}

		var jobIdString = jobId.asText();
		var detailUrl =
			EsiUrl.builder().urlPath("/freelance-jobs/" + jobIdString).build();
		var detailResponse = esiHelper.fetch(detailUrl);
		var detailData = esiHelper.decodeResponse(detailResponse);

		detailedJobs.put(jobIdString, detailData);
	}
}
