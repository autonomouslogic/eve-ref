package com.autonomouslogic.everef.inject;

import com.autonomouslogic.everef.Main;
import dagger.Component;
import javax.inject.Singleton;

@Component(modules = {})
@Singleton
public interface MainComponent {
	void inject(Main main);

	Main createMain();

	static MainComponent create() {
		return DaggerMainComponent.create();
	}
}
