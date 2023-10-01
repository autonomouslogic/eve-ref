package com.autonomouslogic.everef.config;

import com.autonomouslogic.everef.refdata.ReferenceDataSpec;
import java.net.URI;
import java.time.Duration;
import java.time.Period;

public class Configs {
	/**
	 * The version of EVE Ref.
	 */
	public static final Config<String> EVE_REF_VERSION = Config.<String>builder()
			.name("EVE_REF_VERSION")
			.type(String.class)
			.defaultValue("dev")
			.build();

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
	 * Whether command successes should be reported to Slack.
	 */
	public static final Config<Boolean> SLACK_REPORT_SUCCESS = Config.<Boolean>builder()
			.name("SLACK_REPORT_SUCCESS")
			.defaultValue(true)
			.type(Boolean.class)
			.build();

	/**
	 * Whether command failures should be reported to Slack.
	 */
	public static final Config<Boolean> SLACK_REPORT_FAILURE = Config.<Boolean>builder()
			.name("SLACK_REPORT_FAILURE")
			.defaultValue(true)
			.type(Boolean.class)
			.build();

	/**
	 * Whether full stack traces should be reported to Slack.
	 */
	public static final Config<Boolean> SLACK_REPORT_FULL_STACKTRACE = Config.<Boolean>builder()
			.name("SLACK_REPORT_FULL_STACKTRACE")
			.defaultValue(false)
			.type(Boolean.class)
			.build();

	/**
	 * The base path used when accessing the ESI.
	 */
	public static final Config<URI> ESI_BASE_URL = Config.<URI>builder()
			.name("ESI_BASE_URL")
			.defaultValue(URI.create("https://esi.evetech.net/latest"))
			.type(URI.class)
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
	 * The location to store data for the data site.
	 */
	public static final Config<URI> DATA_PATH =
			Config.<URI>builder().name("DATA_PATH").type(URI.class).build();

	/**
	 * The base URL used for fetching data from the data site.
	 */
	public static final Config<URI> DATA_BASE_URL = Config.<URI>builder()
			.name("DATA_BASE_URL")
			.type(URI.class)
			.defaultValue(URI.create("https://data.everef.net"))
			.build();

	/**
	 * The cache time to use for data index pages.
	 */
	public static final Config<Duration> DATA_INDEX_CACHE_CONTROL_MAX_AGE = Config.<Duration>builder()
			.name("DATA_INDEX_CACHE_CONTROL_MAX_AGE")
			.defaultValue(Duration.ofMinutes(2))
			.type(Duration.class)
			.build();

	/**
	 * The cache time to use for archive files.
	 */
	public static final Config<Duration> DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE = Config.<Duration>builder()
			.name("DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE")
			.defaultValue(Duration.ofDays(30))
			.type(Duration.class)
			.build();

	/**
	 * The cache time to use for latest files.
	 */
	public static final Config<Duration> DATA_LATEST_CACHE_CONTROL_MAX_AGE = Config.<Duration>builder()
			.name("DATA_LATEST_CACHE_CONTROL_MAX_AGE")
			.defaultValue(Duration.ofMinutes(2))
			.type(Duration.class)
			.build();

	/**
	 * Number of index pages to build and upload concurrently.
	 */
	public static final Config<Integer> DATA_INDEX_CONCURRENCY = Config.<Integer>builder()
			.name("DATA_INDEX_CONCURRENCY")
			.defaultValue(50)
			.type(Integer.class)
			.build();

	/**
	 * The AWS region for the data site.
	 * If not supplied, normal AWS SDK defaults will be used.
	 */
	public static final Config<String> DATA_AWS_REGION =
			Config.<String>builder().name("DATA_AWS_REGION").type(String.class).build();

	/**
	 * Endpoint override for the data site S3 client.
	 */
	public static final Config<String> DATA_S3_ENDPOINT_URL = Config.<String>builder()
			.name("DATA_S3_ENDPOINT_URL")
			.type(String.class)
			.build();

	/**
	 * Profile for the data S3 client.
	 */
	public static final Config<String> DATA_AWS_PROFILE =
			Config.<String>builder().name("DATA_AWS_PROFILE").type(String.class).build();

	/**
	 * Access key for the data S3 client.
	 */
	public static final Config<String> DATA_AWS_ACCESS_KEY_ID = Config.<String>builder()
			.name("DATA_AWS_ACCESS_KEY_ID")
			.type(String.class)
			.build();

	/**
	 * Secret key for the data S3 client.
	 */
	public static final Config<String> DATA_AWS_SECRET_ACCESS_KEY = Config.<String>builder()
			.name("DATA_AWS_SECRET_ACCESS_KEY")
			.type(String.class)
			.build();

