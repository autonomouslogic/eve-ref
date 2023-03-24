package com.autonomouslogic.everef.test;

import static org.mockito.Mockito.mock;

import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.util.NetUtil;
import dagger.Module;
import dagger.Provides;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.gaul.s3proxy.S3Proxy;
import org.gaul.s3proxy.junit.S3ProxyJunitCore;
import org.gaul.s3proxy.junit.S3ProxyRule;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.endpoints.S3EndpointUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Properties;

@Module
@Log4j2
public class MockS3Module {
	@Provides
	@Named("s3-port")
	@Singleton
	public int s3Port() {
		return NetUtil.getFreePort();
	}

	@Provides
	@Named("s3-tmp-dir")
	@Singleton
	@SneakyThrows
	public File s3TmpDir() {
		var file = Files.createTempDirectory("s3-tmp-").toFile();
		file.deleteOnExit();
		return file;
	}

	@Provides
	@Singleton
	public S3ProxyJunitCore s3Proxy(@Named("s3-port") int port, @Named("s3-tmp-dir") File dir) {
		log.info(String.format("Prepping S3 mock on port %s and tmp dir %s", port, dir));

//		S3MockApplication.start(Collections.emptyMap(), "").stop();

		var properties = new Properties();
		properties.setProperty("jclouds.filesystem.basedir", dir.getAbsolutePath());

		var context = ContextBuilder.newBuilder("filesystem")
			.overrides(properties)
			//.credentials("identity", "credential")
			.build(BlobStoreContext.class);

//		return S3Proxy.builder()
//			.blobStore(context.getBlobStore())
//			.endpoint(URI.create("http://127.0.0.1:" + port))
//			.build();

		var proxy = new S3ProxyJunitCore.Builder().withBlobStoreProvider("filesystem").withPort(port).build();
		return proxy;
	}

	@Provides
	@Named("data")
	@Singleton
	@SneakyThrows
	public S3AsyncClient dataClient(@Named("s3-port") int port) {
		return S3AsyncClient.builder()
			.credentialsProvider(AnonymousCredentialsProvider.create())
			.endpointOverride(new URI(String.format("http://localhost:%s", port))).build();
	}
}
