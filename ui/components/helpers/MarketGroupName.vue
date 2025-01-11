<script setup lang="ts">
import refdataApi from "~/refdata";
import {type MarketGroup} from "~/refdata-openapi";
import {tr} from "~/lib/translate";

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
		{{ tr(marketGroup.name, locale) }}
	</template>
</template>
