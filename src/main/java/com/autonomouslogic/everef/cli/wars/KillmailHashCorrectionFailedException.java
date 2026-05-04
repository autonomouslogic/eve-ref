package com.autonomouslogic.everef.cli.wars;

/**
 * Thrown when a killmail hash is invalid (422) and cannot be corrected via Zkillboard.
 */
public class KillmailHashCorrectionFailedException extends RuntimeException {
	public KillmailHashCorrectionFailedException(long killmailId, String originalHash) {
		super("Failed to correct hash for killmail " + killmailId + " (original hash: " + originalHash + ")");
	}
}
