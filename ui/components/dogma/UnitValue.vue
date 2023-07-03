<script setup lang="ts">
import refdataApi from "~/refdata";

const props = defineProps<{
	value: string | number | undefined,
	unitId: number | undefined
}>();

const {locale} = useI18n();

const unit = await refdataApi.getUnit({unitId: props.unitId});
</script>

<template>
	<template v-if="unit">
		{{ props.value }} {{ unit.displayName }}
	</template>
	<span v-else>(Unknown unit ID {{props.unitId}})</span>
</template>
