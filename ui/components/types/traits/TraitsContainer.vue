<script setup lang="ts">
import {type InventoryTypeTraits} from "~/refdata-openapi";
import TraitGroup from "~/components/types/traits/TraitGroup.vue";
import TypeTraits from "~/components/types/traits/TypeTraits.vue";

const props = defineProps<{
	traits: InventoryTypeTraits | undefined
}>();
</script>

<template>
	<div class="flex gap-4 flex-col md:flex-row">
		<TypeTraits :bonuses="props.traits?.types"/>
		<TraitGroup title="Role Bonuses" :bonuses="props.traits?.roleBonuses"/>
		<TraitGroup title="Misc Bonuses" :bonuses="props.traits?.miscBonuses"/>
	</div>
</template>
