package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.search.InventorySearchResponse;
import com.autonomouslogic.everef.model.api.search.SearchInventoryType;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.service.TypeSearchService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;

import jakarta.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Tag(name = "search")
@Path("/v1/search")
@Log4j2
public class SearchHandler implements HttpService, Handler {
    private static final String cacheControlHeader = String.format(
            "public, max-age=%d, immutable", Duration.ofMinutes(10).toSeconds());
    private final String eveRefVersion = Configs.EVE_REF_VERSION.getRequired();

    public static final List<Long> PRODUCED_CATEGORY_LIST = List.of(
            5L,   // Accessories
            6L,   // Ships
            7L,   // Module
            8L,   // Charges
            18L,  // Drones
            20L,  // Implants
            22L,  // Deployables
            32L,  // Subsystems
            39L,  // Infrastructure Upgrades
            40L,  // Sovereignty Structures
            87L  // Fighters
    );

    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    protected RefDataService refDataService;

    @Inject
    protected TypeSearchService typeSearchService;

    @Inject
    public SearchHandler() {
    }

    protected InventorySearchResponse search(String q, List<Long> categoryId) {
        if (q == null || q.length() < 3) {
            return InventorySearchResponse.builder()
                    .input(q != null ? q : "")
                    .build();
        }

        List<InventoryType> searchResults = typeSearchService
                .setRefData(refDataService.getLoadedRefData())
                .searchType(q, categoryId);

        return InventorySearchResponse.builder()
                .input(q)
                .searchInventoryTypes(searchResults
                        .stream()
                        .map( inventoryType -> SearchInventoryType.builder()
                                        .typeId(inventoryType.getTypeId())
                                        .nameEn(inventoryType.getName().get("en")).build()
                        ).collect(Collectors.toList())
                )
                .build();
    }

    private void handleProducedSearch(ServerRequest req, ServerResponse res) {
        String q = req.query().first("q").orElse(null);
        List<Long> categoriesToSearchIn = new ArrayList<>();
        InventorySearchResponse result = this.search(q, PRODUCED_CATEGORY_LIST);

        sendResponse(req, res, result);
    }

    private void handleAllSearch(ServerRequest req, ServerResponse res) {
        String q = req.query().first("q").orElse(null);

        InventorySearchResponse result = this.search(q, null);

        sendResponse(req, res, result);
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/produced", this::handleProducedSearch);
        rules.get("/all", this::handleAllSearch);
//        rules.any(StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
    }

    @Override
    public void handle(ServerRequest req, ServerResponse res) {
        res.status(Status.NOT_FOUND_404).send();
    }

    public void sendResponse(ServerRequest req, ServerResponse res, InventorySearchResponse result) {
        try {
            var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
            res.status(Status.OK_200)
                    .header("Server", "eve-ref/" + eveRefVersion)
                    .header("Content-Type", "application/json")
                    .header("X-Discord", "https://everef.net/discord")
                    .header("X-OpenAPI", "https://github.com/autonomouslogic/eve-ref/blob/main/spec/eve-ref-api.yaml")
//                    .header("X-Docs", "https://docs.everef.net/api/search.html")
                    .header("Cache-Control", cacheControlHeader)
                    .send(json);
        } catch (Exception e) {
            // Basic error handling for serialization failure
            log.error("Error serializing search response", e);
            res.status(Status.INTERNAL_SERVER_ERROR_500).send();
        }
    }
}