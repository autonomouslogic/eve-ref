package com.autonomouslogic.everef.api;

import com.autonomouslogic.everef.data.LoadedRefData;
import com.autonomouslogic.everef.esi.UniverseEsi;
import com.autonomouslogic.everef.industry.IndustryCalculator;
import com.autonomouslogic.everef.industry.IndustryDecryptors;
import com.autonomouslogic.everef.industry.IndustryRigs;
import com.autonomouslogic.everef.industry.IndustryStructures;
import com.autonomouslogic.everef.model.IndustryRig;
import com.autonomouslogic.everef.model.api.ApiError;
import com.autonomouslogic.everef.model.api.IndustryCost;
import com.autonomouslogic.everef.model.api.IndustryCostInput;
import com.autonomouslogic.everef.model.api.SystemSecurity;
import com.autonomouslogic.everef.openapi.esi.invoker.ApiException;
import com.autonomouslogic.everef.openapi.esi.model.GetUniverseSystemsSystemIdOk;
import com.autonomouslogic.everef.refdata.Blueprint;
import com.autonomouslogic.everef.refdata.InventoryType;
import com.autonomouslogic.everef.service.RefDataService;
import com.autonomouslogic.everef.service.SystemCostIndexService;
import com.autonomouslogic.everef.util.MathUtil;
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
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Tag(name = "industry")
@Path("/v1/industry/cost")
@Log4j2
public class IndustryCostHandler implements HttpService, Handler {
	@Inject
	protected ObjectMapper objectMapper;

	@Inject
	protected RefDataService refDataService;

	@Inject
	protected Provider<IndustryCalculator> industryCostCalculatorProvider;

	@Inject
	protected IndustryDecryptors industryDecryptors;

	@Inject
	protected IndustryStructures industryStructures;

	@Inject
	protected IndustryRigs industryRigs;

	@Inject
	protected UniverseEsi universeEsi;

	@Inject
	protected SystemCostIndexService systemCostIndexService;

