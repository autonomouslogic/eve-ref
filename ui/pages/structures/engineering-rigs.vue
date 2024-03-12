<script setup lang="ts">
import refdataApi from "~/refdata";
import {
	STRUCTURE_RIGS_COMPARISON_ENGINEERING_DOGMA_NAMES
} from "~/lib/structureConstants";
import {getRecursiveMarketGroupTypeIds} from "~/lib/marketGroupUtils";

const {locale} = useI18n();

const title = "Structure Engineering Rigs";
useHead({
	title: title
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
