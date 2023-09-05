<script setup lang="ts">
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

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
	<div :class="'skill-' + indent">
		<template v-if="!inventoryType">
			Unknown skill [{{skillTypeId}}]
		</template>
		<template v-else>
			<TypeLink :type-id="inventoryType.typeId" />
		</template>
	</div>
	<div>(x{{skill.trainingTimeMultiplier}})</div>
	<div>
		[{{level}}]
		<span class="space-x-1">
			<span v-for="l in level" :key="l"><font-awesome-icon icon="fa-solid fa-square" /></span>
			<span v-for="l in (5 - level)" :key="l"><font-awesome-icon icon="fa-regular fa-square" /></span>
		</span>
	</div>

	<template v-if="skill.requiredSkills">
		<RequiredSkillsRow
			v-for="(level, skillTypeId) in skill.requiredSkills"
			:key="skillTypeId"
			:skill-type-id="parseInt(skillTypeId)"
			:level=level
			:indent="1 + 1 * indent"
		/>
	</template>
</template>

<style scoped>
.skill-1 {
  @apply pl-[1rem];
}
.skill-2 {
  @apply pl-[2rem];
}
.skill-3 {
  @apply pl-[3rem];
}
.skill-4 {
  @apply pl-[4rem];
}
.skill-5 {
  @apply pl-[5rem];
}
.skill-6 {
  @apply pl-[6rem];
}
</style>
