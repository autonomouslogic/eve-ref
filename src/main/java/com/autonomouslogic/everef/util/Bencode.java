package com.autonomouslogic.everef.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal bencode decoder, as used by BitTorrent trackers.
 * Integers decode to {@link Long}, byte strings to <code>byte[]</code>, lists to {@link List}, and dictionaries to
 * {@link Map}. Dictionary keys are decoded as ISO-8859-1 strings, so binary keys such as info-hashes can be recovered
 * with <code>key.getBytes(StandardCharsets.ISO_8859_1)</code>.
 */
public class Bencode {
	private final byte[] data;
	private int pos = 0;

	private Bencode(byte[] data) {
		this.data = data;
	}

	public static Object decode(byte[] data) {
		var decoder = new Bencode(data);
		var result = decoder.next();
		if (decoder.pos != data.length) {
			throw new IllegalArgumentException("Trailing data at position " + decoder.pos);
		}
		return result;
	}

	private Object next() {
		var c = peek();
		if (c == 'i') {
			pos++;
			var end = indexOf('e');
			var value = Long.parseLong(new String(data, pos, end - pos, StandardCharsets.US_ASCII));
			pos = end + 1;
			return value;
		}
		if (c == 'l') {
			pos++;
			var list = new ArrayList<>();
			while (peek() != 'e') {
				list.add(next());
			}
			pos++;
			return list;
		}
		if (c == 'd') {
			pos++;
			var map = new LinkedHashMap<String, Object>();
			while (peek() != 'e') {
				var key = new String(nextBytes(), StandardCharsets.ISO_8859_1);
				map.put(key, next());
			}
			pos++;
			return map;
		}
		if (c >= '0' && c <= '9') {
			return nextBytes();
		}
		throw new IllegalArgumentException("Unexpected character '" + (char) c + "' at position " + pos);
	}

	private byte[] nextBytes() {
		var colon = indexOf(':');
		var length = Integer.parseInt(new String(data, pos, colon - pos, StandardCharsets.US_ASCII));
		var start = colon + 1;
		if (length < 0 || start + length > data.length) {
			throw new IllegalArgumentException("Invalid string length " + length + " at position " + pos);
		}
		var bytes = new byte[length];
		System.arraycopy(data, start, bytes, 0, length);
		pos = start + length;
		return bytes;
	}

	private byte peek() {
		if (pos >= data.length) {
			throw new IllegalArgumentException("Unexpected end of data");
		}
		return data[pos];
	}

	private int indexOf(char c) {
		for (int i = pos; i < data.length; i++) {
			if (data[i] == c) {
				return i;
			}
		}
		throw new IllegalArgumentException("Expected '" + c + "' after position " + pos);
	}
}