	/**
	 * Chunk size for the market history scrape.
	 */
	public static final Config<Integer> ESI_MARKET_HISTORY_CHUNK_SIZE = Config.<Integer>builder()
			.name("ESI_MARKET_HISTORY_CHUNK_SIZE")
			.type(Integer.class)
			.defaultValue(Integer.MAX_VALUE)
			.build();

	/**
	 * Concurrency for the market history scrape.
	 */
	public static final Config<Integer> ESI_MARKET_HISTORY_CONCURRENCY = Config.<Integer>builder()
			.name("ESI_MARKET_HISTORY_CONCURRENCY")
			.type(Integer.class)
			.defaultValue(8)
			.build();

	/**
	 * Amount of time to look back when fetching market history.
	 */
	public static final Config<Period> ESI_MARKET_HISTORY_LOOKBACK = Config.<Period>builder()
			.name("ESI_MARKET_HISTORY_LOOKBACK")
			.type(Period.class)
			.defaultValue(Period.ofDays(450))
			.build();

	/**
	 * The amount of time to wait for once the special rate limit for market history has been exceeded.
	 */
	public static final Config<Duration> ESI_MARKET_HISTORY_RATE_LIMIT_WAIT_TIME = Config.<Duration>builder()
			.name("ESI_MARKET_HISTORY_RATE_LIMIT_WAIT_TIME")
			.type(Duration.class)
			.defaultValue(Duration.ofMinutes(1).plusSeconds(10))
			.build();

	/**
	 * The number of times to try once the special rate limit for market history has been exceeded.
	 */
	public static final Config<Integer> ESI_MARKET_HISTORY_RATE_LIMIT_TRIES = Config.<Integer>builder()
			.name("ESI_MARKET_HISTORY_RATE_LIMIT_TRIES")
			.type(Integer.class)
			.defaultValue(3)
			.build();

	/**
	 * User agent string to provide to the ESI.
	 */
	public static final Config<String> ESI_USER_AGENT =
			Config.<String>builder().name("ESI_USER_AGENT").type(String.class).build();

	/**
	 * Rate limit to apply for ESI requests.
	 */
	public static final Config<Integer> ESI_RATE_LIMIT_PER_S = Config.<Integer>builder()
			.name("ESI_RATE_LIMIT_PER_S")
			.defaultValue(50)
			.type(Integer.class)
			.build();

	/**
	 * Number of threads for ESI requests.
	 */
	public static final Config<Integer> ESI_HTTP_THREADS = Config.<Integer>builder()
			.name("ESI_HTTP_THREADS")
			.defaultValue(Runtime.getRuntime().availableProcessors() * 2)
			.type(Integer.class)
			.build();

	/**
	 * The base URL used for fetching SDE complements from Hoboleaks.
	 */
	public static final Config<URI> HOBOLEAKS_SDE_DATA_BASE_URL = Config.<URI>builder()
			.name("HOBOLEAKS_SDE_DATA_BASE_URL")
			.type(URI.class)
			.defaultValue(URI.create("https://sde.hoboleaks.space/tq/"))
			.build();

	/**
	 * User agent string to provide to anything other than the ESI.
	 */
	public static final Config<String> HTTP_USER_AGENT = Config.<String>builder()
			.name("HTTP_USER_AGENT")
			.type(String.class)
			.defaultValue("everef.net")
			.build();

	/**
	 * Directory for HTTP caching.
	 */
	public static final Config<String> HTTP_CACHE_DIR = Config.<String>builder()
			.name("HTTP_CACHE_DIR")
			.type(String.class)
			.defaultValue(System.getProperty("java.io.tmpdir") + "/eve-ref-http-cache")
			.build();

	/**
	 * Maximum size of the HTTP disk cache in megabytes.
	 */
	public static final Config<Long> HTTP_CACHE_SIZE_MB = Config.<Long>builder()
			.name("HTTP_CACHE_SIZE_MB")
			.type(Long.class)
			.defaultValue(512L)
			.build();

	/**
	 * Number of concurrent downloads when loading market history.
	 */
	public static final Config<Integer> MARKET_HISTORY_LOAD_CONCURRENCY = Config.<Integer>builder()
			.name("MARKET_HISTORY_LOAD_CONCURRENCY")
			.type(Integer.class)
			.defaultValue(8)
			.build();

	/**
	 * Number of concurrent uploads when saving market history.
	 */
	public static final Config<Integer> MARKET_HISTORY_SAVE_CONCURRENCY = Config.<Integer>builder()
			.name("MARKET_HISTORY_SAVE_CONCURRENCY")
			.type(Integer.class)
			.defaultValue(8)
			.build();

