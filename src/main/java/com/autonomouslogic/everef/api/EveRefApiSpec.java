package com.autonomouslogic.everef.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
		info =
				@Info(
						title = "EVE Ref API",
						description = "This spec should be considered unstable and subject to change at any time.",
						license =
								@License(
										name = "CCP",
										url = "https://github.com/autonomouslogic/eve-ref/blob/main/LICENSE-CCP"),
						version = "dev",
						contact = @Contact(name = "Kenn", url = "https://everef.net/discord")),
		servers = @Server(url = EveRefApiSpec.BASE_URL))
public interface EveRefApiSpec {
	String BASE_URL = "https://api.everef.net";
}
