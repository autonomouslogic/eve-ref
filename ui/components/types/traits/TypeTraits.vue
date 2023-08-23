<script setup lang="ts">
import {TraitBonus, TraitBonusFromJSON} from "~/refdata-openapi";
import TypeLink from "~/components/helpers/TypeLink.vue";
import TraitGroup from "~/components/types/traits/TraitGroup.vue";

const props = defineProps<{
	bonuses: { [key: string]: { [key: string]: TraitBonus; }; } | undefined;
}>();

// TODO calling TraitBonusFromJSON is required because the generated API code doesn't do it.
const formattedBonuses: { [key: string]: { [key: string]: TraitBonus } } = {};

if (props.bonuses) {
	Object.keys(props.bonuses).forEach(typeId => {
    if (!props.bonuses) {
      throw new Error("props.bonuses is undefined");
    }

		const typeBonuses = props.bonuses[typeId];
		const formattedTypeBonuses: { [key: string]: TraitBonus } = {};

		Object.keys(typeBonuses).forEach(level => {
			formattedTypeBonuses[level] = TraitBonusFromJSON(typeBonuses[level]);
		});

		formattedBonuses[typeId] = formattedTypeBonuses;
	});
}
</script>

<template>
	<h3>Type Bonuses</h3>
	<div v-for="(formattedBonus, typeId) of formattedBonuses" :key="typeId">
		<TypeLink :type-id="typeId" /> bonuses per level
		<TraitGroup title="" :bonuses="formattedBonus" />
	</div>
</template>

<style scoped>

</style>
