package com.autonomouslogic.everef.cli;

import static com.autonomouslogic.everef.util.ArchivePathFactory.FREELANCE_JOBS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.autonomouslogic.everef.http.OkHttpWrapper;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
class GenericHistoryScraperTest {
	@Mock
	private OkHttpWrapper okHttpWrapper;

	@Mock
	private TempFiles tempFiles;

	@Mock
	private S3Util s3Util;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private UrlParser urlParser;

	@Mock
	private S3AsyncClient s3Client;

	@Mock
	private Response response;

	private GenericHistoryScraper scraper;
	private S3Url dataPath;

	@BeforeEach
	void setUp() {
		dataPath = new S3Url("s3://test-bucket/data/");
		when(urlParser.parse(any())).thenReturn(dataPath);

		scraper = new GenericHistoryScraper();
		scraper.okHttpWrapper = okHttpWrapper;
		scraper.tempFiles = tempFiles;
		scraper.s3Util = s3Util;
		scraper.objectMapper = objectMapper;
		scraper.urlParser = urlParser;
		scraper.s3Client = s3Client;
		scraper.init();
	}

	@Test
	void shouldThrowExceptionOnHttpFailure() throws Exception {
		var url = "https://example.com/data";
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
		var tempFile = new File("/tmp/test.json");

		when(response.code()).thenReturn(404);
		when(okHttpWrapper.get(url)).thenReturn(response);
		when(tempFiles.tempFile("scrape", ".json")).thenReturn(tempFile.toPath());

		assertThrows(RuntimeException.class, () -> scraper.fetchAndUpload(url, FREELANCE_JOBS, scrapeTime));
	}

	@Test
	void shouldThrowExceptionOnInvalidJson() throws Exception {
		var url = "https://example.com/data";
		var scrapeTime = ZonedDateTime.now(ZoneOffset.UTC);
		var tempFile = new File("/tmp/test.json");

		when(response.code()).thenReturn(200);
		when(response.body()).thenReturn(null);
		when(okHttpWrapper.get(url)).thenReturn(response);
		when(tempFiles.tempFile("scrape", ".json")).thenReturn(tempFile.toPath());
		when(objectMapper.readTree(tempFile)).thenThrow(new IllegalArgumentException("Invalid JSON"));

		assertThrows(RuntimeException.class, () -> scraper.fetchAndUpload(url, FREELANCE_JOBS, scrapeTime));
	}
}
