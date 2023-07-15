<script setup lang="ts">
import refdataApi from "~/refdata";

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
		{{ props.value }}{{ spacer }}{{ unit.displayName }}
	</template>
	<span v-else>(Unknown unit ID {{props.unitId}})</span>
</template>
