<script setup lang="ts">
import {DogmaAttribute, InventoryType, Skill} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import DogmaListItems from "~/components/cards/DogmaListItems.vue";
import refdataApi from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import RequiredSkillsRow from "~/components/cards/requiredSkills/RequiredSkillsRow.vue";

const {locale} = useI18n();

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

// const skills: { [key: number]: Skill; } = {};
//
// async function populateSkill(id: number) {
//   if (skills[id]) {
//     return;
//   }
//   const skill = await refdataApi.getSkill({skillTypeId: id});
//   skills[id] = skill;
//   if (skill.requiredSkills) {
//     for (const id in skill.requiredSkills) {
//       await populateSkill(parseInt(id));
//     }
//   }
// }
//
// if (props.inventoryType.requiredSkills) {
//   for (const id in props.inventoryType.requiredSkills) {
//     await populateSkill(parseInt(id));
//   }
//   console.log(skills);
// }

// const metaGroups = {};
// if (props.inventoryType.typeVariations) {
// 	var promises = [];
// 	for (const metaGroupId of Object.keys(props.inventoryType.typeVariations)) {
// 		promises.push((async () => {
// 			var group = await refdataApi.getMetaGroup({metaGroupId: parseInt(metaGroupId)});
// 			metaGroups[metaGroupId] = group;
// 		})());
// 	}
// 	await Promise.all(promises);
// }

</script>

<template>
	<template v-if="inventoryType.requiredSkills">
		<CardWrapper :title="title">
			<RequiredSkillsRow
				v-for="(level, skillTypeId) in inventoryType.requiredSkills"
				:key="skillTypeId"
				:skill-type-id="parseInt(skillTypeId)"
				:level=level
				indent="0"
			/>
		</CardWrapper>
	</template>
</template>
