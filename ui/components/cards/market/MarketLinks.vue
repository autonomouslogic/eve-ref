<script setup lang="ts">
import {InventoryType} from "~/refdata-openapi";

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
				<NuxtLink
					:to="`${link}${inventoryType.typeId}`"
					rel="noopener"
					target="_blank">
					{{ name }}
				</NuxtLink>
			</li>
		</ul>
	</div>
</template>
