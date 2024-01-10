<script setup lang="ts">
import {type DogmaAttribute, type InventoryType} from "~/refdata-openapi";
import CardWrapper from "~/components/cards/CardWrapper.vue";
import {insuranceApi} from "~/esi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import {getJitaSellPrice} from "~/lib/marketUtils";
import {MONEY} from "~/lib/unitConstants";
import refdataApi from "~/refdata";
import {SHIP} from "~/lib/categoryConstants";

const props = defineProps<{
	title: string,
	inventoryType: InventoryType,
	dogmaAttributes: DogmaAttribute[]
}>();

const group = !props.inventoryType.groupId ? undefined :
	await refdataApi.getGroup({groupId: props.inventoryType.groupId});
const isShip = group?.categoryId == SHIP;

const prices = !isShip ? undefined :
	(await insuranceApi.getInsurancePrices())
		.filter(e => e.typeId == props.inventoryType.typeId)
		.pop();
const marketPrice = !isShip || !props.inventoryType.typeId ? undefined :
	await getJitaSellPrice(props.inventoryType.typeId);

</script>

<template>
	<CardWrapper :title="title" v-if="isShip">
		<table v-if="prices">
			<thead>
				<tr>
					<th>Level</th>
					<th>Cost</th>
					<th>Est. payout</th>
					<th v-if="marketPrice">Coverage</th>
					<th>Payout over cost</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="level in prices.levels" :key="level.name">
					<td>{{ level.name }}</td>
					<td><UnitValue :unit-id="MONEY" :value="level.cost" /></td>
					<td><UnitValue :unit-id="MONEY" :value="level.payout" /></td>
					<td v-if="marketPrice"><FormattedNumber :number="level.payout / marketPrice * 100" :decimals="1" />%</td>
					<td><FormattedNumber :number="level.payout / level.cost" :decimals="1" />x</td>
				</tr>
			</tbody>
		</table>
	</CardWrapper>
</template>
