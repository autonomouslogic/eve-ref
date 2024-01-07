<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
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
					v-for="(level, requiredSkillTypeId) in inventoryType.requiredSkills"
					:key="requiredSkillTypeId"
					:skill-type-id="parseInt(`${requiredSkillTypeId}`)"
					:level=level
					:indent="0"
					:shown-skills="[]"
				/>
			</div>
		</CardWrapper>
	</template>
</template>
