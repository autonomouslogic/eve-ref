<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";

const props = defineProps<{
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

function value(attributeId: number | undefined): number {
	if (!props.inventoryType.dogmaAttributes || !attributeId) {
		throw new Error(`No dogma attributes for inventory type ${props.inventoryType.typeId}`);
	}

	const attributeValue: number | undefined = props.inventoryType.dogmaAttributes[attributeId].value;
	if (attributeValue === undefined) {
		throw new Error(`No value for attribute ${attributeId} on inventory type ${props.inventoryType.typeId}`);
	}

	return attributeValue;
}
</script>

<template>
	<AttributeListItem v-for="attribute in dogmaAttributes" :key="attribute.attributeId">
		<template v-slot:key>
			<template v-if="attribute">
				<AttributeTypeIcon :dogma-attribute="attribute" :size="25" />
				<DogmaAttributeLink v-if="attribute.attributeId" :attribute="attribute.attributeId" />:
			</template>
		</template>
		<DogmaValue :value="value(attribute.attributeId)" :attribute="attribute" />
	</AttributeListItem>
</template>
