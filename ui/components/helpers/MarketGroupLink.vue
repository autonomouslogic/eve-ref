<script setup lang="ts">
import refdataApi from "~/refdata";
import {MarketGroup} from "~/refdata-openapi";

const props = defineProps<{
	marketGroupId: number | undefined
}>();

const {locale} = useI18n();

const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId: props.marketGroupId});
</script>

<template>
	<NuxtLink
		v-if="marketGroup"
		:to="`/market-groups/${props.marketGroupId}`">
		{{ marketGroup.name[locale] }}
	</NuxtLink>
</template>
