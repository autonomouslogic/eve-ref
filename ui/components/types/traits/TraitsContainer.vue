<script setup lang="ts">
import {InventoryTypeTraits} from "~/refdata-openapi";
import TraitGroup from "~/components/types/traits/TraitGroup.vue";
import TypeTraits from "~/components/types/traits/TypeTraits.vue";

const props = defineProps<{
	traits: InventoryTypeTraits | undefined
}>();

const {locale} = useI18n();
</script>

<template>
	<div>
		<TypeTraits :bonuses="props.traits?.types"/>
		<TraitGroup title="Role Bonuses" :bonuses="props.traits?.roleBonuses"/>
		<TraitGroup title="Misc Bonuses" :bonuses="props.traits?.miscBonuses"/>
	</div>
</template>
