<script setup lang="ts">
import {type Unit} from "~/refdata-openapi";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import {
	CUBIC_METER,
	GIGAJOULE,
	HARDPOINTS,
	HITPOINTS,
	KILOGRAM,
	LEVEL,
	MEGAWATTS,
	METER, MONEY,
	POINTS, REALPERCENT,
	TERAFLOPS
} from "~/lib/unitConstants";

const props = defineProps<{
	value: number,
	unit: Unit
}>();

const noFormatUnitIds: number[] = [];
const ignoreSuffixUnitIds = [
	LEVEL,
	HARDPOINTS,
];
const noDecimalUnitIds = [
	METER,
	KILOGRAM,
	LEVEL,
	POINTS,
	TERAFLOPS,
	GIGAJOULE,
	HITPOINTS,
	MEGAWATTS,
];
const twoDecimalUnitIds = [
	MONEY,
	// REALPERCENT,
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
		return undefined;
	}
	if (noDecimalUnitIds.includes(unitId.value)) {
		return 0;
	}
	if (twoDecimalUnitIds.includes(unitId.value)) {
		return 2;
	}
	return undefined;
});
const minDecimals = computed(() => {
	return decimals.value;
});
const maxDecimals = computed(() => {
	return decimals.value;
});
const formatNumber = computed(() => {
	if (unitId.value === undefined) {
		return true;
	}
	// return noDecimalUnitIds.includes(unitId.value) || twoDecimalUnitIds.includes(unitId.value);
	return !noFormatUnitIds.includes(unitId.value);
});
</script>

<template>
	<template v-if="formatNumber">
		<FormattedNumber :number="value" :min-decimals="minDecimals" :max-decimals="maxDecimals" />
	</template>
	<template v-else>{{ value }}</template>
	<template v-if="displayUnit">
		{{spacer}}
		<template v-if="unit.displayName == 'm2'">m<sup>2</sup></template>
		<template v-if="unit.displayName == 'm3'">m<sup>3</sup></template>
		<template v-else>{{unit.displayName}}</template>
	</template>
</template>
