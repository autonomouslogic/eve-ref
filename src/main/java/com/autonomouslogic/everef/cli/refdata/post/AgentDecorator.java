package com.autonomouslogic.everef.cli.refdata.post;

import io.reactivex.rxjava3.core.Completable;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.node.ObjectNode;

/**
 * Derives the <code>agents</code> ref store from <code>npc_characters</code>, since both are sourced from the same
 * SDE file (<code>npcCharacters.yaml</code>) and the loading pipeline only routes a given SDE file into a single
 * config. Filters down to characters with an <code>agent</code> block and flattens it onto the row.
 */
@Log4j2
public class AgentDecorator extends PostDecorator {
	@Inject
	protected AgentDecorator() {}

	public Completable create() {
		return Completable.fromAction(() -> {
			log.info("Deriving agents from NPC characters");
			var npcCharacters = storeHandler.getRefStore("npcCharacters");
			var agents = storeHandler.getRefStore("agents");
			for (var entry : npcCharacters.entrySet()) {
				var character = (ObjectNode) entry.getValue();
				var agentInfo = character.get("agent");
				if (agentInfo == null || !agentInfo.isObject()) {
					continue;
				}
				var agent = character.deepCopy();
				((ObjectNode) agentInfo).properties().forEach(field -> agent.set(field.getKey(), field.getValue()));
				agent.remove("agent");
				agent.remove("character_id");
				agent.remove("ceo");
				agent.remove("gender");
				agent.remove("unique_name");
				agent.put("agent_id", entry.getKey());
				agents.put(entry.getKey(), agent);
			}
		});
	}
}
