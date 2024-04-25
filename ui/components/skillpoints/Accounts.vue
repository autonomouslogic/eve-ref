<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {MULTIPLE_PILOT_TRAINING_CERTIFICATE, PLEX_TYPE_ID} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {calculateSkillpoints} from "~/lib/skillUtils";
import {DAY} from "~/lib/timeUtils";

const averageAttribute = 20;
const omegaSkillPointsPerMonth = calculateSkillpoints(averageAttribute, averageAttribute, 30 * DAY, true);
const alphaSkillPointsPerMonth = calculateSkillpoints(averageAttribute, averageAttribute, 30 * DAY, false);
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const multiPrice = await getJitaSellPrice(MULTIPLE_PILOT_TRAINING_CERTIFICATE) || 0;

const omegaPrices = [
	{ months: 1, plex: 500 },
	{ months: 3, plex: 1200 },
	{ months: 6, plex: 2100 },
	{ months: 12, plex: 3600 },
	{ months: 24, plex: 6600 },
];

const mctPrices = [
	{ count: 1, plex: 350 },
	{ count: 3, plex: 800 },
	{ count: 6, plex: 1500 },
	{ count: 12, plex: 2700 },
	{ count: 24, plex: 4600 },
];

</script>

<template>
	<h2>Accounts</h2>
	<p>
		SP/mth is based on all attributes set to {{ averageAttribute }}.
	</p>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left"></th>
			<th class="text-left">Account Restriction</th>
			<th class="text-right">Price</th>
			<th class="text-right">SP/mth</th>
			<th class="text-right">ISK/SP</th>
		</thead>

		<tbody>
			<tr>
				<td class="text-left">Alpha</td>
				<td class="text-left">Up to 5,000,000 SP</td>
				<td class="text-right"><Money :value="0" /></td>
				<td class="text-right"><FormattedNumber :number="alphaSkillPointsPerMonth" /></td>
				<td class="text-right"><Money :value="0" /></td>
			</tr>

			<tr v-for="(omegaPrice, idx) in omegaPrices" :key="omegaPrice.months">
				<td class="text-left">Omega ({{ omegaPrice.months }} month{{ omegaPrice.months > 1 ? "s" : "" }})</td>
				<td class="text-left"></td>
				<td class="text-right">
					<FormattedNumber :number="omegaPrice.plex" /> PLEX -
					<Money :value="omegaPrice.plex * plexPrice" />
				</td>
				<td class="text-right" v-if="idx == 0" :rowspan="omegaPrices.length">
					<FormattedNumber :number="omegaSkillPointsPerMonth" />
				</td>
				<td class="text-right">
					<Money :value="omegaPrice.plex * plexPrice / (omegaPrice.months * omegaSkillPointsPerMonth)" />
				</td>
			</tr>

			<tr>
				<td class="text-left"><TypeLink :type-id="MULTIPLE_PILOT_TRAINING_CERTIFICATE" /> (Jita)</td>
				<td class="text-left">Omega</td>
				<td class="text-right"><Money :value="multiPrice" /></td>
				<td class="text-right" :rowspan="mctPrices.length + 1"><FormattedNumber :number="omegaSkillPointsPerMonth" /></td>
				<td class="text-right"><Money :value="multiPrice / omegaSkillPointsPerMonth" /></td>
			</tr>

			<tr v-for="mctPrice in mctPrices" :key="mctPrice.count">
				<td class="text-left">Multiple Character Training ({{ mctPrice.count }}x, NES)</td>
				<td class="text-left">Omega</td>
				<td class="text-right">
					<FormattedNumber :number="mctPrice.plex" /> PLEX -
					<Money :value="mctPrice.plex * plexPrice" />
				</td>
				<td class="text-right"><Money :value="mctPrice.plex * plexPrice / (mctPrice.count * omegaSkillPointsPerMonth)" /></td>
			</tr>
		</tbody>
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
