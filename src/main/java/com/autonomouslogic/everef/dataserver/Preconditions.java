package com.autonomouslogic.everef.dataserver;

import java.time.Instant;
import java.util.List;

/**
 * Evaluates RFC 9110 §13 preconditions, in §13.2.2 order. Called only when the file exists and the
 * unconditional response would be 2xx (the caller handles the §13.2.1 "resource doesn't exist" case before
 * reaching here, since a missing file must stay 404 even with {@code If-Match: *}).
 *
 * <p>{@code Range} / {@code If-Range} are never passed in: they're ignored entirely (data-server-plan.md
 * step 4), so §13.2.2 step 5 never applies.
 */
public class Preconditions {
	private Preconditions() {}

	public static PreconditionResult evaluate(
			String method,
			List<String> ifMatch,
			List<String> ifUnmodifiedSince,
			List<String> ifNoneMatch,
			List<String> ifModifiedSince,
			String currentETag,
			Instant lastModified) {
		var current = EntityTags.parseSingle(currentETag);

		if (!ifMatch.isEmpty()) {
			if (!matches(EntityTags.parse(ifMatch), current, true)) {
				return PreconditionResult.PRECONDITION_FAILED;
			}
		} else if (!ifUnmodifiedSince.isEmpty()) {
			var date = HttpDates.parseSingleValuedHeader(ifUnmodifiedSince);
			if (date.isPresent() && lastModified.isAfter(date.get())) {
				return PreconditionResult.PRECONDITION_FAILED;
			}
		}

		var isGetOrHead = method.equals("GET") || method.equals("HEAD");
		if (!ifNoneMatch.isEmpty()) {
			if (matches(EntityTags.parse(ifNoneMatch), current, false)) {
				return PreconditionResult.NOT_MODIFIED;
			}
		} else if (!ifModifiedSince.isEmpty() && isGetOrHead) {
			var date = HttpDates.parseSingleValuedHeader(ifModifiedSince);
			if (date.isPresent() && !lastModified.isAfter(date.get())) {
				return PreconditionResult.NOT_MODIFIED;
			}
		}

		return PreconditionResult.PROCEED;
	}

	private static boolean matches(List<EntityTag> tags, java.util.Optional<EntityTag> current, boolean strong) {
		if (tags.stream().anyMatch(t -> t.wildcard())) {
			return true;
		}
		if (current.isEmpty()) {
			return false;
		}
		return tags.stream().anyMatch(t -> strong ? t.strongMatches(current.get()) : t.weakMatches(current.get()));
	}
}
