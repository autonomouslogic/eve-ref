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
	<ul>
		<li><InternalLink to="/structures/engineering-complexes"><MarketGroupName :market-group-id="2324" /></InternalLink></li>
		<li><InternalLink to="/structures/refineries"><MarketGroupName :market-group-id="2327" /></InternalLink></li>
		<li><InternalLink to="/structures/standard-citadels"><MarketGroupName :market-group-id="2201" /></InternalLink></li>
		<li><InternalLink to="/structures/faction-citadels"><MarketGroupName :market-group-id="2200" /></InternalLink></li>
	</ul>
	<h2>Rigs</h2>
	<ul>
		<li><InternalLink to="/structures/engineering-rigs"><MarketGroupName :market-group-id="2340" /></InternalLink></li>
	</ul>
</template>
