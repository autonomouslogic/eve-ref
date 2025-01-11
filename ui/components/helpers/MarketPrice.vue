<script setup lang="ts">
import MarketPriceInner from "~/components/helpers/MarketPriceInner.vue";

interface Props {
	typeId: number,
	orderType?: "sell" | "buy"
	regionId?: number,
	stationId?: number
}
const props = withDefaults(defineProps<Props>(), {});
</script>

<template>
	<ClientOnly>
		<MarketPriceInner :type-id="typeId" :order-type="orderType" :region-id="regionId" :station-id="stationId" />
	</ClientOnly>
</template>
