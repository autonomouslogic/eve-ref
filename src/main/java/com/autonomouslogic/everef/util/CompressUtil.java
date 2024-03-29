package com.autonomouslogic.everef.util;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.BiConsumer;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
public class CompressUtil {
	@SneakyThrows
	public static File compressBzip2(File file) {
		var compressed = new File(file.getPath() + ".bz2");
		int blockSize = Math.min(BZip2CompressorOutputStream.chooseBlockSize(file.length()), 7);
		log.trace("Compressing {} to {} - blockSize: {}", file.getPath(), compressed.getPath(), blockSize);
		try (var out = new BZip2CompressorOutputStream(new FileOutputStream(compressed), blockSize)) {
			IOUtils.copy(new FileInputStream(file), out);
		}
		return compressed;
	}

	@SneakyThrows
	public static File compressXz(File file) {
		var compressed = new File(file.getPath() + ".xz");
		log.trace("Compressing {} to {}", file.getPath(), compressed.getPath());
		try (var out = new XZCompressorOutputStream(new FileOutputStream(compressed))) {
			IOUtils.copy(new FileInputStream(file), out);
		}
		return compressed;
	}

	@SneakyThrows
	public static ArchiveInputStream uncompressArchive(File file) {
		var in = new BufferedInputStream(new FileInputStream(file));
		var name = file.getName();
		if (name.endsWith(".zip")) {
			log.trace("Opening zip file: {}", file.getPath());
			return new ZipArchiveInputStream(in);
		}
		if (name.endsWith(".tar.xz")) {
			log.trace("Opening tar-xz file: {}", file.getPath());
			return new TarArchiveInputStream(new XZCompressorInputStream(in));
		}
		if (name.endsWith(".tar.bz2")) {
			log.trace("Opening tar-bz2 file: {}", file.getPath());
			return new TarArchiveInputStream(new BZip2CompressorInputStream(in));
		}
		throw new IllegalArgumentException("Unknown file type: " + name);
	}

	public static Flowable<Pair<ArchiveEntry, byte[]>> loadArchive(File file) {
		return Flowable.generate(
						() -> CompressUtil.uncompressArchive(file),
						(BiConsumer<ArchiveInputStream, Emitter<Pair<ArchiveEntry, byte[]>>>) (stream, emitter) -> {
							var entry = stream.getNextEntry();
							if (entry == null) {
								log.trace("Finished reading: {}", file.getPath());
								emitter.onComplete();
							} else {
								log.trace("Reading entry {}#{}", file.getPath(), entry.getName());
								var bytes = IOUtils.toByteArray(stream);
								emitter.onNext(Pair.of(entry, bytes));
							}
						},
						stream -> stream.close())
				.compose(Rx.offloadFlowable());
	}

	@SneakyThrows
	public static InputStream uncompress(File file) {
		var in = new BufferedInputStream(new FileInputStream(file));
		var name = file.getName();
		if (name.endsWith(".gz")) {
			log.trace("Opening gzip file: {}", file.getPath());
			return new GZIPInputStream(in);
		}
		if (name.endsWith(".bz2")) {
			log.trace("Opening bz2 file: {}", file.getPath());
			return new BZip2CompressorInputStream(in);
		}
		throw new IllegalArgumentException("Unknown file type: " + name);
	}

	@SneakyThrows
	public static int lineCount(File file) {
		try (var reader = new BufferedReader(new InputStreamReader(uncompress(file), StandardCharsets.UTF_8))) {
			int lines = 0;
			while ((reader.readLine()) != null) {
				lines++;
			}
			return lines;
		}
	}
}
