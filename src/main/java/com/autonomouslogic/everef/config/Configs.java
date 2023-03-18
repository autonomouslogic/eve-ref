package com.autonomouslogic.everef.config;

import java.time.Duration;
import software.amazon.awssdk.regions.Region;

public class Configs {
	/**
	 * An external URL to ping once one a command has finished running.
	 * If left empty, no ping will be sent.
	 * While this was originally designed to work with <a href="https://healthchecks.io/">healthchecks.io</a>,
	 * any URL accepting a POST request will work.
	 *
	 * @see <a href="https://healthchecks.io/docs/http_api/">Pinging API</a>
	 */
	public static final Config<String> HEALTH_CHECK_URL =
			Config.<String>builder().name("HEALTH_CHECK_URL").type(String.class).build();

	/**
	 * The URL which will be pinged at the start of a command.
	 * If left empty, no ping will be sent.
	 *
	 * @link Configs#HEALTH_CHECK_URL
	 * @see <a href="https://healthchecks.io/docs/http_api/">Pinging API</a>
	 * @see <a href="https://healthchecks.io/docs/measuring_script_run_time/">Measuring Script Run Time</a>
	 */
	public static final Config<String> HEALTH_CHECK_START_URL = Config.<String>builder()
			.name("HEALTH_CHECK_START_URL")
			.type(String.class)
			.build();

	/**
	 * The URL which will be pinged in the event of a command failure.
	 * If left empty, no ping will be sent.
	 *
	 * @link Configs#HEALTH_CHECK_URL
	 * @see <a href="https://healthchecks.io/docs/http_api/">Pinging API</a>
	 * @see <a href="https://healthchecks.io/docs/signaling_failures/">Signaling failures</a>
	 */
	public static final Config<String> HEALTH_CHECK_FAIL_URL = Config.<String>builder()
			.name("HEALTH_CHECK_FAIL_URL")
			.type(String.class)
			.build();

	/**
	 * The URL which will be pinged in the event of a command failure.
	 * If left empty, no logging will be sent.
	 *
	 * @link Configs#HEALTH_CHECK_URL
	 * @see <a href="https://healthchecks.io/docs/http_api/">Pinging API</a>
	 * @see <a href="https://healthchecks.io/docs/signaling_failures/">Signaling failures</a>
	 */
	public static final Config<String> HEALTH_CHECK_LOG_URL = Config.<String>builder()
			.name("HEALTH_CHECK_LOG_URL")
			.type(String.class)
			.build();

	/**
	 * The webhook URL for Slack reporting.
	 * If no URL is supplied, reporting will not occur.
	 */
	public static final Config<String> SLACK_WEBHOOK_URL = Config.<String>builder()
			.name("SLACK_WEBHOOK_URL")
			.type(String.class)
			.build();

	/**
	 * The channel for Slack reporting.
	 * Required if {@link Configs#SLACK_WEBHOOK_URL} is supplied.
	 */
	public static final Config<String> SLACK_WEBHOOK_CHANNEL = Config.<String>builder()
			.name("SLACK_WEBHOOK_CHANNEL")
			.type(String.class)
			.build();

	/**
	 * The username for Slack reporting.
	 * Required if {@link Configs#SLACK_WEBHOOK_URL} is supplied.
	 */
	public static final Config<String> SLACK_WEBHOOK_USERNAME = Config.<String>builder()
			.name("SLACK_WEBHOOK_USERNAME")
			.defaultValue("EVE Ref")
			.type(String.class)
			.build();

	/**
	 * The base URL used for accessing the ESI.
	 */
	public static final Config<String> ESI_BASE_URL = Config.<String>builder()
			.name("ESI_DOMAIN")
			.defaultValue("https://esi.evetech.net")
			.type(String.class)
			.build();

	/**
	 * The base path used when accessing the ESI.
	 * This will be the version.
	 */
	public static final Config<String> ESI_BASE_PATH = Config.<String>builder()
			.name("ESI_BASE_PATH")
			.defaultValue("/latest")
			.type(String.class)
			.build();

	/**
	 * The datasource to request from the ESI.
	 */
	public static final Config<String> ESI_DATASOURCE = Config.<String>builder()
			.name("ESI_DATASOURCE")
			.defaultValue("tranquility")
			.type(String.class)
			.build();

	/**
	 * Log4j2 log level to use.
	 * Can be any of: TRACE, DEBUG, INFO, WARN, ERROR, FATAL, or OFF.
	 * @see <a href="https://logging.apache.org/log4j/2.x/manual/architecture.html">Apache Logging Architecture</a>
	 */
	// Note that this isn't set anywhere in code, but is loaded in via log4j2.properties.
	public static final Config<String> LOG_LEVEL = Config.<String>builder()
			.name("LOG_LEVEL")
			.defaultValue("info")
			.type(String.class)
			.build();

	/**
	 * The domain for the data site.
	 */
	public static final Config<String> DATA_DOMAIN = Config.<String>builder()
			.name("DATA_DOMAIN")
			.defaultValue("data.everef.net")
			.type(String.class)
			.build();

	/**
	 * The S3 bucket name for the data site.
	 */
	public static final Config<String> DATA_S3_BUCKET =
			Config.<String>builder().name("DATA_S3_BUCKET").type(String.class).build();

	/**
	 * The cache time to use for data index pages.
	 */
	public static final Config<Duration> DATA_INDEX_CACHE_TIME = Config.<Duration>builder()
			.name("DATA_INDEX_CACHE_TIME")
			.defaultValue(Duration.ofMinutes(1))
			.type(Duration.class)
			.build();

	/**
	 * The AWS region for the data site.
	 */
	public static final Config<String> DATA_S3_REGION = Config.<String>builder()
			.name("DATA_S3_REGION")
			.defaultValue(Region.US_EAST_1.id())
			.type(String.class)
			.build();

	/**
	 * Endpoint override for the data site S3 client.
	 */
	public static final Config<String> DATA_S3_ENDPOINT =
			Config.<String>builder().name("DATA_S3_ENDPOINT").type(String.class).build();
}