	/**
	 * The maximum runtime to aim for when examining top traded types.
	 */
	public static final Config<Duration> MARKET_HISTORY_TOP_TRADED_MAX_TIME = Config.<Duration>builder()
			.name("MARKET_HISTORY_TOP_TRADED_MAX_TIME")
			.type(Duration.class)
			.defaultValue(Duration.ofHours(2))
			.build();

	/**
	 * The cache size to use for MVStores.
	 */
	public static final Config<Integer> MVSTORE_CACHE_SIZE_MB = Config.<Integer>builder()
			.name("MVSTORE_CACHE_SIZE_MB")
			.type(Integer.class)
			.defaultValue(128)
			.build();

	/**
	 * The cache time to use for reference data files.
	 */
	public static final Config<Duration> REFERENCE_DATA_CACHE_CONTROL_MAX_AGE = Config.<Duration>builder()
			.name("REFERENCE_DATA_CACHE_CONTROL_MAX_AGE")
			.defaultValue(Duration.ofMinutes(2))
			.type(Duration.class)
			.build();

	/**
	 * The location to store data for the reference data API site.
	 */
	public static final Config<URI> REFERENCE_DATA_PATH =
			Config.<URI>builder().name("REFERENCE_DATA_PATH").type(URI.class).build();

	/**
	 * Endpoint override for the reference data S3 client.
	 */
	public static final Config<String> REFERENCE_DATA_S3_ENDPOINT_URL = Config.<String>builder()
			.name("REFERENCE_DATA_S3_ENDPOINT_URL")
			.type(String.class)
			.build();

	/**
	 * Profile for the reference data S3 client.
	 */
	public static final Config<String> REFERENCE_DATA_AWS_PROFILE = Config.<String>builder()
			.name("REFERENCE_DATA_AWS_PROFILE")
			.type(String.class)
			.build();

	/**
	 * Access key for the reference data S3 client.
	 */
	public static final Config<String> REFERENCE_DATA_AWS_ACCESS_KEY_ID = Config.<String>builder()
			.name("REFERENCE_DATA_AWS_ACCESS_KEY_ID")
			.type(String.class)
			.build();

	/**
	 * Secret key for the reference data S3 client.
	 */
	public static final Config<String> REFERENCE_DATA_AWS_SECRET_ACCESS_KEY = Config.<String>builder()
			.name("REFERENCE_DATA_AWS_SECRET_ACCESS_KEY")
			.type(String.class)
			.build();

	/**
	 * The AWS region for the reference data site.
	 * If not supplied, normal AWS SDK defaults will be used.
	 */
	public static final Config<String> REFERENCE_DATA_AWS_REGION = Config.<String>builder()
			.name("REFERENCE_DATA_AWS_REGION")
			.type(String.class)
			.build();

	/**
	 * Base path for requests to the main EVE Ref website.
	 */
	public static final Config<URI> EVE_REF_BASE_URL = Config.<URI>builder()
			.name("EVE_REF_BASE_URL")
			.type(URI.class)
			.defaultValue(URI.create("https://everef.net"))
			.build();

	/**
	 * URL for the Hoboleaks <code>dynamicitemattributes.json</code> file.
	 */
	public static final Config<URI> HOBOLEAKS_DYNAMIC_ATTRIBUTES_URL = Config.<URI>builder()
			.name("HOBOLEAKS_DYNAMIC_ATTRIBUTES_URL")
			.type(URI.class)
			.defaultValue(URI.create("https://sde.hoboleaks.space/tq/dynamicitemattributes.json"))
			.build();

	/**
	 * Base path for requests to the main EVE Ref website.
	 */
	public static final Config<URI> REF_DATA_BASE_URL = Config.<URI>builder()
			.name("REF_DATA_BASE_URL")
			.type(URI.class)
			.defaultValue(URI.create(ReferenceDataSpec.BASE_URL))
			.build();

	/**
	 * Base path for fuzzwork.co.uk
	 */
	public static final Config<URI> FUZZWORK_BASE_URL = Config.<URI>builder()
			.name("FUZZWORK_BASE_URL")
			.type(URI.class)
			.defaultValue(URI.create("https://market.fuzzwork.co.uk/"))
			.build();

	/**
	 * Whether to force ref data processing.
	 */
	public static final Config<Boolean> FORCE_REF_DATA = Config.<Boolean>builder()
			.name("FORCE_REF_DATA")
			.defaultValue(false)
			.type(Boolean.class)
			.build();
}
