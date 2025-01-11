package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.autonomouslogic.everef.pug.TimeUtil;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@ExtendWith(MockitoExtension.class)
@Log4j2
@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + DataIndexTest.BUCKET_NAME + "/")
public class DataIndexTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	DataIndex dataIndex;

	@Inject
	S3Adapter s3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient s3Data;

	MockS3Adapter mockS3;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
		mockS3 = (MockS3Adapter) s3Adapter;

		// Rig listing.
		var lastModified = Instant.parse("2000-01-01T00:00:00.100Z");
		var files = List.of(
				"index.html",
				"data.zip",
				"dir/index.html",
				"dir/more-data.zip",
				"dir/sub/sub-data.zip",
				"dir2/more-data2.zip");
		for (String file : files) {
			var content = file.endsWith("/") ? "" : "content " + file;
			mockS3.putTestObject(BUCKET_NAME, file, content, s3Data, lastModified);
			lastModified = lastModified.plusSeconds(1);
		}

		when(s3Data.headObject(any(HeadObjectRequest.class))).thenAnswer(invocation -> {
			var req = invocation.getArgument(0, HeadObjectRequest.class);
			var meta = new HashMap<String, String>();
			if (req.key().equals("data.zip")) {
				meta.put("src_last_modified_millis", "931789583000");
			}
			return CompletableFuture.completedFuture(
					HeadObjectResponse.builder().metadata(meta).build());
		});
	}

	@Test
	@SneakyThrows
	void shouldGenerateRecursiveIndexPagesFromRoot() {
		dataIndex.run().blockingAwait();

		verifyMainIndex();
		verifyDir1Index();
		verifyDir1SubIndex();
		verifyDir2Index();

		// Assert correct uploaded files.
		assertEquals(List.of("dir/index.html", "dir/sub/index.html", "dir2/index.html", "index.html"), getAllPutKeys());
	}

	@Test
	@SneakyThrows
	void shouldGenerateNonRecursiveIndexPageAtRoot() {
		dataIndex.setRecursive(false).run().blockingAwait();

		verifyMainIndex();

		// Assert correct uploaded files.
		assertEquals(List.of("index.html"), getAllPutKeys());
	}

	@Test
	@SneakyThrows
	void shouldGenerateRecursiveIndexPagesFromPrefix() {
		dataIndex.setPrefix("dir/").run().blockingAwait();

		verifyDir1Index();
		verifyDir1SubIndex();

		// Assert correct uploaded files.
		assertEquals(List.of("dir/index.html", "dir/sub/index.html"), getAllPutKeys());
	}

	@Test
	@SneakyThrows
	void shouldGenerateNonRecursiveIndexPageAtPrefix() {
		dataIndex.setPrefix("dir/").setRecursive(false).run().blockingAwait();

		verifyDir1Index();

		// Assert correct uploaded files.
		assertEquals(List.of("dir/index.html"), getAllPutKeys());
	}

	@NotNull
	private List<String> getAllPutKeys() {
		return mockS3.getAllPutKeys(BUCKET_NAME, s3Data).stream().sorted().toList();
	}

	private void verifyMainIndex() {
		var html = getPageContent("index.html");
		assertEquals(List.of(Pair.of("/dir/", "dir"), Pair.of("/dir2/", "dir2")), getDirLinks(html));
		assertEquals(
				// Timestamp overridden by header in metadata.
				List.of(new FileLink("/data.zip", "data.zip", "16 bytes", "16", "1999-07-12T14:26:23Z")),
				getFileLinks(html));
		verifySizeOverview(html, "2", "1", "16 bytes");
	}

	private void verifyDir1Index() {
		var html = getPageContent("dir/index.html");
		assertEquals(List.of(Pair.of("/dir/sub/", "sub")), getDirLinks(html));
		assertEquals(
				List.of(new FileLink("/dir/more-data.zip", "more-data.zip", "25 bytes", "25", "2000-01-01T00:00:03Z")),
				getFileLinks(html));
		verifySizeOverview(html, "1", "1", "25 bytes");
	}

	private void verifyDir1SubIndex() {
		var html = getPageContent("dir/sub/index.html");
		assertEquals(List.of(), getDirLinks(html));
		assertEquals(
				List.of(new FileLink(
						"/dir/sub/sub-data.zip", "sub-data.zip", "28 bytes", "28", "2000-01-01T00:00:04Z")),
				getFileLinks(html));
		verifySizeOverview(html, "0", "1", "28 bytes");
	}

	private void verifyDir2Index() {
		var html = getPageContent("dir2/index.html");
		assertEquals(List.of(), getDirLinks(html));
		assertEquals(
				List.of(new FileLink(
						"/dir2/more-data2.zip", "more-data2.zip", "27 bytes", "27", "2000-01-01T00:00:05Z")),
				getFileLinks(html));
		verifySizeOverview(html, "0", "1", "27 bytes");
	}

	private String getPageContent(String path) {
		var mockS3 = (MockS3Adapter) s3Adapter;
		return mockS3.getTestObject(BUCKET_NAME, path, s3Data).map(String::new).orElseThrow();
	}

	private List<Pair<String, String>> getDirLinks(String html) {
		var mainLinks = Jsoup.parse(html).select("tr.data-dir a.url");
		return mainLinks.stream().map(e -> Pair.of(e.attr("href"), e.text())).toList();
	}

	private List<FileLink> getFileLinks(String html) {
		var rows = Jsoup.parse(html).select("tr.data-file");
		return rows.stream()
				.map(e -> {
					var timeTag = e.select("td.data-file-last-modified time");
					var attrTime = Instant.parse(timeTag.attr("datetime"));
					var textTime = ZonedDateTime.parse(timeTag.text(), TimeUtil.ISO_LIKE);
					assertEquals(attrTime, textTime.toInstant());
					assertTrue(timeTag.text().endsWith(" UTC"), timeTag.text());
					return new FileLink(
							e.select("a.data-file-url").attr("href"),
							e.select("a.data-file-url").text(),
							e.select("td.data-file-size-formatted").text(),
							e.select("td.data-file-size-bytes").text(),
							attrTime.toString());
				})
				.toList();
	}

	private void verifySizeOverview(
			String html, String expectedDirectories, String expectedFiles, String expectedSize) {
		var dom = Jsoup.parse(html);
		var directories = dom.select(".data-directory-directories");
		var files = dom.select(".data-directory-files");
		var size = dom.select(".data-directory-size");
		assertEquals(1, directories.size());
		assertEquals(1, files.size());
		assertEquals(1, size.size());
		assertEquals(expectedDirectories, directories.text());
		assertEquals(expectedFiles, files.text());
		assertEquals(expectedSize, size.text());
	}

	@Value
	private static class FileLink {
		String url;
		String name;
		String sizeFormatted;
		String sizeBytes;
		String lastModified;
	}
}
