<script setup lang="ts">
import refdataApi from "~/refdata";
import {tr} from "~/lib/translate";

const props = defineProps<{
	metaGroupId: number | undefined
}>();

const {locale} = useI18n();
const metaGroup = props.metaGroupId === undefined ? undefined :
	await refdataApi.getMetaGroup({metaGroupId: props.metaGroupId});
</script>

<template>
	<template v-if="metaGroup && metaGroup.name">
		{{ tr(metaGroup.name, locale) }}
	</template>
</template>
