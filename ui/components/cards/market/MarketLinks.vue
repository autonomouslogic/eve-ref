<script setup lang="ts">
import {InventoryType} from "~/refdata-openapi";
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const props = defineProps<{
	inventoryType: InventoryType,
}>();

const links = {
	"Adam4EVE": "https://www.adam4eve.eu/commodity.php?typeID=",
	"dev.eveonline.it": "https://evetycoon.com/market/",
	"EVE Tycoon": "https://evetycoon.com/market/",
};

</script>

<template>
	<div v-if="inventoryType">
		View on:
		<ul>
			<li v-for="(link, name) in links" :key="name">
				<ExternalLink :url="`${link}${inventoryType.typeId}`">{{name}}</ExternalLink>
			</li>
		</ul>
	</div>
</template>
