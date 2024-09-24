<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import refdataApi from "~/refdata";
import Duration from "~/components/dogma/units/Duration.vue";
import {researchTimeForLevel} from "~/lib/blueprintResearchUtil";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const blueprintType = props.inventoryType.isBlueprint ? props.inventoryType : undefined;
const blueprint = !blueprintType || !blueprintType?.typeId ? undefined :
	await refdataApi.getBlueprint({blueprintTypeId: blueprintType.typeId});
const teTime = blueprint?.activities?.research_time?.time;
</script>

<template>
	<template v-if="teTime !== undefined">
		<CardWrapper :title="title">

			<template v-if="teTime !== undefined">
				<table class="standard-table">
					<thead>
						<th>Time Efficiency</th>
						<th class="text-right">Total time</th>
					</thead>
					<tbody>
						<tr v-for="level in 10" :key="level">
							<td>{{level * 2}}</td>
							<td class="text-right"><Duration :milliseconds="researchTimeForLevel(level, teTime) * 1000" /></td>
						</tr>
					</tbody>
				</table>
			</template>

			<div class="mt-2 italic">Basic research time, not accounting for skills and other bonuses.</div>

		</CardWrapper>
	</template>
</template>
