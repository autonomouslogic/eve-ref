<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {DAILY_ALPHA_INJECTOR, LARGE_SKILL_INJECTOR, SMALL_SKILL_INJECTOR} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

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

const alphaInjectorPrice = await getJitaSellPrice(DAILY_ALPHA_INJECTOR) || 0;
const smallInjectorPrice = await getJitaSellPrice(SMALL_SKILL_INJECTOR) || 0;
const largeInjectorPrice = await getJitaSellPrice(LARGE_SKILL_INJECTOR) || 0;

</script>

<template>
	<h2>Injectors</h2>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left"></th>
			<th class="text-left">Account Restriction</th>
			<th class="text-right">Price</th>
			<th class="text-right">Injected SP</th>
			<th class="text-right">Max SP/mth</th>
			<th class="text-right">ISK/SP</th>
		</thead>

		<tr>
			<td class="text-left"><TypeLink :type-id="DAILY_ALPHA_INJECTOR" /></td>
			<td class="text-left">Alpha</td>
			<td class="text-right"><Money :value="alphaInjectorPrice" /></td>
			<td class="text-right"><FormattedNumber :number="50000" /></td>
			<td class="text-right"><FormattedNumber :number="50000 * 30" /></td>
			<td class="text-right"><Money :value="alphaInjectorPrice / 50000" /></td>
		</tr>

		<template v-for="point, idx in injectorPoints" :key="idx">
			<tr>
				<td class="text-left"><TypeLink :type-id="SMALL_SKILL_INJECTOR" /></td>
				<td class="text-left">Omega - {{point.limit}}</td>
				<td class="text-right"><Money :value="smallInjectorPrice" /></td>
				<td class="text-right"><FormattedNumber :number="point.points / 5" /></td>
				<td class="text-right">&infin;</td>
				<td class="text-right"><Money :value="smallInjectorPrice / (point.points / 5)" /></td>
			</tr>
			<tr>
				<td class="text-left"><TypeLink :type-id="LARGE_SKILL_INJECTOR" /></td>
				<td class="text-left">Omega - {{point.limit}}</td>
				<td class="text-right"><Money :value="largeInjectorPrice" /></td>
				<td class="text-right"><FormattedNumber :number="point.points" /></td>
				<td class="text-right">&infin;</td>
				<td class="text-right"><Money :value="largeInjectorPrice / point.points" /></td>
			</tr>
		</template>
	</table>
</template>

<style scoped>
h2 {
  @apply mt-6;
}
th, tr {
  @apply border-b border-slate-500;
}
th, td {
  @apply px-2;
}
</style>
