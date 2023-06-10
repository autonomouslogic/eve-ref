package com.autonomouslogic.everef.cli.refdata.post;

import com.autonomouslogic.everef.cli.refdata.StoreDataHelper;
import com.autonomouslogic.everef.cli.refdata.StoreHandler;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.Skill;
import com.autonomouslogic.everef.util.HoboleaksHelper;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Completable;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.h2.mvstore.MVMap;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Populates mutiplasmid data for inventory types.
 */
@Log4j2
public class MutiplasmidDecorator {
	private static final int MUTIPLASMID_GROUP_ID = 1964;

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected HoboleaksHelper hoboleaksHelper;

	@Setter
	@NonNull
	private StoreHandler storeHandler;

	private StoreDataHelper helper;
	private MVMap<Long, JsonNode> types;

	@Inject
	protected MutiplasmidDecorator() {}

	public Completable create() {
		return Completable.complete();
	}

}
