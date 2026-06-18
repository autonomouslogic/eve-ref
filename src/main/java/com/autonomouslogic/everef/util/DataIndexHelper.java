package com.autonomouslogic.everef.util;

import com.autonomouslogic.everef.cli.DataIndex;
import com.autonomouslogic.everef.url.S3Url;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Log4j2
public class DataIndexHelper {
	@Inject
	protected Provider<DataIndex> dataIndexProvider;

	@Inject
	public DataIndexHelper() {}

	public Completable updateIndex(S3Url... urls) {
		return updateIndex(List.of(urls));
	}

	public Completable updateIndex(List<S3Url> urls) {
		log.debug("Updating data index for {} S3 URLs", urls.size());
		return resolvePrefixes(urls).flatMapCompletable(this::updateIndex, false, 1);
	}

	private Flowable<String> resolvePrefixes(List<S3Url> urls) {
		return Flowable.fromIterable(urls)
				.map(S3Url::getPath)
				.flatMap(path -> {
					var list = new ArrayList<String>();
					while (!path.isEmpty()) {
						path = FilenameUtils.getPath(StringUtils.removeEnd(path, "/"));
						list.add(path);
					}
					return Flowable.fromIterable(list);
				})
				.distinct();
	}

	private Completable updateIndex(String prefix) {
		return Completable.fromAction(() -> {
			dataIndexProvider.get().setPrefix(prefix).setRecursive(false).run();
		});
	}
}
