<script setup lang="ts">
import refdataApi from "~/refdata";
import {MarketGroup} from "~/refdata-openapi";

const props = defineProps<{
	marketGroupId: number | undefined
}>();

const {locale} = useI18n();

if (props.marketGroupId === undefined) {
	throw new Error("marketGroupId is required");
}

const marketGroup: MarketGroup = await refdataApi.getMarketGroup({marketGroupId: props.marketGroupId});
</script>

<template>
	<template v-if="marketGroup && marketGroup.name">
		{{ marketGroup.name[locale] }}
	</template>
</template>
