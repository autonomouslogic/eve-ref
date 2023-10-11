package com.autonomouslogic.everef.s3;

import com.autonomouslogic.everef.url.S3Url;
import java.time.Instant;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Value
@Builder(toBuilder = true)
public class ListedS3Object {
	@NonNull
	S3Url url;

	long size;

	Instant lastModified;

	String md5Hex;

	S3Object s3Object;

	ListObjectsV2Response s3Response;

	boolean directory;

	public static ListedS3Object create(CommonPrefix common, String bucket) {
		return ListedS3Object.builder()
				.url(S3Url.builder().bucket(bucket).path(common.prefix()).build())
				.directory(true)
				.build();
	}

	public static ListedS3Object create(S3Object obj, String bucket) {
		var md5Hex = Optional.ofNullable(obj.eTag())
				.map(e -> StringUtils.remove(e, "\""))
				.orElse(null);
		return ListedS3Object.builder()
				.url(S3Url.builder().bucket(bucket).path(obj.key()).build())
				.md5Hex(md5Hex)
				.size(obj.size())
				.lastModified(obj.lastModified())
				.s3Object(obj)
				.build();
	}
}
