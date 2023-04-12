package com.autonomouslogic.everef.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;

@Log4j2
public class CompressUtil {
	@SneakyThrows
	public static File compressBzip2(File file) {
		var compressed = new File(file.getPath() + ".bz2");
		int blockSize = Math.min(BZip2CompressorOutputStream.chooseBlockSize(file.length()), 7);
		log.info("Compressing {} to {} - blockSize: {}", file.getPath(), compressed.getPath(), blockSize);
		try (var out = new BZip2CompressorOutputStream(new FileOutputStream(compressed), blockSize)) {
			IOUtils.copy(new FileInputStream(file), out);
		}
		return compressed;
	}
}
