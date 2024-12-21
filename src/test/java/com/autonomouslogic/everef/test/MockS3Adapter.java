package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.s3.ListedS3Object;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.util.HashUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A testable implementation of {@link S3Adapter}.
 *
 * I've tried using both <a href="https://github.com/gaul/s3proxy">gaul/s3proxy</a> and
 * <a href="https://github.com/adobe/S3Mock">adobe/S3Mock</a>, but was unable to make either of them work.
 *
 * So in its place, I've created this dirty bare-bones mock implementation, which is better than a bunch of nasty
 * {@link org.mockito.Mockito#mock(Class)} and {@link org.mockito.Mockito#verify(Object)} calls.
 */
public class MockS3Adapter extends S3Adapter {
	private final Map<Entry, byte[]> data = new ConcurrentHashMap<>();
	private final Set<Entry> putKeys = Collections.synchronizedSet(new LinkedHashSet<>());
	private final Set<Entry> deleteKeys = Collections.synchronizedSet(new LinkedHashSet<>());

	@Value
	private class Entry {
		String bucket;
		String key;
		S3AsyncClient client;

		@EqualsAndHashCode.Exclude
		Instant lastModified;
	}

	protected MockS3Adapter() {
		tempFiles = new TempFiles();
	}

	public void putTestObject(String bucket, String key, String content, S3AsyncClient client) {
		putTestObject(bucket, key, content, client, Instant.now());
	}

	public void putTestObject(String bucket, String key, String content, S3AsyncClient client, Instant lastModified) {
		putTestObject(bucket, key, content.getBytes(), client, lastModified);
	}

	public void putTestObject(String bucket, String key, byte[] bytes, S3AsyncClient client) {
		putTestObject(bucket, key, bytes, client, Instant.now());
	}

	public void putTestObject(String bucket, String key, byte[] bytes, S3AsyncClient client, Instant lastModified) {
		data.put(new Entry(bucket, key, client, lastModified), bytes);
	}

	public Optional<byte[]> getTestObject(String bucket, String key, S3AsyncClient client) {
		return Optional.ofNullable(data.get(new Entry(bucket, key, client, null)));
	}

	@Override
	@SneakyThrows
	public Single<GetObjectResponse> getObject(GetObjectRequest get, Path destination, S3AsyncClient s3Client) {
		return Single.fromCallable(() -> {
			var bytes = getTestObject(get.bucket(), get.key(), s3Client);
			if (bytes.isEmpty()) {
				// @todo I think this is correct
				throw new CompletionException(NoSuchKeyException.builder().build());
			}
			try (var in = new ByteArrayInputStream(bytes.get());
					var out = new FileOutputStream(destination.toFile())) {
				IOUtils.copy(in, out);
			}
			return GetObjectResponse.builder().build();
		});
	}

	@Override
	public Flowable<ListedS3Object> listObjects(S3Url url, boolean recursive, S3AsyncClient client) {
		return Flowable.defer(() -> {
			var stream = data.entrySet().stream()
					.filter(entry -> entry.getKey().getClient() == client)
					.filter(entry -> entry.getKey().getBucket().equals(url.getBucket()));
			if (!Strings.isNullOrEmpty(url.getPath())) {
				stream = stream.filter(entry -> entry.getKey().getKey().startsWith(url.getPath()));
			}
			var prefix = url.getPath();
			return Flowable.fromStream(stream).map(entry -> {
				var prefixRelative = StringUtils.removeStart(entry.getKey().getKey(), prefix);
				if (!recursive && prefixRelative.contains("/")) {
					return ListedS3Object.create(
							CommonPrefix.builder()
									.prefix(entry.getKey().getKey() + "/")
									.build(),
							url.getBucket());
				} else {
					return ListedS3Object.create(
							S3Object.builder()
									.key(entry.getKey().getKey())
									.size((long) entry.getValue().length)
									.eTag("\"" + Hex.encodeHexString(HashUtil.md5(entry.getValue())) + "\"")
									.lastModified(entry.getKey().getLastModified())
									.build(),
							entry.getKey().getBucket());
				}
			});
		});
	}

	@Override
	public Single<PutObjectResponse> putObject(PutObjectRequest req, byte[] bytes, S3AsyncClient client) {
		return Single.defer(() -> {
			var entry = new Entry(req.bucket(), req.key(), client, Instant.now());
			data.put(entry, bytes);
			putKeys.add(entry);
			putDirectory(req.bucket(), req.key(), client);
			return Single.just(PutObjectResponse.builder().build());
		});
	}

	@Override
	public Single<PutObjectResponse> putObject(PutObjectRequest req, AsyncRequestBody body, S3AsyncClient client) {
		return Single.defer(() -> {
			return Flowable.fromPublisher(body)
					.reduce(new byte[0], (a, b) -> {
						var result = new byte[a.length + b.remaining()];
						System.arraycopy(a, 0, result, 0, a.length);
						b.get(result, a.length, b.remaining());
						return result;
					})
					.flatMap(buffer -> {
						return putObject(req, buffer, client);
					});
		});
	}

	@Override
	public Single<PutObjectResponse> putObject(PutObjectRequest req, File file, S3AsyncClient client) {
		return Single.defer(() -> {
			return putObject(req, IOUtils.toByteArray(new FileInputStream(file)), client);
		});
	}

	/**
	 * Create a directory entry for each part of the key.
	 * This is for accurate simulation of Backblaze B2 buckets.
	 * S3 will not return these, but B2 will.
	 */
	private void putDirectory(String bucket, String key, S3AsyncClient client) {
		if (key.endsWith("/")) {
			return;
		}
		var parts = key.split("/");
		var dir = "";
		for (var i = 0; i < parts.length - 1; i++) {
			dir += parts[i] + "/";
			data.put(new Entry(bucket, dir, client, null), new byte[0]);
		}
	}

	@Override
	public Single<DeleteObjectResponse> deleteObject(DeleteObjectRequest req, S3AsyncClient client) {
		return Single.defer(() -> {
			var entry = new Entry(req.bucket(), req.key(), client, null);
			deleteKeys.add(entry);
			return Single.just(DeleteObjectResponse.builder().build());
		});
	}

	public List<String> getAllPutKeys(String bucket, S3AsyncClient client) {
		return putKeys.stream()
				.filter(entry -> entry.getClient() == client)
				.filter(entry -> entry.getBucket().equals(bucket))
				.map(entry -> entry.getKey())
				.collect(Collectors.toList());
	}

	public List<String> getAllDeleteKeys(String bucket, S3AsyncClient client) {
		return deleteKeys.stream()
				.filter(entry -> entry.getClient() == client)
				.filter(entry -> entry.getBucket().equals(bucket))
				.map(entry -> entry.getKey())
				.collect(Collectors.toList());
	}

	public void assertSameContent(String bucket, String key1, String key2, S3AsyncClient client) {
		var bytes1 = data.get(new Entry(bucket, key1, client, null));
		var bytes2 = data.get(new Entry(bucket, key2, client, null));
		Assertions.assertEquals(bytes1.length, bytes2.length);
		var hash1 = Hashing.murmur3_128().hashBytes(bytes1);
		var hash2 = Hashing.murmur3_128().hashBytes(bytes2);
		Assertions.assertEquals(hash1, hash2);
	}
}
