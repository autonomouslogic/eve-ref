<script setup lang="ts">
import {DogmaAttribute, InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import MarketRow from "~/components/cards/market/MarketRow.vue";
import MarketLinks from "~/components/cards/market/MarketLinks.vue";
import {insuranceApi} from "~/esi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import {getJitaSellPrice} from "~/lib/marketUtils";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const prices = (await insuranceApi.getInsurancePrices())
	.filter(e => e.typeId == props.inventoryType.typeId)
	.pop();
const marketPrice = !!props.inventoryType.typeId ? await getJitaSellPrice(props.inventoryType.typeId) : undefined;

</script>

<template>
	<CardWrapper :title="title">
		<table v-if="prices">
			<thead>
				<tr>
					<th>Level</th>
					<th>Cost</th>
					<th>Est. payout</th>
					<th v-if="marketPrice">Coverage</th>
					<th>Cost vs payout</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="level in prices.levels" :key="level.name">
					<td>{{ level.name }}</td>
					<td><UnitValue :unit-id="133" :value="level.cost" /></td>
					<td><UnitValue :unit-id="133" :value="level.payout" /></td>
					<td v-if="marketPrice"><FormattedNumber :number="level.payout / marketPrice * 100" :decimals="1" />%</td>
					<td><FormattedNumber :number="level.payout / level.cost" :decimals="1" />x</td>
				</tr>
			</tbody>
		</table>
	</CardWrapper>
</template>
