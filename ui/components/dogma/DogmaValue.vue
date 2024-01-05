<script setup lang="ts">
import UnitValue from "~/components/dogma/UnitValue.vue";
import {type DogmaAttribute} from "~/refdata-openapi";

const props = defineProps<{
	value: number,
	attribute: DogmaAttribute
}>();
</script>

<template>
	<template v-if="attribute">
		<UnitValue v-if="attribute.unitId" :value="parseFloat(`${value}`)" :unit-id="attribute.unitId" />
		<span v-else>{{ props.value }}</span>
	</template>
</template>
