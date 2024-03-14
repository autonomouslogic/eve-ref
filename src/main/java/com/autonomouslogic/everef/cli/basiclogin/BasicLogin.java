package com.autonomouslogic.everef.cli.basiclogin;

import com.autonomouslogic.everef.cli.Command;
import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.http.OkHttpHelper;
import com.autonomouslogic.everef.s3.S3Adapter;
import com.autonomouslogic.everef.s3.S3Util;
import com.autonomouslogic.everef.url.HttpUrl;
import com.autonomouslogic.everef.url.S3Url;
import com.autonomouslogic.everef.url.UrlParser;
import com.autonomouslogic.everef.util.CompressUtil;
import com.autonomouslogic.everef.util.DataIndexHelper;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.runtime.Micronaut;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static com.autonomouslogic.everef.util.ArchivePathFactory.HOBOLEAKS;

/**
 * Fetches and stores all public contracts.
 */
@Log4j2
public class BasicLogin implements Command {
	@Inject
	protected BasicLoginFactory basicLoginFactory;

	private final int micronautPort = Configs.MICRONAUT_PORT.getRequired();

	@Inject
	protected BasicLogin() {}

	public Completable run() {
		return Completable.fromAction(() -> {
			Configs.EVE_OAUTH_CLIENT_ID.getRequired();
			Configs.EVE_OAUTH_SECRET_KEY.getRequired();

			Micronaut.build(new String[]{}).banner(false)
				.classes(BasicLoginController.class)
				.singletons(basicLoginFactory)
				.start();
			while (true) {
				Thread.sleep(1000);
			}
		});
	}
}
