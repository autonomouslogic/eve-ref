package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.model.api.search.InventorySearchResponse;
import com.autonomouslogic.everef.model.api.search.SearchInventoryType;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.refdata.MarketGroup;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.service.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;

import jakarta.inject.Inject;

import javax.ws.rs.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "search")
@Path("/v1/search")
@Log4j2
public class SearchHandler implements HttpService, Handler {
    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    protected RefDataService refDataService;

    @Inject
    protected SearchService searchService;

    @Inject
    protected ApiResponseUtil apiResponseUtil;

    @Inject
    public SearchHandler() {
    }

    protected InventorySearchResponse search(String q) {
        if (q == null || q.length() < 3) {
            return InventorySearchResponse.builder()
                    .input(q != null ? q : "")
                    .build();
        }

        List<InventoryType> searchResults = searchService.searchType(q);

        return InventorySearchResponse.builder()
                .input(q)
                .searchInventoryType(
                        searchResults.stream().map(item -> {
                            MarketGroup marketGroup = getRootMarketGroup(item, refDataService.getLoadedRefData());
                            return SearchInventoryType.builder()
                                    .typeId(item.getTypeId())
                                    .nameEn(item.getName().get("en"))
                                    .rootMarketGroup(Optional.ofNullable(marketGroup)
                                            .flatMap(g -> Optional.ofNullable(g.getName().get("en")))
                                            .orElse("Inventory type"))
                                    .rootMarketGroupId(Optional.ofNullable(marketGroup)
                                            .flatMap(g -> Optional.ofNullable(g.getMarketGroupId()))
                                            .orElse(null))
                                    .build();
                        }).collect(Collectors.toList())
                )
                .build();
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get(this);
    }

    @Override
    public void handle(ServerRequest req, ServerResponse res) {
        String q = req.query().first("q").orElse(null);
        InventorySearchResponse result = this.search(q);
        sendResponse(req, res, result);
    }

    public void sendResponse(ServerRequest req, ServerResponse res, InventorySearchResponse result) {
        try {
            var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
            res.status(Status.OK_200);
            apiResponseUtil.setStandardHeaders(res, Duration.ofMinutes(10));
            res.send(json);
        } catch (Exception e) {
            log.error("Error serializing search response", e);
            res.status(Status.INTERNAL_SERVER_ERROR_500).send();
        }
    }
    private MarketGroup getRootMarketGroup(InventoryType type, LoadedRefData loadedRefData) {
        if (type.getMarketGroupId() == null) {
            return null;
        }
        var marketGroup = loadedRefData.getMarketGroup(type.getMarketGroupId());
        if (marketGroup == null) {
            return null;
        }
        return getRootMarketGroup(marketGroup, loadedRefData);
    }

    private MarketGroup getRootMarketGroup(MarketGroup marketGroup, LoadedRefData loadedRefData) {
        if (marketGroup.getParentGroupId() == null) {
            return marketGroup;
        }
        var parentGroup = loadedRefData.getMarketGroup(marketGroup.getParentGroupId());
        if (parentGroup == null) {
            return marketGroup;
        }
        return getRootMarketGroup(parentGroup, loadedRefData);
    }
}