<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import RequiredSkillsRow from "~/components/cards/requiredSkills/RequiredSkillsRow.vue";

const {locale} = useI18n();

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();
</script>

<template>
	<template v-if="inventoryType.requiredSkills">
		<CardWrapper :title="title">
			<div class="grid grid-cols-3">
				<RequiredSkillsRow
					v-for="(level, skillTypeId) in inventoryType.requiredSkills"
					:key="skillTypeId"
					:skill-type-id="parseInt(skillTypeId)"
					:level=level
					:indent="0"
				/>
			</div>
		</CardWrapper>
	</template>
</template>
