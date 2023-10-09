package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import java.time.Instant;
import java.util.List;
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
				List.of(new FileLink("/data.zip", "data.zip", "16 bytes", "16", "2000-01-01T00:00:01Z")),
				getFileLinks(html));
	}

	private void verifyDir1Index() {
		var html = getPageContent("dir/index.html");
		assertEquals(List.of(Pair.of("/dir/sub/", "sub")), getDirLinks(html));
		assertEquals(
				List.of(new FileLink("/dir/more-data.zip", "more-data.zip", "25 bytes", "25", "2000-01-01T00:00:03Z")),
				getFileLinks(html));
	}

	private void verifyDir1SubIndex() {
		var html = getPageContent("dir/sub/index.html");
		assertEquals(List.of(), getDirLinks(html));
		assertEquals(
				List.of(new FileLink(
						"/dir/sub/sub-data.zip", "sub-data.zip", "28 bytes", "28", "2000-01-01T00:00:04Z")),
				getFileLinks(html));
	}

	private void verifyDir2Index() {
		var html = getPageContent("dir2/index.html");
		assertEquals(List.of(), getDirLinks(html));
		assertEquals(
				List.of(new FileLink(
						"/dir2/more-data2.zip", "more-data2.zip", "27 bytes", "27", "2000-01-01T00:00:05Z")),
				getFileLinks(html));
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
				.map(e -> new FileLink(
						e.select("a.data-file-url").attr("href"),
						e.select("a.data-file-url").text(),
						e.select("td.data-file-size-formatted").text(),
						e.select("td.data-file-size-bytes").text(),
						e.select("td.data-file-last-modified").text()))
				.toList();
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
