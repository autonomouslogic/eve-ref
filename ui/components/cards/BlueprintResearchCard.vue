<script setup lang="ts">
import {type Blueprint, type BlueprintActivity, type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdataApi from "~/refdata";
import BlueprintManufacturingLinks from "~/components/cards/BlueprintManufacturingLinks.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {secondsToMilliseconds} from "~/lib/timeUtils";
import {researchTimeForLevel} from "~/lib/blueprintResearchUtil";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const blueprintType = props.inventoryType.isBlueprint ? props.inventoryType : undefined;
const blueprint = !blueprintType || !blueprintType?.typeId ? undefined :
	await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId});
const meTime = blueprint?.activities?.research_material?.time;
const teTime = blueprint?.activities?.research_time?.time;
</script>

<template>
	<template v-if="meTime !== undefined || teTime !== undefined">
		<CardWrapper :title="title">

			<template v-if="meTime !== undefined">
				<h3>Material Efficiency</h3>
				<table class="w-full">
					<thead>
						<th class="text-left">Level</th>
						<th class="text-right">Time</th>
					</thead>
					<tr v-for="level in 10" :key="level">
						<td class="text-left">{{level}}</td>
						<td class="text-right"><Duration :milliseconds="researchTimeForLevel(level, meTime) * 1000" /></td>
					</tr>
				</table>
			</template>

			<template v-if="teTime !== undefined">
				<h3>Time Efficiency</h3>
				<table class="w-full">
					<thead>
						<th class="text-left">Level</th>
						<th class="text-right">Time</th>
					</thead>
					<tr v-for="level in 10" :key="level">
						<td class="text-left">{{level * 2}}</td>
						<td class="text-right"><Duration :milliseconds="researchTimeForLevel(level, teTime) * 1000" /></td>
					</tr>
				</table>
			</template>

			<div class="mt-2 italic">Basic research time, not accounting for skills and other bonuses.</div>

		</CardWrapper>
	</template>
</template>
