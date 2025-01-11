<script setup lang="ts">
import {type Blueprint} from "~/refdata-openapi";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const props = defineProps<{
	blueprint: Blueprint,
}>();

const links = {
	"Adam4EVE": "https://www.adam4eve.eu/manu_calc.php?typeID=",
	"EVE Cookbook": "https://evecookbook.com/?share=1&blueprintTypeId=",
	"EVE Tycoon": "https://evetycoon.com/industryCalculator/",
	"Fuzzwork": "https://www.fuzzwork.co.uk/blueprint/?typeid="
};

</script>

<template>
	<div v-if="blueprint" class="mt-2">
		View on:
		<ul>
			<li v-for="(link, name) in links" :key="name">
				<ExternalLink :url="`${link}${blueprint.blueprintTypeId}`">{{name}}</ExternalLink>
			</li>
		</ul>
	</div>
</template>
