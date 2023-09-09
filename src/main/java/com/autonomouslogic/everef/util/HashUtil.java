package com.autonomouslogic.everef.util;

import com.google.common.hash.Hashing;
import java.io.File;
import java.io.FileInputStream;
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

	@SneakyThrows
	public static byte[] sha1(File file) {
		return Hashing.sha1()
				.hashBytes(IOUtils.toByteArray(new FileInputStream(file)))
				.asBytes();
	}

	public static String sha1Hex(File file) {
		return Hex.encodeHexString(sha1(file));
	}
}
