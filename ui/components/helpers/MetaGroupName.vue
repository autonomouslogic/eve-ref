<script setup lang="ts">
import refdataApi from "~/refdata";
import {type MetaGroup} from "~/refdata-openapi";

const props = defineProps<{
	metaGroupId: number | undefined
}>();

const {locale} = useI18n();

if (props.metaGroupId === undefined) {
	throw new Error("metaGroupId is required");
}

const metaGroup: MetaGroup = await refdataApi.getMetaGroup({metaGroupId: props.metaGroupId});
</script>

<template>
	<template v-if="metaGroup && metaGroup.name">
		{{ metaGroup.name[locale] }}
	</template>
</template>
