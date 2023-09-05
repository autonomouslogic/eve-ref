<script setup lang="ts">
import {Unit} from "~/refdata-openapi";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	value: number,
	unit: Unit
}>();

const ignoreSuffixUnitIds = [
	140, // Level
];
const noDecimalUnitIds = [
	1, // m
	2, // kg
	140, // Level
];
const twoDecimalUnitIds = [
	9, // m3
	133, // ISK
];

const displayName = computed(() => props.unit.displayName);
const unitId = computed(() => props.unit.unitId);

const spacer = computed(() => {
  if (displayName.value) {
    return displayName.value.length > 0 ? " " : "";
  }
  return "";
});

const displayUnit = computed(() => {
  if (unitId.value === undefined) {
    return false;
  }
  return props.unit?.displayName && !ignoreSuffixUnitIds.includes(unitId.value);
});

const decimals = computed(() => {
  if (unitId.value === undefined) {
    return 0;
  }
	if (noDecimalUnitIds.includes(unitId.value)) {
		return 0;
	}
	if (twoDecimalUnitIds.includes(unitId.value)) {
		return 2;
	}
	return 0;
});
const formatNumber = computed(() => {
  if (unitId.value === undefined) {
    return 0;
  }
  return noDecimalUnitIds.includes(unitId.value) || twoDecimalUnitIds.includes(unitId.value);
});
</script>

<template>
	<template v-if="formatNumber">
		<FormattedNumber :number="value" :decimals="decimals" />
	</template>
	<template v-else>{{ value }}</template>
	<template v-if="displayUnit">
		{{spacer}}
		<template v-if="unit.displayName == 'm2'">m<sup>2</sup></template>
		<template v-if="unit.displayName == 'm3'">m<sup>3</sup></template>
		<template v-else>{{unit.displayName}}</template>
	</template>
</template>
