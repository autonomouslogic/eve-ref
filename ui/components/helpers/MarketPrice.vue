<script setup lang="ts">
import {GetMarketsRegionIdOrdersOrderTypeEnum,} from "~/esi-openapi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import {getOrders, HUB_STATIONS} from "~/lib/marketUtils";
import {MONEY} from "~/lib/unitConstants";
import MarketCardInner from "~/components/cards/market/MarketCardInner.vue";
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
