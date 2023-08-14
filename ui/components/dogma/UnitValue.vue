<script setup lang="ts">
import refdataApi from "~/refdata";
import AttibuteId from "~/components/dogma/units/AttibuteId.vue";
import CubicMeters from "~/components/dogma/units/CubicMeters.vue";
import Kilogram from "~/components/dogma/units/Kilogram.vue";
import Sizeclass from "~/components/dogma/units/Sizeclass.vue";
import Meters from "~/components/dogma/units/Meters.vue";

const props = defineProps<{
	value: number,
	unitId: number
}>();

const {locale} = useI18n();

const ignoreSuffixUnits = [
	"Level"
];

const unit = await refdataApi.getUnit({unitId: props.unitId});
const spacer = unit?.displayName?.length > 1 ? " " : "";
const displayUnit = unit?.displayName && !ignoreSuffixUnits.includes(unit?.displayName);
</script>

<template>
	<template v-if="unit">
		<AttibuteId v-if="unit.displayName == 'attributeID'" :value="value" />
		<CubicMeters v-else-if="unit.unitId == 9" :value="value" />
		<Kilogram v-else-if="unit.unitId == 2" :value="value" />
		<Sizeclass v-else-if="unit.unitId == 117" :value="value" />
		<Meters v-else-if="unit.unitId == 1" :value="value" />
		<template v-else>{{ props.value }}<template v-if="displayUnit">{{ spacer }}{{ unit.displayName }}</template></template>
	</template>
	<span v-else>{{ props.value }} (Unknown unit ID {{props.unitId}})</span>
</template>
