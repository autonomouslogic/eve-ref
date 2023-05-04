package com.autonomouslogic.everef.util;

import com.google.common.hash.Hashing;
import org.apache.commons.codec.binary.Hex;

public class HashUtil {
	public static final String md5Hex(byte[] in) {
		return Hex.encodeHexString(Hashing.md5().hashBytes(in).asBytes());
	}
}
