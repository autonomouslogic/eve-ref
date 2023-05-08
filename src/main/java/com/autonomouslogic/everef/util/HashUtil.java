package com.autonomouslogic.everef.util;

import com.google.common.hash.Hashing;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HashUtil {
	public static final byte[] md5(byte[] in) {
		return Hashing.md5().hashBytes(in).asBytes();
	}
}
