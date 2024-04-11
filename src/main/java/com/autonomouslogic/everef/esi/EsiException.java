package com.autonomouslogic.everef.esi;

import lombok.Getter;

@Getter
public class EsiException extends RuntimeException {
	final int code;

	public EsiException(int code, String message) {
		super(message);
		this.code = code;
	}
}
