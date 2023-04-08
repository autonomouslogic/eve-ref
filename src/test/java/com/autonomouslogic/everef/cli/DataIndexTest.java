package com.autonomouslogic.everef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.test.DaggerTestComponent;
import com.autonomouslogic.everef.test.MockS3Adapter;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@ExtendWith(MockitoExtension.class)
@Log4j2
class DataIndexTest {
	static final String BUCKET_NAME = "data-bucket";

	@Inject
	DataIndex dataIndex;

	@Inject
	S3Adapter s3Adapter;

	@Inject
	@Named("data")
	S3AsyncClient s3Data;

	@BeforeEach
	@SneakyThrows
	void before() {
		DaggerTestComponent.builder().build().inject(this);
	}

	@SetEnvironmentVariable(key = "DATA_PATH", value = "s3://" + BUCKET_NAME + "/")
	@Test
	@SneakyThrows
	void shouldGenerateIndexPages() {
		var mockS3 = (MockS3Adapter) s3Adapter;
		// Rig listing.
		var files = List.of(
				// "index.html",
				"data.zip",
				// "dir/", // @todo
				// "dir/index.html",
				"dir/more-data.zip",
				// "dir2/", // @todo
				"dir2/more-data2.zip");
		for (String file : files) {
			mockS3.putTestObject(BUCKET_NAME, file, "test", s3Data);
		}
		// Run.
		dataIndex.run().blockingAwait();
		// Parse main index page.
		var mainPage = mockS3.getTestObject(BUCKET_NAME, "index.html", s3Data)
				.map(String::new)
				.orElseThrow();
		var mainLinks = Jsoup.parse(mainPage).select("a.url");
		assertEquals("/dir/", mainLinks.get(0).attr("href"));
		assertEquals("/dir2/", mainLinks.get(1).attr("href"));
		// Parse dir index page.
		var dirPage = mockS3.getTestObject(BUCKET_NAME, "dir/index.html", s3Data)
				.map(String::new)
				.orElseThrow();
		var dirLinks = Jsoup.parse(dirPage).select("a.data-file-url");
		assertEquals("/dir/more-data.zip", dirLinks.get(0).attr("href"));
		// Assert correct files.
		assertEquals(
				List.of("dir/index.html", "dir2/index.html", "index.html"),
				mockS3.getAllPutKeys(BUCKET_NAME, s3Data).stream().sorted().toList());
	}
}
