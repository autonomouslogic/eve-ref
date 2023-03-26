package com.autonomouslogic.everef.test;

import com.autonomouslogic.everef.cli.DataIndexTest;
import com.autonomouslogic.everef.cli.MockDataIndexModule;
import com.autonomouslogic.everef.cli.marketorders.ScrapeMarketOrdersTest;
import com.autonomouslogic.everef.esi.EsiHelperTest;
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
			MockOkHttpModule.class,
			MockDataIndexModule.class,
			MockLocationPopulatorModule.class
		})
@Singleton
public interface TestComponent {
	void inject(DataIndexTest test);

	void inject(UrlParserTest test);

	void inject(ScrapeMarketOrdersTest test);

	void inject(EsiHelperTest test);

	void inject(LocationPopulatorTest test);
}
