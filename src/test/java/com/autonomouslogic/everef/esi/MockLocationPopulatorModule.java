package com.autonomouslogic.everef.esi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dagger.MembersInjector;
import dagger.Module;
import dagger.Provides;
import io.reactivex.rxjava3.core.Completable;
import javax.inject.Singleton;
import lombok.Setter;
import org.mockito.stubbing.Answer;

/**
 * Specifically for overriding DataIndex, when needed.
 */
@Module
public class MockLocationPopulatorModule {
	@Setter
	private LocationPopulator locationPopulator;

	@Provides
	@Singleton
	public LocationPopulator locationPopulator(MembersInjector<LocationPopulator> injector) {
		if (locationPopulator != null) {
			return locationPopulator;
		}
		// Fall back to the real thing.
		var locationPopulator = new LocationPopulator();
		injector.injectMembers(locationPopulator);
		return locationPopulator;
	}

	public static Answer<Completable> mockPopulate() {
		return invocation -> {
			var record = invocation.getArgument(0, ObjectNode.class);
			if (!record.has("region_id")) {
				record.put("region_id", 999);
			}
			if (!record.has("constellation_id")) {
				record.put("constellation_id", 999);
			}
			if (!record.has("system_id")) {
				record.put("system_id", 999);
			}
			if (!record.has("station_id")) {
					record.put("station_id", 999);
			}
			return Completable.complete();
		};
	}
}
