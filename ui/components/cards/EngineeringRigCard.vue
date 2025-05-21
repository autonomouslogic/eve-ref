<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import EngineeringRigCardCategories from "~/components/cards/EngineeringRigCardCategories.vue";
import EngineeringRigCardGroups from "~/components/cards/EngineeringRigCardGroups.vue";
import EngineeringRigCardTypes from "~/components/cards/EngineeringRigCardTypes.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";

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
	if (type.engineeringRigSourceTypeIds) {
		n++;
	}
	return n;
});

</script>

<template>
	<CardWrapper title="Engineering Rig" v-if="fields > 0">

		<AttributeList :elements="dogmaAttributes.length">
			<DogmaListItems :inventory-type="inventoryType" :dogma-attributes="dogmaAttributes" />

			<EngineeringRigCardCategories name="time research" :categories="inventoryType.engineeringRigAffectedCategoryIds?.researchTime" />
			<EngineeringRigCardCategories name="material research" :categories="inventoryType.engineeringRigAffectedCategoryIds?.researchMaterial" />
			<EngineeringRigCardCategories name="manufacturing" :categories="inventoryType.engineeringRigAffectedCategoryIds?.manufacturing" />
			<EngineeringRigCardCategories name="reaction" :categories="inventoryType.engineeringRigAffectedCategoryIds?.reaction" />
			<EngineeringRigCardCategories name="invention" :categories="inventoryType.engineeringRigAffectedCategoryIds?.invention" />
			<EngineeringRigCardCategories name="copying" :categories="inventoryType.engineeringRigAffectedCategoryIds?.copying" />

			<EngineeringRigCardGroups name="time research" :groups="inventoryType.engineeringRigAffectedGroupIds?.researchTime" />
			<EngineeringRigCardGroups name="material research" :groups="inventoryType.engineeringRigAffectedGroupIds?.researchMaterial" />
			<EngineeringRigCardGroups name="manufacturing" :groups="inventoryType.engineeringRigAffectedGroupIds?.manufacturing" />
			<EngineeringRigCardGroups name="reaction" :groups="inventoryType.engineeringRigAffectedGroupIds?.reaction" />
			<EngineeringRigCardGroups name="invention" :groups="inventoryType.engineeringRigAffectedGroupIds?.invention" />
			<EngineeringRigCardGroups name="copying" :groups="inventoryType.engineeringRigAffectedGroupIds?.copying" />

			<EngineeringRigCardTypes name="time research" :types="inventoryType.engineeringRigSourceTypeIds?.researchTime" />
			<EngineeringRigCardTypes name="material research" :types="inventoryType.engineeringRigSourceTypeIds?.researchMaterial" />
			<EngineeringRigCardTypes name="manufacturing" :types="inventoryType.engineeringRigSourceTypeIds?.manufacturing" />
			<EngineeringRigCardTypes name="reaction" :types="inventoryType.engineeringRigSourceTypeIds?.reaction" />
			<EngineeringRigCardTypes name="invention" :types="inventoryType.engineeringRigSourceTypeIds?.invention" />
			<EngineeringRigCardTypes name="copying" :types="inventoryType.engineeringRigSourceTypeIds?.copying" />


			<AttributeListItem v-if="inventoryType.engineeringRigGlobalActivities">
				<template v-slot:key>
					Global activites:
				</template>
				<div v-for="activity in inventoryType.engineeringRigGlobalActivities" :key="activity">
					{{ activity }}
				</div>
			</AttributeListItem>

		</AttributeList>
	</CardWrapper>
</template>
