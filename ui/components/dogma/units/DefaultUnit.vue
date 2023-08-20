<script setup lang="ts">
import refdataApi from "~/refdata";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {DogmaAttribute, Unit} from "~/refdata-openapi";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";

const props = defineProps<{
	value: string | number | undefined,
	unit: Unit | undefined
}>();

const {locale} = useI18n();

const ignoreSuffixUnitIds = [
	140 // Level
];
const noDecimalUnitIds = [
	9, // m3
	2, // kg
	1, // m
];
const twoDecimalUnitIds = [
	133, // ISK
];

const spacer = computed(() => props.unit?.displayName?.length > 0 ? " " : "");
const displayUnit = computed(() => props.unit?.displayName && !ignoreSuffixUnitIds.includes(props.unit?.unitId));
const decimals = computed(() => {
	if (noDecimalUnitIds.includes(props.unit?.unitId)) {
		return 0;
	}
	if (twoDecimalUnitIds.includes(props.unit?.unitId)) {
		return 2;
	}
	return 0;
});
const formatNumber = computed(() => noDecimalUnitIds.includes(props.unit?.unitId) || twoDecimalUnitIds.includes(props.unit?.unitId));

</script>

<template>
	<template v-if="formatNumber">
		<FormattedNumber :number="value" :decimals="decimals" />
	</template>
	<template v-else>{{ value }}</template>
	<template v-if="displayUnit">{{ spacer }}{{ unit.displayName }}</template>
</template>
