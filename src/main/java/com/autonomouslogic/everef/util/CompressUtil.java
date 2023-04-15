package com.autonomouslogic.everef.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.BiConsumer;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
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
	public static ArchiveInputStream uncompressArchive(File file) {
		var in = new BufferedInputStream(new FileInputStream(file));
		var name = file.getName();
		if (name.endsWith(".zip")) {
			log.trace("Opening zip file: {}", file.getPath());
			return new ZipArchiveInputStream(in);
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
				stream -> stream.close()
			)
			.compose(Rx.offloadFlowable());
	}
}
