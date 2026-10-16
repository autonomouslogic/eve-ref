package com.autonomouslogic.everef.dataserver;

/**
 * A single member of an entity-tag list (RFC 9110 §8.8.3), or the wildcard {@code *}.
 */
public record EntityTag(boolean weak, String tag, boolean wildcard) {
	public static EntityTag any() {
		return new EntityTag(false, null, true);
	}

	public static EntityTag strong(String tag) {
		return new EntityTag(false, tag, false);
	}

	public static EntityTag weak(String tag) {
		return new EntityTag(true, tag, false);
	}

	/**
	 * Strong comparison (RFC 9110 §8.8.3.2): both tags must be non-weak, with identical opaque-tags.
	 */
	public boolean strongMatches(EntityTag other) {
		return !weak && !other.weak && tag.equals(other.tag);
	}

	/**
	 * Weak comparison (RFC 9110 §8.8.3.2): identical opaque-tags, ignoring the weak flag on either side.
	 */
	public boolean weakMatches(EntityTag other) {
		return tag.equals(other.tag);
	}
}
