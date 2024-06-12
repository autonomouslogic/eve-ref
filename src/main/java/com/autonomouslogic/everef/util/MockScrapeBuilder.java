package com.autonomouslogic.everef.util;

import com.autonomouslogic.commons.ResourceUtil;
import com.autonomouslogic.everef.refdata.RefDataMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Singleton
public class MockScrapeBuilder {
	private static final String REFDATA_RESOURCES = "src/test/resources/refdata/";

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected RefDataUtil refDataUtil;

	@Inject
	protected DataUtil dataUtil;

	@Setter
	private String basePath;

	@Inject
	protected MockScrapeBuilder() {}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	public File createTestSde() {
		var prefix = new File(REFDATA_RESOURCES, "sde");
		var entries = new ArrayList<Map.Entry<String, byte[]>>();
		FileUtils.listFiles(prefix, null, true).stream()
				.map(f -> new File(StringUtils.remove(f.getPath(), REFDATA_RESOURCES + "sde/")))
				.forEach(f -> entries.add(createEntry("/refdata/sde", f.getPath())));
		return createZipFile(Map.ofEntries(entries.toArray(new Map.Entry[0])));
	}

	@SneakyThrows
	public File createTestEsiDump() {
		var entries = new ArrayList<Map.Entry<String, byte[]>>();
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			if (config.getEsi() == null) {
				continue;
			}
			var file = config.getEsi().getFile();
			var languages = config.getTest().getLanguages();
			if (languages == null) {
				entries.add(createEntry("/refdata/esi", String.format("data/tranquility/%s.yaml", file)));
			} else {
				for (var language : languages) {
					entries.add(
							createEntry("/refdata/esi", String.format("data/tranquility/%s.%s.yaml", file, language)));
				}
			}
		}
		return createTarXzFile(Map.ofEntries(entries.toArray(new Map.Entry[0])));
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	public File createTestHoboleaksSde() {
		var entries = new ArrayList<Map.Entry<String, byte[]>>();
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			if (config.getHoboleaks() == null) {
				continue;
			}
			entries.add(createEntry("/refdata/hoboleaks/", config.getHoboleaks().getFile()));
		}
		return createTarXzFile(Map.ofEntries(entries.toArray(new Map.Entry[0])));
	}

	@SuppressWarnings("unchecked")
	public File createTestRefdata() {
		return createTestRefdata(RefDataMeta.builder()
				.buildTime(Instant.parse("2000-01-02T03:04:05Z"))
				.build());
	}

	@SneakyThrows
	public File createTestRefdata(RefDataMeta meta) {
		var entries = new ArrayList<Map.Entry<String, byte[]>>();
		entries.add(Map.entry("meta.json", objectMapper.writeValueAsBytes(meta)));
		for (var config : refDataUtil.loadReferenceDataConfig()) {
			var testConfig = config.getTest();
			var json = objectMapper.createObjectNode();
			for (var id : testConfig.getIds()) {
				var resourceFile = String.format("/refdata/refdata/%s-%s.json", testConfig.getFilePrefix(), id);
				json.set(id.toString(), dataUtil.loadJsonResource(resourceFile));
			}
			entries.add(Map.entry(config.getOutputFile() + ".json", objectMapper.writeValueAsBytes(json)));
		}
		return createTarXzFile(Map.ofEntries(entries.toArray(new Map.Entry[0])));
	}

	@SneakyThrows
	private Map.Entry<String, byte[]> createEntry(String base, String path) {
		InputStream in;
		if (basePath == null) {
			in = ResourceUtil.loadResource(base + "/" + path);
		} else {
			var file = new File(basePath, base + "/" + path);
			in = new FileInputStream(file);
		}
		try (in) {
			return Map.entry(path, IOUtils.toByteArray(in));
		}
	}

	@SneakyThrows
	public File createTarXzFile(Map<String, byte[]> entries) {
		var file = File.createTempFile(MockScrapeBuilder.class.getSimpleName() + "-", ".tar.xz");
		try (var out = new TarArchiveOutputStream(new XZCompressorOutputStream(new FileOutputStream(file)))) {
			for (var entry : entries.entrySet()) {
				var bytes = entry.getValue();
				var archiveEntry = new TarArchiveEntry(entry.getKey());
				archiveEntry.setSize(bytes.length);
				out.putArchiveEntry(archiveEntry);
				out.write(bytes);
				out.closeArchiveEntry();
			}
		}
		return file;
	}

	@SneakyThrows
	public File createZipFile(Map<String, byte[]> entries) {
		var file = File.createTempFile(MockScrapeBuilder.class.getSimpleName() + "-", ".zip");
		try (var out = new ZipOutputStream(new FileOutputStream(file))) {
			for (var entry : entries.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				out.write(entry.getValue());
				out.closeEntry();
			}
		}
		return file;
	}
}
