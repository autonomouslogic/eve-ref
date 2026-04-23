package com.autonomouslogic.everef.s3;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.ArchivePathFactory;
import com.autonomouslogic.everef.util.DataIndexHelper;
import java.io.File;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Utility for working with S3.
 */
@Singleton
@Log4j2
public class S3Util {
	@Inject
	protected S3Adapter s3Adapter;

	@Inject
	protected DataIndexHelper dataIndexHelper;

	@Inject
	protected S3Util() {}

	public PutObjectRequest putObjectRequest(long len, S3Url url) {
		return PutObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.contentLength(len)
				.build();
	}

	public PutObjectRequest putObjectRequest(long len, S3Url url, String contentType) {
		return putObjectRequest(len, url).toBuilder().contentType(contentType).build();
	}

	public PutObjectRequest putObjectRequest(long len, S3Url url, String contentType, Duration maxAge) {
		return putObjectRequest(len, url, contentType).toBuilder()
				.cacheControl(cacheControl(maxAge))
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url) {
		return putObjectRequest(len, url).toBuilder()
				.acl(ObjectCannedACL.PUBLIC_READ)
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url, Duration maxAge) {
		return putPublicObjectRequest(len, url).toBuilder()
				.cacheControl(cacheControl(maxAge))
				.build();
	}

	public PutObjectRequest putPublicObjectRequest(long len, S3Url url, String contentType, Duration maxAge) {
		return putPublicObjectRequest(len, url, maxAge).toBuilder()
				.contentType(contentType)
				.build();
	}

	public DeleteObjectRequest deleteObjectRequest(S3Url url) {
		return DeleteObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.build();
	}

	public String cacheControl(Duration maxAge) {
		return cacheControl(maxAge, false);
	}

	public String cacheControl(Duration maxAge, boolean immutable) {
		var parts = new ArrayList<String>();
		parts.add("public");
		parts.add("max-age=" + maxAge.toSeconds());
		if (immutable) {
			parts.add("immutable");
		}
		return String.join(", ", parts);
	}

	public GetObjectRequest getObjectRequest(S3Url url) {
		return GetObjectRequest.builder()
				.bucket(url.getBucket())
				.key(url.getPath())
				.build();
	}

	/**
	 * Uploads a file to both the latest and archive paths in S3, then updates the data index.
	 * This centralizes the common pattern of uploading timestamped data files with latest+archive structure.
	 *
	 * @param file the file to upload to both latest and archive paths
	 * @param baseUrl the base S3 URL to resolve paths from
	 * @param pathFactory the factory for creating latest and archive paths
	 * @param archiveTime the timestamp to use in the archive path
	 * @param contentType the MIME type for the uploaded files
	 * @param s3Client the S3 async client to use for uploads
	 */
	public void uploadLatestAndArchive(
			@NonNull File file,
			@NonNull S3Url baseUrl,
			@NonNull ArchivePathFactory pathFactory,
			@NonNull ZonedDateTime archiveTime,
			@NonNull String contentType,
			@NonNull S3AsyncClient s3Client) {
		var latestCacheTime = Configs.DATA_LATEST_CACHE_CONTROL_MAX_AGE.getRequired();
		var archiveCacheTime = Configs.DATA_ARCHIVE_CACHE_CONTROL_MAX_AGE.getRequired();

		var latestPath = baseUrl.resolve(pathFactory.createLatestPath());
		var archivePath = baseUrl.resolve(pathFactory.createArchivePath(archiveTime));

		var latestPut = putPublicObjectRequest(file.length(), latestPath, contentType, latestCacheTime);
		var archivePut = putPublicObjectRequest(file.length(), archivePath, contentType, archiveCacheTime);

		log.info("Uploading latest file to {}", latestPath);
		log.info("Uploading archive file to {}", archivePath);

		s3Adapter.putObject(latestPut, file, s3Client);
		s3Adapter.putObject(archivePut, file, s3Client);

		dataIndexHelper.updateIndex(latestPath, archivePath).blockingAwait();
	}
}
