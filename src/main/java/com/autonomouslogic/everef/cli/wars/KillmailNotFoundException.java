package com.autonomouslogic.everef.cli.wars;

/**
 * Thrown when a killmail is not found (404) in ESI.
 */
public class KillmailNotFoundException extends RuntimeException {
	public KillmailNotFoundException(long killmailId) {
		super("Killmail not found: " + killmailId);
	}
}
