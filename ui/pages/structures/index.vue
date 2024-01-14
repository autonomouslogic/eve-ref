<script setup lang="ts">
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";

useHead({
	title: "Structures"
});
</script>

<template>
	<h1>
		<MarketGroupName :market-group-id="477" />
	</h1>
	<ul>
		<li><NuxtLink to="/structures/engineering-complexes"><MarketGroupName :market-group-id="2324" /></NuxtLink></li>
		<li><NuxtLink to="/structures/refineries"><MarketGroupName :market-group-id="2327" /></NuxtLink></li>
		<li><NuxtLink to="/structures/standard-citadels"><MarketGroupName :market-group-id="2201" /></NuxtLink></li>
		<li><NuxtLink to="/structures/faction-citadels"><MarketGroupName :market-group-id="2200" /></NuxtLink></li>
	</ul>
</template>
