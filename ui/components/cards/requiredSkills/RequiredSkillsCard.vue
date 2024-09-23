<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import RequiredSkillsRow from "~/components/cards/requiredSkills/RequiredSkillsRow.vue";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="inventoryType.requiredSkills">
		<CardWrapper :title="title">
			<table class="standard-table">
				<thead>
					<th>Skill</th>
					<th>Multiplier</th>
					<th>Level</th>
				</thead>
				<tbody>
					<RequiredSkillsRow
						v-for="(level, requiredSkillTypeId) in inventoryType.requiredSkills"
						:key="requiredSkillTypeId"
						:skill-type-id="parseInt(`${requiredSkillTypeId}`)"
						:level=level
						:indent="0"
						:shown-skills="[]"
					/>
				</tbody>
			</table>
		</CardWrapper>
	</template>
</template>
