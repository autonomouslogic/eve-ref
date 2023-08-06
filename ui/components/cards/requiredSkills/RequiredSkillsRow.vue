<script setup lang="ts">
import {DogmaAttribute, InventoryType, Skill} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";

const {locale} = useI18n();

const props = defineProps<{
	skillTypeId: number,
	level: number,
	indent: number
}>();
const inventoryType = await refdataApi.getType({typeId: props.skillTypeId});
const skill = await refdataApi.getSkill({skillTypeId: props.skillTypeId});

</script>

<template>
	<div v-if="!inventoryType">Unknown skill {{skillTypeId}}</div>
	<div v-else>{{indent}} {{inventoryType.name[locale]}} - {{level}}</div>

	<template v-if="skill.requiredSkills">
		<RequiredSkillsRow
			v-for="(level, skillTypeId) in skill.requiredSkills"
			:key="skillTypeId"
			:skill-type-id="parseInt(skillTypeId)"
			:level=level
			:indent="parseInt(indent) + 1"
		/>
	</template>
</template>
