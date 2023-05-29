package com.autonomouslogic.everef.cli;

import com.autonomouslogic.everef.model.ReferenceEntry;
import com.autonomouslogic.everef.refdata.DogmaAttribute;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.util.OkHttpHelper;
import com.autonomouslogic.everef.util.RefDataUtil;
import com.autonomouslogic.everef.util.TempFiles;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

/**
 * Loads the reference data archive and verifies all entries can be parsed using the models.
 */
@Log4j2
public class VerifyRefDataModels implements Command {
	@Inject
	protected OkHttpClient okHttpClient;

	@Inject
	protected OkHttpHelper okHttpHelper;

	@Inject
	protected TempFiles tempFiles;

	@Inject
	protected RefDataUtil refDataUtil;

	private final ObjectMapper objectMapper;
	private final CollectionType listOfInts;

	@Inject
	protected VerifyRefDataModels(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		listOfInts = this.objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class);
	}

	@SneakyThrows
	@Override
	public Completable run() {
		return refDataUtil
				.downloadLatestReferenceData()
				.flatMapPublisher(file -> refDataUtil.parseReferenceDataArchive(file))
				.flatMapCompletable(
						entry -> verify(entry), false, Runtime.getRuntime().availableProcessors());
	}

	private Completable verify(@NonNull ReferenceEntry entry) {
		return Completable.fromAction(() -> {
			try {
				switch (entry.getType()) {
					case "meta":
						break;
					case "types":
						verifyTypes(entry);
						break;
					case "dogma-attributes":
						verifyDogmaAttributes(entry);
						break;
					default:
						throw new IllegalStateException("Unknown type: " + entry.getType());
				}
			} catch (Exception e) {
				throw new RuntimeException(
						String.format("Failed verifying %s [%s]", entry.getType(), entry.getId()), e);
			}
		});
	}

	@SneakyThrows
	private void verifyTypes(@NonNull ReferenceEntry entry) {
		if (entry.getId() == null) {
			objectMapper.readValue(entry.getContent(), listOfInts);
		} else {
			objectMapper.readValue(entry.getContent(), InventoryType.class);
		}
	}

	@SneakyThrows
	private void verifyDogmaAttributes(@NonNull ReferenceEntry entry) {
		if (entry.getId() == null) {
			objectMapper.readValue(entry.getContent(), listOfInts);
		} else {
			objectMapper.readValue(entry.getContent(), DogmaAttribute.class);
		}
	}
}
