<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";

useHead({
	title: "Structures"
});
</script>

<template>
	<h1>
		<MarketGroupName :market-group-id="477" />
	</h1>
	<div class="flex flex-col">
		<InternalLink class="py-2" to="/structures/engineering-complexes"><MarketGroupName :market-group-id="2324" /></InternalLink>
		<InternalLink class="py-2" to="/structures/refineries"><MarketGroupName :market-group-id="2327" /></InternalLink>
		<InternalLink class="py-2" to="/structures/standard-citadels"><MarketGroupName :market-group-id="2201" /></InternalLink>
		<InternalLink class="py-2" to="/structures/faction-citadels"><MarketGroupName :market-group-id="2200" /></InternalLink>
	</div>
	<h2>Rigs</h2>
	<div class="flex flex-col">
		<InternalLink class="py-2" to="/structures/engineering-rigs"><MarketGroupName :market-group-id="2340" /></InternalLink>
	</div>
</template>
