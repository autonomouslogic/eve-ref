<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {MULTIPLE_PILOT_TRAINING_CERTIFICATE, PLEX_TYPE_ID} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

const averageAttribute = 20;
const omegaSkillPointsPerMonth = 30 * 24 * 60 * (averageAttribute + averageAttribute / 2);
const alphaSkillPointsPerMonth = omegaSkillPointsPerMonth / 2;
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const multiPrice = await getJitaSellPrice(MULTIPLE_PILOT_TRAINING_CERTIFICATE) || 0;

</script>

<template>
	<h2>Accounts</h2>
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
				<td class="text-left">Alpha account</td>
				<td class="text-left"></td>
				<td class="text-right"><Money :value="0" /></td>
				<td class="text-right"><FormattedNumber :number="alphaSkillPointsPerMonth" /></td>
				<td class="text-right"><Money :value="0" /></td>
			</tr>

			<tr>
				<td class="text-left">Omega account</td>
				<td class="text-left"></td>
				<td class="text-right">
					500 PLEX<br/>
					<Money :value="500 * plexPrice" />
				</td>
				<td class="text-right">
					<FormattedNumber :number="omegaSkillPointsPerMonth" /><br/>
					(<FormattedNumber :number="omegaSkillPointsPerMonth - alphaSkillPointsPerMonth" /> more than Alpha)
				</td>
				<td class="text-right"><Money :value="500 * plexPrice / (omegaSkillPointsPerMonth - alphaSkillPointsPerMonth)" /></td>
			</tr>

			<tr>
				<td class="text-left"><TypeLink :type-id="MULTIPLE_PILOT_TRAINING_CERTIFICATE" /></td>
				<td class="text-left">Omega</td>
				<td class="text-right"><Money :value="multiPrice" /></td>
				<td class="text-right"><FormattedNumber :number="omegaSkillPointsPerMonth" /> per month</td>
				<td class="text-right"><Money :value="multiPrice / omegaSkillPointsPerMonth" /></td>
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
