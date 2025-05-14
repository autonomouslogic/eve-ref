package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.industry.IndustryCostCalculator;
import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.service.RefDataService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.helidon.http.Status;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Tag(name = "industry")
@Path("/v1/industry/cost")
public class IndustryCostHandler implements HttpService, Handler {
	private static final String cacheControlHeader = String.format(
			"public, max-age=%d, immutable", Duration.ofMinutes(10).toSeconds());

	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected Provider<IndustryCostCalculator> industryCostCalculatorProvider;

	private final ObjectMapper queryStringMapper;

	@Inject
	protected IndustryCostHandler(ObjectMapper objectMapper) {
		queryStringMapper = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	@GET
	@ApiResponse(
			responseCode = "200",
			description = "Success",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = IndustryCost.class)))
	@ApiResponse(
			responseCode = "400",
			description = "Client error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	@ApiResponse(
			responseCode = "500",
			description = "Server error",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
	@Parameter(
			in = ParameterIn.QUERY,
			name = "input",
			schema = @Schema(implementation = IndustryCostInput.class),
			explode = Explode.TRUE)
	public IndustryCost industryCost(IndustryCostInput input) {
		var calculator = industryCostCalculatorProvider.get().setIndustryCostInput(input);
		var refdata = Objects.requireNonNull(refDataService.getLoadedRefData(), "refdata");
		var productType = refdata.getType(input.getProductId());
		if (productType == null) {
			throw new ClientException(String.format("Product type ID %d not found", input.getProductId()));
		}
		calculator.setProductType(productType);
		var blueprints = Optional.ofNullable(productType.getProducedByBlueprints())
				.map(Map::values)
				.filter(c -> !c.isEmpty())
				.orElseThrow(() -> new ClientException(
						String.format("Product type ID %d is not produced from a blueprint", input.getProductId())));
		if (blueprints.size() > 1) {
			throw new ClientException(String.format(
					"Product type ID %d can be source from more than one blueprint", input.getProductId()));
		}
		var producingBlueprint = blueprints.stream().findFirst().orElseThrow();
		var activity = producingBlueprint.getBlueprintActivity();
		if (!activity.equals("manufacturing")) {
			throw new ClientException(String.format("Only manufacturing is supported, %s seen", activity));
		}
		var blueprintTypeId =
				Optional.ofNullable(producingBlueprint.getBlueprintTypeId()).orElseThrow();
		var blueprint = refdata.getBlueprint(blueprintTypeId);
		if (blueprint == null) {
			throw new ClientException(String.format("Blueprint ID %d not found", blueprintTypeId));
		}
		calculator.setBlueprint(blueprint);
		return calculator.calc().toBuilder().input(input).build();
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		var input = createInput(req);
		validateInput(input);
		var result = industryCost(input);
		var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
		res.status(Status.OK_200)
				.header(
						"X-OpenAPI",
						"https://github.com/autonomouslogic/eve-ref/blob/industry-api/spec/eve-ref-api.yaml")
				.header("Cache-Control", cacheControlHeader)
				.send(json);
	}

	private IndustryCostInput createInput(ServerRequest req) {
		var query = req.query();
		var input = new HashMap<String, Object>();
		for (var name : query.names()) {
			var values = query.all(name);
			if (values.size() == 1) {
				input.put(name, values.getFirst());
			} else {
				input.put(name, values);
			}
		}
		try {
			return queryStringMapper.convertValue(input, IndustryCostInput.class);
		} catch (IllegalArgumentException e) {
			throw new ClientException(ExceptionUtils.getMessage(e));
		}
	}

	private void validateInput(IndustryCostInput input) {
		if (input.getProductId() < 1) {
			throw new ClientException("Product ID must be provided");
		}
		if (input.getRuns() < 1) {
			throw new ClientException("Runs must be at least 1");
		}
		//		if ((input.getSystemId() == null) == (input.getSystemCostIndex() == null)) {
		//			throw new ClientException("Exactly one of system ID and system cost index must be provided");
		//		}
		//		if ((input.getSystemId() == null) == (input.getSecurityClass() == null)) {
		//			throw new ClientException("Exactly one of system ID and system security class must be provided");
		//		}
		//		if ((input.getRigTypeIds() != null && !input.getRigTypeIds().isEmpty())
		//				&& (input.getMeRigTechLevel() != null
		//						|| input.getTeRigTechLevel() != null
		//						|| input.getInventionRigTechLevel() != null
		//						|| input.getCopyingRigTechLevel() != null)) {
		//			throw new ClientException("Both rig type IDs and rig tech levels cannot be provided at the same time");
		//		}
		//		if (input.getIndustrySkills() == null) {
		//			throw new ClientException("Industry skills must be set");
		//		}
		//		validateSkill(input.getIndustrySkills().getIndustry(), "Industry");
		//		validateSkill(input.getIndustrySkills().getResearch(), "Research");
		//		validateSkill(input.getIndustrySkills().getScience(), "Science");
		//		validateSkill(input.getIndustrySkills().getAdvancedIndustry(), "Advanced Industry");
		//		validateSkill(input.getIndustrySkills().getMetallurgy(), "Metallurgy");
		//		validateSkill(input.getIndustrySkills().getDatacore1(), "Datacore #1");
		//		validateSkill(input.getIndustrySkills().getDatacore2(), "Datacore #2");
	}

	private void validateSkill(int level, String name) {
		if (level < 0 || level > 5) {
			throw new ClientException(name + " skill level must be between 0 and 5");
		}
	}

	@Override
	public void routing(HttpRules rules) {
		var path = Objects.requireNonNull(IndustryCostHandler.class.getAnnotation(Path.class))
				.value();
		rules.get(path, this);
		rules.any(StandardHandlers.HTTP_METHOD_NOT_ALLOWED);
	}
}
