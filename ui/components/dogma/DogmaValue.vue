<script setup lang="ts">
import refdataApi from "~/refdata";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {DogmaAttribute} from "~/refdata-openapi";

const props = defineProps<{
	value: string | number | undefined,
	attribute: DogmaAttribute
}>();

const {locale} = useI18n();
</script>

<template>
	<template v-if="attribute">
		<UnitValue v-if="attribute.unitId" :value="value" :unit-id="attribute.unitId" />
		<span v-else>{{ props.value }}</span>
	</template>
	<span v-else>(Unknown dogma attribute ID {{attribute.attributeId}})</span>
</template>
