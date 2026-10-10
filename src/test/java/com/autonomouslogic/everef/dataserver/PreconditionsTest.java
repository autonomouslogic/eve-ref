package com.autonomouslogic.everef.dataserver;

import static com.autonomouslogic.everef.dataserver.PreconditionResult.NOT_MODIFIED;
import static com.autonomouslogic.everef.dataserver.PreconditionResult.PRECONDITION_FAILED;
import static com.autonomouslogic.everef.dataserver.PreconditionResult.PROCEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreconditionsTest {
	private static final String ETAG = "\"abc123\"";
	private static final Instant LAST_MODIFIED = Instant.parse("2026-01-01T00:00:00Z");
	private static final List<String> NONE = List.of();

	private static PreconditionResult evaluate(
			String method, List<String> ifMatch, List<String> ius, List<String> inm, List<String> ims) {
		return Preconditions.evaluate(method, ifMatch, ius, inm, ims, ETAG, LAST_MODIFIED);
	}

	// --- If-Match ---

	@Test
	void ifMatchStrongTagMatches() {
		assertEquals(PROCEED, evaluate("GET", List.of(ETAG), NONE, NONE, NONE));
	}

	@Test
	void ifMatchWildcardMatches() {
		assertEquals(PROCEED, evaluate("GET", List.of("*"), NONE, NONE, NONE));
	}

	@Test
	void ifMatchListWithOneMatchingMember() {
		assertEquals(PROCEED, evaluate("GET", List.of("\"other\", " + ETAG), NONE, NONE, NONE));
	}

	@Test
	void ifMatchMismatchFails() {
		assertEquals(PRECONDITION_FAILED, evaluate("GET", List.of("\"nope\""), NONE, NONE, NONE));
	}

	@Test
	void ifMatchWeakTagEqualToEtagFails() {
		// Weak tags never satisfy If-Match, even with the same opaque value.
		assertEquals(PRECONDITION_FAILED, evaluate("GET", List.of("W/" + ETAG), NONE, NONE, NONE));
	}

	// --- If-Unmodified-Since ---

	@Test
	void ifUnmodifiedSinceAfterLastModifiedProceeds() {
		assertEquals(PROCEED, evaluate("GET", NONE, List.of(httpDate(LAST_MODIFIED.plusSeconds(60))), NONE, NONE));
	}

	@Test
	void ifUnmodifiedSinceBeforeLastModifiedFails() {
		assertEquals(
				PRECONDITION_FAILED,
				evaluate("GET", NONE, List.of(httpDate(LAST_MODIFIED.minusSeconds(60))), NONE, NONE));
	}

	@Test
	void ifUnmodifiedSinceInvalidDateIsIgnored() {
		assertEquals(PROCEED, evaluate("GET", NONE, List.of("garbage"), NONE, NONE));
	}

	// --- If-None-Match ---

	@Test
	void ifNoneMatchExactTagNotModified() {
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, List.of(ETAG), NONE));
	}

	@Test
	void ifNoneMatchWeakTagEqualToEtagNotModified() {
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, List.of("W/" + ETAG), NONE));
	}

	@Test
	void ifNoneMatchWildcardNotModified() {
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, List.of("*"), NONE));
	}

	@Test
	void ifNoneMatchListWithOneMatchingMemberNotModified() {
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, List.of("\"other\", " + ETAG), NONE));
	}

	@Test
	void ifNoneMatchMismatchProceeds() {
		assertEquals(PROCEED, evaluate("GET", NONE, NONE, List.of("\"nope\""), NONE));
	}

	@Test
	void ifNoneMatchRepeatedOnTwoLinesMatchOnSecond() {
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, List.of("\"nope\"", ETAG), NONE));
	}

	@Test
	void ifNoneMatchAppliesOnHead() {
		assertEquals(NOT_MODIFIED, evaluate("HEAD", NONE, NONE, List.of(ETAG), NONE));
	}

	// --- If-Modified-Since ---

	@Test
	void ifModifiedSinceAtOrAfterLastModifiedNotModified() {
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, NONE, List.of(httpDate(LAST_MODIFIED))));
		assertEquals(NOT_MODIFIED, evaluate("GET", NONE, NONE, NONE, List.of(httpDate(LAST_MODIFIED.plusSeconds(60)))));
	}

	@Test
	void ifModifiedSinceBeforeLastModifiedProceeds() {
		assertEquals(PROCEED, evaluate("GET", NONE, NONE, NONE, List.of(httpDate(LAST_MODIFIED.minusSeconds(60)))));
	}

	@Test
	void ifModifiedSinceInvalidDateIsIgnored() {
		assertEquals(PROCEED, evaluate("GET", NONE, NONE, NONE, List.of("garbage")));
	}

	@Test
	void ifModifiedSinceTwoDatesIsIgnored() {
		assertEquals(
				PROCEED,
				evaluate(
						"GET",
						NONE,
						NONE,
						NONE,
						List.of(httpDate(LAST_MODIFIED), httpDate(LAST_MODIFIED.plusSeconds(60)))));
	}

	@Test
	void ifModifiedSinceAppliesOnHead() {
		assertEquals(NOT_MODIFIED, evaluate("HEAD", NONE, NONE, NONE, List.of(httpDate(LAST_MODIFIED))));
	}

	// --- Precedence ---

	@Test
	void failingIfMatchTakesPrecedenceOverMatchingIfNoneMatch() {
		assertEquals(PRECONDITION_FAILED, evaluate("GET", List.of("\"nope\""), NONE, List.of(ETAG), NONE));
	}

	@Test
	void failingIfUnmodifiedSinceTakesPrecedenceOverMatchingIfNoneMatch() {
		assertEquals(
				PRECONDITION_FAILED,
				evaluate("GET", NONE, List.of(httpDate(LAST_MODIFIED.minusSeconds(60))), List.of(ETAG), NONE));
	}

	@Test
	void ifUnmodifiedSinceIsIgnoredWhenIfMatchPresent() {
		// If-Match present (and passing) means If-Unmodified-Since, even if it would fail, is never evaluated.
		assertEquals(
				PROCEED, evaluate("GET", List.of(ETAG), List.of(httpDate(LAST_MODIFIED.minusSeconds(60))), NONE, NONE));
	}

	@Test
	void ifModifiedSinceIsIgnoredWhenIfNoneMatchPresent() {
		// If-None-Match present (and not matching) means If-Modified-Since, even if it would give 304, is
		// never evaluated.
		assertEquals(PROCEED, evaluate("GET", NONE, NONE, List.of("\"nope\""), List.of(httpDate(LAST_MODIFIED))));
	}

	// --- Default ---

	@Test
	void noPreconditionsProceeds() {
		assertEquals(PROCEED, evaluate("GET", NONE, NONE, NONE, NONE));
	}

	// --- Entity-tag tokenizer (via EntityTags, exercised through parse) ---

	@Test
	void tokenizerHandlesOpaqueTagContainingComma() {
		var tags = EntityTags.parse(List.of("\"a,b\""));
		assertEquals(1, tags.size());
		assertEquals("a,b", tags.get(0).tag());
	}

	@Test
	void tokenizerHandlesSpacesAroundCommas() {
		var tags = EntityTags.parse(List.of("\"a\" , \"b\""));
		assertEquals(2, tags.size());
	}

	@Test
	void tokenizerDropsEmptyMembers() {
		var tags = EntityTags.parse(List.of("\"a\", , \"b\""));
		assertEquals(2, tags.size());
	}

	@Test
	void tokenizerDropsMalformedMembers() {
		var tags = EntityTags.parse(List.of("not-quoted, W/, \"ok\""));
		assertEquals(1, tags.size());
		assertEquals("ok", tags.get(0).tag());
	}

	@Test
	void tokenizerHandlesWildcardMixedWithTags() {
		var tags = EntityTags.parse(List.of("\"a\", *"));
		assertTrue(tags.stream().anyMatch(EntityTag::wildcard));
	}

	@Test
	void tokenizerMergesRepeatedHeaderLines() {
		var tags = EntityTags.parse(List.of("\"a\"", "\"b\""));
		assertEquals(2, tags.size());
	}

	// --- Strong vs weak comparison (RFC 9110 §8.8.3.2) ---

	@Test
	void strongComparisonRequiresBothStrongAndEqualTag() {
		assertTrue(EntityTag.strong("1").strongMatches(EntityTag.strong("1")));
		assertFalse(EntityTag.weak("1").strongMatches(EntityTag.strong("1")));
		assertFalse(EntityTag.strong("1").strongMatches(EntityTag.weak("1")));
		assertFalse(EntityTag.strong("1").strongMatches(EntityTag.strong("2")));
	}

	@Test
	void weakComparisonIgnoresWeakFlag() {
		assertTrue(EntityTag.weak("1").weakMatches(EntityTag.strong("1")));
		assertTrue(EntityTag.strong("1").weakMatches(EntityTag.weak("1")));
		assertTrue(EntityTag.weak("1").weakMatches(EntityTag.weak("1")));
		assertFalse(EntityTag.weak("1").weakMatches(EntityTag.weak("2")));
	}

	private static String httpDate(Instant instant) {
		return java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(java.time.ZoneOffset.UTC));
	}
}
