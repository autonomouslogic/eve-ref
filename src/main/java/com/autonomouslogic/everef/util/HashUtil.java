package com.autonomouslogic.everef.util;

import com.google.common.hash.Hashing;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashUtil {
	public static byte[] md5(byte[] in) {
		return Hashing.md5().hashBytes(in).asBytes();
	}

	public static byte[] md5(String in) {
		return md5(in.getBytes(StandardCharsets.UTF_8));
	}

	@SneakyThrows
	public static byte[] sha256(File file) {
		return Hashing.sha256()
				.hashBytes(IOUtils.toByteArray(new FileInputStream(file)))
				.asBytes();
	}

	public static String sha256Hex(File file) {
		return Hex.encodeHexString(sha256(file));
	}
}
