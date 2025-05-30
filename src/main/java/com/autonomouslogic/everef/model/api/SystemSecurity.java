package com.autonomouslogic.everef.model.api;

public enum SystemSecurity {
	HIGH_SEC,
	LOW_SEC,
	NULL_SEC;

	public static SystemSecurity forStatus(double status) {
		if (status >= 0.45) {
			return HIGH_SEC;
		} else if (status > 0.0) {
			return LOW_SEC;
		} else {
			return NULL_SEC;
		}
	}
}
