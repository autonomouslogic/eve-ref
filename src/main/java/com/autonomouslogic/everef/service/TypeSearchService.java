package com.autonomouslogic.everef.service;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.refdata.InventoryType;
import jakarta.inject.Inject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Log4j2
public class TypeSearchService {

    private LoadedRefData loadedRefData;

    @Inject
    public TypeSearchService() {
    }

    public TypeSearchService setRefData(LoadedRefData loadedRefData) {
        this.loadedRefData = loadedRefData;
        return this;
    }

    protected Pattern getSearchPatter(String q) {
        String[] queryParts = q.trim().split("\\s+");
        String regexString = String.join(".*", queryParts);
        return Pattern.compile(regexString, Pattern.CASE_INSENSITIVE);

    }

    public List<InventoryType> searchType(String q, List<Long> categories) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }
        Pattern searchPatter = getSearchPatter(q);
        Stream<Pair<Long, InventoryType>> inventoryTypeStream = loadedRefData.getAllTypes()
                .filter(Objects::nonNull)
                .filter(type -> type.getValue().getPublished());

        if (categories != null && !categories.isEmpty()) {
            inventoryTypeStream = inventoryTypeStream.filter(type -> {
                return categories.contains(type.getValue().getCategoryId());
            });
        }

        return inventoryTypeStream.filter(type -> {
                    String typeNameEn = type.getValue().getName().get("en");
                    if (typeNameEn == null) {
                        return false;
                    }
                    return searchPatter.matcher(typeNameEn).find();
                }).map(Pair::getValue)
                .collect(Collectors.toList());


    }
}
