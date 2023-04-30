package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.cli.DataIndexTest;
import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.cli.decorator.HealthcheckDecoratorTest;
import com.autonomouslogic.everef.cli.decorator.SlackDecoratorTest;
import com.autonomouslogic.everef.cli.marketorders.ScrapeMarketOrdersTest;
import com.autonomouslogic.everef.cli.publiccontracts.ScrapePublicContractsTest;
import com.autonomouslogic.everef.cli.refdata.BuildRefDataTest;
import com.autonomouslogic.everef.esi.EsiHelperTest;
import com.autonomouslogic.everef.esi.EsiLimitExceededInterceptorTest;
import com.autonomouslogic.everef.esi.EsiRateLimitInterceptorTest;
import com.autonomouslogic.everef.esi.LocationPopulatorTest;
import com.autonomouslogic.everef.esi.MetaGroupScraperTest;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.http.DataCrawlerTest;
import com.autonomouslogic.everef.http.MockDataCrawlerModule;
import com.autonomouslogic.everef.inject.AwsModule;
import com.autonomouslogic.everef.inject.EsiModule;
import com.autonomouslogic.everef.inject.JacksonModule;
import com.autonomouslogic.everef.refdata.FieldRenamerTest;
import com.autonomouslogic.everef.refdata.ObjectMergerTest;
import com.autonomouslogic.everef.refdata.esi.EsiLoaderTest;
import com.autonomouslogic.everef.refdata.sde.SdeLoaderTest;
import com.autonomouslogic.everef.url.UrlParserTest;
import dagger.Component;
import javax.inject.Singleton;

@Component(
		modules = {
			JacksonModule.class,
			AwsModule.class,
			EsiModule.class,
			MockS3Module.class,
			TestOkHttpModule.class,
			MockDataIndexModule.class,
			MockLocationPopulatorModule.class,
			MockDataCrawlerModule.class
		})
@Singleton
public interface TestComponent {
	void inject(BuildRefDataTest test);

	void inject(DataCrawlerTest test);

	void inject(DataIndexTest test);

	void inject(EsiHelperTest test);

	void inject(EsiLoaderTest test);

	void inject(EsiLimitExceededInterceptorTest test);

	void inject(EsiRateLimitInterceptorTest test);

	void inject(HealthcheckDecoratorTest test);

	void inject(LocationPopulatorTest test);

	void inject(MetaGroupScraperTest test);

	void inject(ObjectMergerTest test);

	void inject(FieldRenamerTest test);

	void inject(ScrapeMarketOrdersTest test);

	void inject(ScrapePublicContractsTest test);

	void inject(SdeLoaderTest test);

	void inject(SlackDecoratorTest test);

	void inject(UrlParserTest test);
}
