package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.config.Configs;
import com.autonomouslogic.everef.model.api.InventorySearchResponse;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.service.TypeSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.log4j.Log4j2;

import jakarta.inject.Inject;

import javax.ws.rs.Path;
import java.time.Duration;
import java.util.List;

@Tag(name = "search")
@Path("/v1/search")
@Log4j2
public class SearchHandler implements HttpService, Handler {
    private static final String cacheControlHeader = String.format(
            "public, max-age=%d, immutable", Duration.ofMinutes(10).toSeconds());
    private final String eveRefVersion = Configs.EVE_REF_VERSION.getRequired();

    @Inject
    protected ObjectMapper objectMapper;

    @Inject
    protected RefDataService refDataService;

    @Inject
    protected TypeSearchService typeSearchService;

    @Inject
    public SearchHandler() {
    }

    protected InventorySearchResponse search(String q) {
        if (q == null || q.length() < 3) {
            return InventorySearchResponse.builder()
                    .input(q != null ? q : "")
                    .build();
        }

        List<InventoryType> searchResults = typeSearchService
                .setRefData(refDataService.getLoadedRefData())
                .searchType(q);

        return InventorySearchResponse.builder()
                .input(q)
                .inventoryTypes(searchResults)
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