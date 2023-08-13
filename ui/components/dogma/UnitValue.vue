<script setup lang="ts">
import refdataApi from "~/refdata";
import AttibuteId from "~/components/dogma/units/AttibuteId.vue";

const props = defineProps<{
	value: string | number,
	unitId: number
}>();

const {locale} = useI18n();

const unit = await refdataApi.getUnit({unitId: props.unitId});
const spacer = unit.displayName?.length > 1 ? " " : "";
</script>

<template>
	<template v-if="unit">
		<AttibuteId v-if="unit.displayName == 'attributeID'"
			:unit="unit"
			:value="value" />
		<template v-else>{{ props.value }}{{ spacer }}{{ unit.displayName }}</template>
	</template>
	<span v-else>{{ props.value }} (Unknown unit ID {{props.unitId}})</span>
</template>
