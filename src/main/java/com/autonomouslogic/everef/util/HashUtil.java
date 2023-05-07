package com.autonomouslogic.everef.util;

import com.google.common.hash.Hashing;

public class HashUtil {
	public static final byte[] md5(byte[] in) {
		return Hashing.md5().hashBytes(in).asBytes();
	}
}
