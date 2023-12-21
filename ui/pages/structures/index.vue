<script setup lang="ts">
import refdataApi, {cacheBundle} from "~/refdata";
import {DogmaAttribute, DogmaTypeAttribute, InventoryGroup, InventoryType} from "~/refdata-openapi";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import MarketGroupBreadcrumbs from "~/components/helpers/MarketGroupBreadcrumbs.vue";
import TypeCards from "~/components/types/TypeCards.vue";
import LinkParser from "~/components/helpers/LinkParser.vue";
import {getAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import MarketGroupName from "~/components/helpers/MarketGroupName.vue";
import {STRUCTURE_COMPARISON_STRUCTURE_NAMES} from "~/lib/structureConstants";
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
