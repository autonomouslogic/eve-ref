<script setup lang="ts">
import {STRUCTURE_RIGS_COMPARISON_ENGINEERING_DOGMA_NAMES} from "~/lib/structureConstants";
import {getRecursiveMarketGroupTypeIds} from "~/lib/marketGroupUtils";

const title = "Structure Engineering Rigs";
useHead({
	title: title
});
useSeoMeta({
	ogDescription: "Engineering rigs overview."
});

let typeIds = await getRecursiveMarketGroupTypeIds(2340);
console.log("typeIds:", typeIds);

const attrNames = STRUCTURE_RIGS_COMPARISON_ENGINEERING_DOGMA_NAMES;
</script>

<template>
	<h1>
		{{ title}}
	</h1>
	<CompareTable :type-ids="typeIds" :dogma-attribute-names="attrNames" direction="vertical" />
</template>