	@Inject
	protected ApiUtil apiUtil;

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
		var calculator = industryCostCalculatorProvider.get().setRefData(refDataService.getLoadedRefData());
		var refdata = Objects.requireNonNull(refDataService.getLoadedRefData(), "refdata");
		var productType = handleProduct(input, refdata, calculator);
		var blueprint = handleBlueprint(input, productType, refdata, calculator);
		if (blueprint == null) {
			handleBlueprintForProduct(input, productType, refdata, calculator);
		}
		handleDecryptor(input, calculator);
		handleStructure(input, calculator);
		handleRigs(input, calculator);
		input = handleSystem(input);
		return calculator.setIndustryCostInput(input).calc().toBuilder()
				.input(input)
				.build();
	}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		log.info(
				"Received request: {}",
				URLEncoder.encode(req.requestedUri().toUri().toString(), StandardCharsets.UTF_8));
		var input = createInput(req);
		input = handleDefaults(input);
		validateInput(input);
		var result = industryCost(input);
		var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
		res.status(Status.OK_200);
		apiUtil.setStandardHeaders(res, Duration.ofSeconds(10), "https://docs.everef.net/api/industry-cost.html");
		res.send(json);
	}

	private IndustryCostInput createInput(ServerRequest req) {
		var query = req.query();
		var input = new HashMap<String, Object>();
		for (var name : query.names()) {
			var values = query.all(name).stream().filter(v -> !v.isEmpty()).toList();
			if (values.isEmpty()) {
				// Noop.
			} else if (values.size() == 1) {
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

	private IndustryCostInput handleDefaults(IndustryCostInput input) {
		var builder = input.toBuilder();
		if (input.getSystemId() == null) {
			if (input.getSecurity() == null) {
				builder.security(SystemSecurity.HIGH_SEC);
			}
			if (input.getManufacturingCost() == null) {
				builder.manufacturingCost(BigDecimal.ZERO);
			}
			if (input.getResearchingTeCost() == null) {
				builder.researchingTeCost(BigDecimal.ZERO);
			}
			if (input.getResearchingMeCost() == null) {
				builder.researchingMeCost(BigDecimal.ZERO);
			}
			if (input.getCopyingCost() == null) {
				builder.copyingCost(BigDecimal.ZERO);
			}
			if (input.getInventionCost() == null) {
				builder.inventionCost(BigDecimal.ZERO);
			}
			if (input.getReactionCost() == null) {
				builder.reactionCost(BigDecimal.ZERO);
			}
		}
		return builder.build();
	}

	private void validateInput(IndustryCostInput input) {
		if (input.getProductId() == null && input.getBlueprintId() == null) {
			throw new ClientException("Either a product ID or a blueprint ID must be supplied");
		}
		if (input.getProductId() != null && input.getProductId() < 1) {
			throw new ClientException("Product ID must be positive");
		}
		if (input.getBlueprintId() != null && input.getBlueprintId() < 1) {
			throw new ClientException("Blueprint ID must be positive");
		}
		if (input.getRuns() < 1) {
			throw new ClientException("Runs must be at least 1");
		}
		if (input.getSystemId() != null && input.getSecurity() != null) {
			throw new ClientException("System ID and security cannot be provided at the same time");
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
		rules.get(this);
	}

	private static InventoryType handleProduct(
			IndustryCostInput input, LoadedRefData refdata, IndustryCalculator calculator) {
		if (input.getProductId() == null) {
			return null;
		}
		var productType = refdata.getType(input.getProductId());
		if (productType == null) {
			throw new ClientException(String.format("Product type ID %d not found", input.getProductId()));
		}
		calculator.setProductType(productType);
		return productType;
	}

	private static Blueprint handleBlueprint(
			IndustryCostInput input, InventoryType productType, LoadedRefData refdata, IndustryCalculator calculator) {
		if (input.getBlueprintId() == null) {
			return null;
		}
		var blueprint = refdata.getBlueprint(input.getBlueprintId());
		if (blueprint == null) {
			throw new ClientException(String.format("Blueprint type ID %d not found", input.getBlueprintId()));
		}
		calculator.setBlueprint(blueprint);
		if (productType != null) {
			if (!productType.getProducedByBlueprints().containsKey(blueprint.getBlueprintTypeId())) {
				throw new ClientException(String.format(
						"Product type ID %d is not produced from blueprint ID %d",
						input.getProductId(), blueprint.getBlueprintTypeId()));
			}
		}
		return blueprint;
	}

	private void handleBlueprintForProduct(
			IndustryCostInput input, InventoryType productType, LoadedRefData refdata, IndustryCalculator calculator) {
		if (productType == null) {
			return;
		}
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
		if (!activity.equals("manufacturing") && !activity.equals("reaction") && !activity.equals("invention")) {
			throw new ClientException(
					String.format("Only manufacturing, reaction, and invention are supported, %s seen", activity));
		}
		var blueprintTypeId =
				Optional.ofNullable(producingBlueprint.getBlueprintTypeId()).orElseThrow();
		var blueprint = refdata.getBlueprint(blueprintTypeId);
		if (blueprint == null) {
			throw new ClientException(String.format("Blueprint ID %d not found", blueprintTypeId));
		}
		calculator.setBlueprint(blueprint);
	}

	private void handleDecryptor(IndustryCostInput input, IndustryCalculator calculator) {
		var decryptorId = input.getDecryptorId();
		if (decryptorId != null) {
			var decryptor = industryDecryptors.get(decryptorId);
			if (decryptor == null) {
				throw new ClientException(String.format("Decryptor ID %d not found", decryptorId));
			}
			calculator.setDecryptor(decryptor);
		}
	}

	private void handleStructure(IndustryCostInput input, IndustryCalculator calculator) {
		var structureTypeId = input.getStructureTypeId();
		if (structureTypeId != null) {
			var structure = industryStructures.get(structureTypeId);
			if (structure == null) {
				throw new ClientException(String.format("Structure ID %d not found", structureTypeId));
			}
			calculator.setStructure(structure);
		}
	}

	private void handleRigs(IndustryCostInput input, IndustryCalculator calculator) {
		var rigIds = input.getRigId();
		if (rigIds != null && !rigIds.isEmpty()) {
			var rigs = new ArrayList<IndustryRig>();
			for (var rigId : rigIds) {
				var rig = industryRigs.get(rigId);
				if (rig == null) {
					throw new ClientException(String.format("Rig ID %d not found", rigId));
				}
				rigs.add(rig);
			}
			calculator.setRigs(rigs);
		}
	}

	private IndustryCostInput handleSystem(IndustryCostInput input) {
		var systemId = input.getSystemId();
		if (systemId == null) {
			return input;
		}
		GetUniverseSystemsSystemIdOk system;
		try {
			system = universeEsi.getSystem(systemId).blockingGet();
		} catch (Exception e) {
			var root = ExceptionUtils.getRootCause(e);
			if (root instanceof ApiException apiException) {
				if (apiException.getCode() == 404) {
					throw new ClientException(String.format("System ID %d not found", systemId));
				}
			}
			throw e;
		}
		var builder = input.toBuilder();
		if (input.getSecurity() == null) {
			builder.security(SystemSecurity.forStatus(system.getSecurityStatus()));
		}
		var cost = systemCostIndexService.getSystem(systemId);
		if (cost == null) {
			throw new ClientException(String.format("System ID %d not found", systemId));
		}
		if (input.getManufacturingCost() == null) {
			builder.manufacturingCost(MathUtil.round(BigDecimal.valueOf(cost.getManufacturing()), 4));
		}
		if (input.getResearchingTeCost() == null) {
			builder.researchingTeCost(MathUtil.round(BigDecimal.valueOf(cost.getResearchingTimeEfficiency()), 4));
		}
		if (input.getResearchingMeCost() == null) {
			builder.researchingMeCost(MathUtil.round(BigDecimal.valueOf(cost.getResearchingMaterialEfficiency()), 4));
		}
		if (input.getCopyingCost() == null) {
			builder.copyingCost(MathUtil.round(BigDecimal.valueOf(cost.getCopying()), 4));
		}
		if (input.getInventionCost() == null) {
			builder.inventionCost(MathUtil.round(BigDecimal.valueOf(cost.getInvention()), 4));
		}
		if (input.getReactionCost() == null) {
			builder.reactionCost(MathUtil.round(BigDecimal.valueOf(cost.getReaction()), 4));
		}
		return builder.build();
	}
}
