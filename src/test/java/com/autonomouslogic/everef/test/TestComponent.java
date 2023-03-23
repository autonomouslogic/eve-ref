package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.cli.DataIndexTest;
import com.autonomouslogic.everef.inject.AwsModule;
import com.autonomouslogic.everef.inject.JacksonModule;
import com.autonomouslogic.everef.inject.S3Module;
import com.autonomouslogic.everef.scrape.ScrapeFetcherTest;
import com.autonomouslogic.everef.url.UrlParserTest;
import dagger.Component;
import javax.inject.Singleton;

@Component(modules = {JacksonModule.class, AwsModule.class, S3Module.class, MockS3Module.class})
@Singleton
public interface TestComponent {
	void inject(DataIndexTest test);

	void inject(UrlParserTest test);
	void inject(ScrapeFetcherTest test);
}
