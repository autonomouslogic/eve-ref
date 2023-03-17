package com.autonomouslogic.everef.config;

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
}
