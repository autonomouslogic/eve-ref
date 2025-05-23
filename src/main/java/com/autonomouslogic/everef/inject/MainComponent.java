package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.Main;
import dagger.Component;
import javax.inject.Singleton;

@Component(
		modules = {
			JacksonModule.class,
			AwsModule.class,
			S3Module.class,
			DynamoDBModule.class,
			EsiModule.class,
			OkHttpModule.class,
			RefDataApiModule.class,
			ExecutorServiceModule.class
		})
@Singleton
public interface MainComponent {
	void inject(Main main);

	Main createMain();

	static MainComponent create() {
		return DaggerMainComponent.create();
	}
}
