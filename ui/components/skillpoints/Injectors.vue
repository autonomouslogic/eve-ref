<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {DAILY_ALPHA_INJECTOR, LARGE_SKILL_INJECTOR, PLEX_TYPE_ID, SMALL_SKILL_INJECTOR} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {alphaInjectorPrices} from "~/conf/newEdenStore";

const injectorPoints = [
	{
		limit: "up to 5,000,000 SP",
		points: 500_000
	},
	{
		limit: "up to 50,000,000 SP",
		points: 400_000
	},
	{
		limit: "up to 80,000,000 SP",
		points: 300_000
	},
	{
		limit: "over 80,000,000 SP",
		points: 150_000
	},
];

const alphaInjectorSp = 50_000;

const alphaInjectorPrice = await getJitaSellPrice(DAILY_ALPHA_INJECTOR) || 0;
const smallInjectorPrice = await getJitaSellPrice(SMALL_SKILL_INJECTOR) || 0;
const largeInjectorPrice = await getJitaSellPrice(LARGE_SKILL_INJECTOR) || 0;
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;

</script>

<template>
	<h2>Injectors</h2>
	<table class="table-auto w-full my-3">
		<thead>
			<tr>
				<th class="text-left"></th>
				<th class="text-left">Account Restriction</th>
				<th class="text-right">Price</th>
				<th class="text-right">Injected SP</th>
				<th class="text-right">Max SP/mth</th>
				<th class="text-right">ISK/SP</th>
			</tr>
		</thead>

		<tbody>
			<tr>
				<td class="text-left"><TypeLink :type-id="DAILY_ALPHA_INJECTOR" /> (Jita)</td>
				<td class="text-left" :rowspan="alphaInjectorPrices.length + 1">Alpha</td>
				<td class="text-right"><Money :value="alphaInjectorPrice" /></td>
				<td class="text-right"><FormattedNumber :number="alphaInjectorSp" /></td>
				<td class="text-right" :rowspan="alphaInjectorPrices.length + 1"><FormattedNumber :number="alphaInjectorSp * 30" /></td>
				<td class="text-right"><Money :value="alphaInjectorPrice / alphaInjectorSp" /></td>
			</tr>

			<tr v-for="injectorPrice in alphaInjectorPrices" :key="injectorPrice.count">
				<td class="text-left"><TypeLink :type-id="DAILY_ALPHA_INJECTOR" /> ({{ injectorPrice.count }}x, NES)</td>
				<td class="text-right">
					<FormattedNumber :number="injectorPrice.count * injectorPrice.plex" /> PLEX -
					<Money :value="injectorPrice.plex * plexPrice" />
				</td>
				<td class="text-right"><FormattedNumber :number="injectorPrice.count * alphaInjectorSp" /></td>
				<td class="text-right"><Money :value="injectorPrice.plex * plexPrice / (injectorPrice.count * alphaInjectorSp)" /></td>
			</tr>

			<template v-for="point, idx in injectorPoints" :key="idx">
				<tr>
					<td class="text-left"><TypeLink :type-id="SMALL_SKILL_INJECTOR" /></td>
					<td class="text-left" rowspan="2">Omega - {{point.limit}}</td>
					<td class="text-right"><Money :value="smallInjectorPrice" /></td>
					<td class="text-right"><FormattedNumber :number="point.points / 5" /></td>
					<td class="text-right">&infin;</td>
					<td class="text-right"><Money :value="smallInjectorPrice / (point.points / 5)" /></td>
				</tr>
				<tr>
					<td class="text-left"><TypeLink :type-id="LARGE_SKILL_INJECTOR" /></td>
					<td class="text-right"><Money :value="largeInjectorPrice" /></td>
					<td class="text-right"><FormattedNumber :number="point.points" /></td>
					<td class="text-right">&infin;</td>
					<td class="text-right"><Money :value="largeInjectorPrice / point.points" /></td>
				</tr>
			</template>
		</tbody>
	</table>
</template>

<style scoped>
</style>
