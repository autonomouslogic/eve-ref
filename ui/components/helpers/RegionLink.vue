<script setup lang="ts">
import refdataApi from "~/refdata";
import {type Region} from "~/refdata-openapi";
import {tr} from "~/lib/translate";
import InternalLink from "~/components/helpers/InternalLink.vue";

const props = defineProps<{
	regionId: number | undefined
}>();

const {locale} = useI18n();

if (props.regionId === undefined) {
	throw new Error("regionId is required");
}

const region: Region = await refdataApi.getRegion({regionId: props.regionId});
</script>

<template>
	<InternalLink
		v-if="region && region.name"
		:to="`/regions/${props.regionId}`">
		{{ tr(region.name, locale) }}
	</InternalLink>
	<span v-else>(Unknown region ID {{props.regionId}})</span>
</template>
