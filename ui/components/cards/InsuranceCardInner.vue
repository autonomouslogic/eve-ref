<script setup lang="ts">
import {type InventoryType} from "~/refdata-openapi";
import {insuranceApi} from "~/esi";
import UnitValue from "~/components/dogma/UnitValue.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import {getJitaSellPrice} from "~/lib/marketUtils";
import {MONEY} from "~/lib/unitConstants";
import type {GetInsurancePricesRequest} from "~/esi-openapi";

const props = defineProps<{
	inventoryType: InventoryType
}>();

const prices = (await insuranceApi.getInsurancePrices({} as GetInsurancePricesRequest))
	.filter(e => e.typeId == props.inventoryType.typeId)
	.pop();
const marketPrice = props.inventoryType.typeId ? await getJitaSellPrice(props.inventoryType.typeId) : undefined;
</script>

<template>
	<div class="overflow-scroll">
		<table v-if="prices" class="standard-table">
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
					<td class="text-right">
						<UnitValue :unit-id="MONEY" :value="level.cost"/>
					</td>
					<td class="text-right">
						<UnitValue :unit-id="MONEY" :value="level.payout"/>
					</td>
					<td v-if="marketPrice" class="text-right mx-4">
						<FormattedNumber :number="level.payout / marketPrice * 100" :decimals="1"/>
						%
					</td>
					<td class="text-right">
						<FormattedNumber :number="level.payout / level.cost" :decimals="1"/>
						x
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>

<style scoped>

</style>
