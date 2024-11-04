<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import MarketGroupLink from "~/components/helpers/MarketGroupLink.vue";
import UnitValue from "~/components/dogma/UnitValue.vue";
import GroupLink from "~/components/helpers/GroupLink.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import {CUBIC_METER, METER, MONEY} from "~/lib/unitConstants";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import DogmaAttributeLink from "~/components/helpers/DogmaAttributeLink.vue";
import AttributeTypeIcon from "~/components/icons/AttributeTypeIcon.vue";
import CategoryLink from "~/components/helpers/CategoryLink.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

var fields = computed(() => {
	const type = props.inventoryType;
	var n = props.dogmaAttributes.length;
	if (type.engineeringRigAffectedCategoryIds) {
		n++;
	}
	if (type.engineeringRigAffectedGroupIds) {
		n++;
	}
	if (type.engineeringRigAffectedGroupIds) {
		n++;
	}
	return n;
});

</script>

<template>
	<CardWrapper title="Engineering Rig" v-if="fields > 0">

		<AttributeList :elements="dogmaAttributes.length">
			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

			<AttributeListItem v-if="inventoryType.engineeringRigAffectedCategoryIds">
				<template v-slot:key>
					Affected categories:
				</template>
				<div v-for="categoryId in inventoryType.engineeringRigAffectedCategoryIds" :key="categoryId">
					<CategoryLink :category-id="categoryId" />
				</div>
			</AttributeListItem>

			<AttributeListItem v-if="inventoryType.engineeringRigAffectedGroupIds">
				<template v-slot:key>
					Affected groups:
				</template>
				<div v-for="groupId in inventoryType.engineeringRigAffectedGroupIds" :key="groupId">
					<GroupLink :group-id="groupId" />
				</div>
			</AttributeListItem>

			<AttributeListItem v-if="inventoryType.engineeringRigSourceTypeIds">
				<template v-slot:key>
					Rig types:
				</template>
				<div v-for="typeId in inventoryType.engineeringRigSourceTypeIds" :key="typeId">
					<TypeLink :type-id="typeId" />
				</div>
			</AttributeListItem>

		</AttributeList>
	</CardWrapper>
</template>
