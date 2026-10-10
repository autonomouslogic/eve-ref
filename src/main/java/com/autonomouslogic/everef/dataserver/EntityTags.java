package com.autonomouslogic.everef.dataserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses {@code If-Match} / {@code If-None-Match} style entity-tag lists (RFC 9110 §8.8.3, §13.1.1, §13.1.2).
 * Not a generic HTTP header parser: it only tokenizes the {@code #entity-tag} list grammar used by these two
 * headers, including the {@code *} wildcard.
 */
public class EntityTags {
	private EntityTags() {}

	/**
	 * Parses one entity-tag header's raw values. Repeated header lines are merged into a single list.
	 * Malformed members are dropped (they can never match anything), rather than failing the whole header.
	 */
	public static List<EntityTag> parse(List<String> rawValues) {
		var tags = new ArrayList<EntityTag>();
		for (var line : rawValues) {
			for (var member : splitTopLevelCommas(line)) {
				parseMember(member.trim()).ifPresent(tags::add);
			}
		}
		return tags;
	}

	/**
	 * Splits a header line on commas, except commas inside a quoted opaque-tag.
	 */
	private static List<String> splitTopLevelCommas(String line) {
		var parts = new ArrayList<String>();
		var current = new StringBuilder();
		boolean insideQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			var c = line.charAt(i);
			if (c == '"') {
				insideQuotes = !insideQuotes;
				current.append(c);
			} else if (c == ',' && !insideQuotes) {
				parts.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		parts.add(current.toString());
		return parts;
	}

	private static Optional<EntityTag> parseMember(String member) {
		if (member.isEmpty()) {
			return Optional.empty();
		}
		if (member.equals("*")) {
			return Optional.of(EntityTag.any());
		}
		boolean weak = false;
		var rest = member;
		if (rest.startsWith("W/")) {
			weak = true;
			rest = rest.substring(2);
		}
		if (rest.length() < 2 || rest.charAt(0) != '"' || rest.charAt(rest.length() - 1) != '"') {
			return Optional.empty();
		}
		var tag = rest.substring(1, rest.length() - 1);
		return Optional.of(weak ? EntityTag.weak(tag) : EntityTag.strong(tag));
	}

	/**
	 * Parses a single raw {@code ETag} header value (no comma-separated list), as used for an upstream
	 * ETag we need to compare against a client's entity-tag list.
	 */
	public static Optional<EntityTag> parseSingle(String rawValue) {
		if (rawValue == null) {
			return Optional.empty();
		}
		return parseMember(rawValue.trim());
	}
}
