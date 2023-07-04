<script setup lang="ts">
import {InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import FormattedCurrency from "~/components/helpers/FormattedCurrency.vue";

const props = defineProps<{
	inventoryType: InventoryType
}>();
</script>

<template>
	<CardWrapper title="Dogma" v-if="inventoryType.dogmaAttributes">
		<ul class="list-disc list-inside">
			<li v-for="(attributeValue, i) in inventoryType.dogmaAttributes" :key="i">
				<DogmaAttributeValue :value="attributeValue.value" :attribute-id="attributeValue.attributeId" />
			</li>
		</ul>
	</CardWrapper>
</template>
