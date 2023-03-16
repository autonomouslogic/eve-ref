package com.autonomouslogic.everef.config;

public class Configs {
	public static final Config<String> HEALTH_CHECK_URL =
			Config.<String>builder().name("HEALTH_CHECK_URL").type(String.class).build();
}
