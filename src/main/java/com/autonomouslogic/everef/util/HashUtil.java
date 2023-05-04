package com.autonomouslogic.everef.util;

import com.google.common.hash.Hashing;
import org.apache.commons.codec.binary.Base64;

public class HashUtil {
	public static final String md5b64(byte[] in) {
		return Base64.encodeBase64String(Hashing.md5().hashBytes(in).asBytes());
	}
}
