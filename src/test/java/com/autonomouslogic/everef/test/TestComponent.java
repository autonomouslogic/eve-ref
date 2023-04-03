package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.cli.DataIndexTest;
import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.cli.decorator.HealthcheckDecoratorTest;
import com.autonomouslogic.everef.cli.decorator.SlackDecoratorTest;
import com.autonomouslogic.everef.cli.marketorders.ScrapeMarketOrdersTest;
import com.autonomouslogic.everef.esi.EsiHelperTest;
import com.autonomouslogic.everef.esi.EsiLimitExceededInterceptorTest;
import com.autonomouslogic.everef.esi.EsiRateLimitInterceptorTest;
import com.autonomouslogic.everef.esi.LocationPopulatorTest;
import com.autonomouslogic.everef.esi.MockLocationPopulatorModule;
import com.autonomouslogic.everef.inject.AwsModule;
import com.autonomouslogic.everef.inject.EsiModule;
import com.autonomouslogic.everef.inject.JacksonModule;
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
			MockLocationPopulatorModule.class
		})
@Singleton
public interface TestComponent {
	void inject(DataIndexTest test);

	void inject(EsiHelperTest test);

	void inject(EsiLimitExceededInterceptorTest test);

	void inject(EsiRateLimitInterceptorTest test);

	void inject(HealthcheckDecoratorTest test);

	void inject(LocationPopulatorTest test);

	void inject(ScrapeMarketOrdersTest test);

	void inject(SlackDecoratorTest test);

	void inject(UrlParserTest test);
}
