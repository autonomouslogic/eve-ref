<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import Duration from "~/components/dogma/units/Duration.vue";
import AttributeList from "~/components/attr/AttributeList.vue";
import AttributeListItem from "~/components/attr/AttributeListItem.vue";
import RequiredSkillsRow from "~/components/cards/requiredSkills/RequiredSkillsRow.vue";
import AbsolutePercent from "~/components/dogma/units/AbsolutePercent.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const blueprintType = props.inventoryType.isBlueprint ? props.inventoryType : undefined;
const blueprint = blueprintType?.typeId ? await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId}) : undefined;
const invention = blueprint?.activities?.invention;

</script>

<template>
	<template v-if="invention">
		<CardWrapper :title="title">
			<AttributeList v-if="invention.time">
				<AttributeListItem>
					<template v-slot:key>Copy time:</template>
					<Duration :milliseconds="invention.time * 1000" />
				</AttributeListItem>
			</AttributeList>

			<template v-if="invention.products">
				<h3>Output</h3>
				<table class="w-full">
					<thead>
						<th class="text-left"></th>
						<th class="text-right">Probability</th>
						<th class="text-right">Quantity</th>
					</thead>
					<tr v-for="(product, index) in invention.products" :key="index">
						<td class="text-left"><TypeLink :type-id="product.typeId" /></td>
						<td class="text-right"><AbsolutePercent v-if="product.probability" :value="product.probability" /></td>
						<td class="text-right"><FormattedNumber :number="product.quantity" /></td>
					</tr>
				</table>
			</template>

			<h3>Input</h3>
			<table class="w-full">
				<thead>
					<th class="text-left"></th>
					<th class="text-right">Quantity</th>
				</thead>
				<tr v-for="(material, index) in invention.materials" :key="index">
					<td class="text-left"><TypeLink :type-id="material.typeId" /></td>
					<td class="text-right"><FormattedNumber :number="material.quantity" /></td>
				</tr>
			</table>

			<template v-if="invention.requiredSkills">
				<h3>Required Skills</h3>
				<div class="grid grid-cols-3">
					<RequiredSkillsRow
						v-for="(level, requiredSkillTypeId) in invention.requiredSkills"
						:key="requiredSkillTypeId"
						:skill-type-id="parseInt(`${requiredSkillTypeId}`)"
						:level=level
						:indent="0"
						:shown-skills="[]"
					/>
				</div>
			</template>
		</CardWrapper>
	</template>
</template>
