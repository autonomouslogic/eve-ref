<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {MULTIPLE_PILOT_TRAINING_CERTIFICATE, PLEX_TYPE_ID} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {calculateSkillpoints} from "~/lib/skillUtils";
import {DAY} from "~/lib/timeUtils";
import {mctPrices, omegaPrices} from "~/conf/newEdenStore";

const averageAttribute = 20;
const omegaSkillPointsPerMonth = calculateSkillpoints(averageAttribute, averageAttribute, 30 * DAY, true);
const alphaSkillPointsPerMonth = calculateSkillpoints(averageAttribute, averageAttribute, 30 * DAY, false);
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const multiPrice = await getJitaSellPrice(MULTIPLE_PILOT_TRAINING_CERTIFICATE) || 0;
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

			<tr v-for="(omega, idx) in omegaPrices" :key="omega.months">
				<td class="text-left">Omega ({{ omega.months }} month{{ omega.months > 1 ? "s" : "" }})</td>
				<td class="text-left"></td>
				<td class="text-right">
					<FormattedNumber :number="omega.plex" /> PLEX -
					<Money :value="omega.plex * plexPrice" />
				</td>
				<td class="text-right" v-if="idx == 0" :rowspan="omegaPrices.length">
					<FormattedNumber :number="omegaSkillPointsPerMonth" />
				</td>
				<td class="text-right">
					<Money :value="omega.plex * plexPrice / (omega.months * omegaSkillPointsPerMonth)" />
				</td>
			</tr>

			<tr>
				<td class="text-left"><TypeLink :type-id="MULTIPLE_PILOT_TRAINING_CERTIFICATE" /> (Jita)</td>
				<td class="text-left">Omega</td>
				<td class="text-right"><Money :value="multiPrice" /></td>
				<td class="text-right" :rowspan="mctPrices.length + 1"><FormattedNumber :number="omegaSkillPointsPerMonth" /></td>
				<td class="text-right"><Money :value="multiPrice / omegaSkillPointsPerMonth" /></td>
			</tr>

			<tr v-for="mct in mctPrices" :key="mct.count">
				<td class="text-left">Multiple Character Training ({{ mct.count }}x, NES)</td>
				<td class="text-left">Omega</td>
				<td class="text-right">
					<FormattedNumber :number="mct.plex" /> PLEX -
					<Money :value="mct.plex * plexPrice" />
				</td>
				<td class="text-right"><Money :value="mct.plex * plexPrice / (mct.count * omegaSkillPointsPerMonth)" /></td>
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
