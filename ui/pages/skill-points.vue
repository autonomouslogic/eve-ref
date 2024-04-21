<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {getJitaSellPrice, getOrders, THE_FORGE_REGION_ID} from "~/lib/marketUtils";
import {GetMarketsRegionIdOrdersOrderTypeEnum} from "~/esi-openapi";
import {
	DAILY_ALPHA_INJECTOR, LARGE_SKILL_INJECTOR,
	MASTER_AT_ARMS_CEREBRAL_ACCELERATOR,
	MULTIPLE_PILOT_TRAINING_CERTIFICATE,
	PLEX_TYPE_ID, SMALL_SKILL_INJECTOR
} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";

useHead({
	title: "Skill Points"
});

const averageAttribute = 20;
const omegaSkillPointsPerMonth = 30 * 24 * 60 * (averageAttribute + averageAttribute / 2);
const alphaSkillPointsPerMonth = omegaSkillPointsPerMonth / 2;
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const multiPrice = await getJitaSellPrice(MULTIPLE_PILOT_TRAINING_CERTIFICATE) || 0;
const alphaInjectorPrice = await getJitaSellPrice(DAILY_ALPHA_INJECTOR) || 0;
const masterAtArmsPrice = await getJitaSellPrice(MASTER_AT_ARMS_CEREBRAL_ACCELERATOR) || 0;
const smallInjectorPrice = await getJitaSellPrice(SMALL_SKILL_INJECTOR) || 0;
const largeInjectorPrice = await getJitaSellPrice(LARGE_SKILL_INJECTOR) || 0;

const injectorPoints = [
	{
		limit: "up to 5,000,000",
		points: 500_000
	},
	{
		limit: "up to 50,000,000",
		points: 400_000
	},
	{
		limit: "up to 80,000,000",
		points: 300_000
	},
	{
		limit: "over 80,000,000",
		points: 150_000
	},
];

</script>

<template>
	<h1>Skill Points</h1>
	<div>PLEX price: <Money :value="plexPrice" /></div>

	<h2>Accounts</h2>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left">Source</th>
			<th class="text-right">Price</th>
			<th class="text-right">Skill points</th>
			<th class="text-right">ISK per SP</th>
		</thead>

		<tbody>
			<tr>
				<td class="text-left">Alpha account</td>
				<td class="text-right">-</td>
				<td class="text-right"><FormattedNumber :number="alphaSkillPointsPerMonth" /> per month</td>
				<td class="text-right">-</td>
			</tr>

			<tr>
				<td class="text-left">Omega account</td>
				<td class="text-right">500 PLEX (<Money :value="500 * plexPrice" />)</td>
				<td class="text-right">
					<FormattedNumber :number="omegaSkillPointsPerMonth" /> per month
					(<FormattedNumber :number="omegaSkillPointsPerMonth - alphaSkillPointsPerMonth" /> more than Alpha)
				</td>
				<td class="text-right"><Money :value="500 * plexPrice / (omegaSkillPointsPerMonth - alphaSkillPointsPerMonth)" /></td>
			</tr>

			<tr>
				<td class="text-left"><TypeLink :type-id="MULTIPLE_PILOT_TRAINING_CERTIFICATE" /></td>
				<td class="text-right"><Money :value="multiPrice" /></td>
				<td class="text-right"><FormattedNumber :number="omegaSkillPointsPerMonth" /> per month</td>
				<td class="text-right"><Money :value="multiPrice / omegaSkillPointsPerMonth" /></td>
			</tr>

		</tbody>
	</table>

	<h2>Injectors</h2>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left">Source</th>
			<th class="text-right">Price</th>
			<th class="text-right">Skill points</th>
			<th class="text-right">ISK per SP</th>
		</thead>

		<tbody>
			<tr>
				<td class="text-left"><TypeLink :type-id="DAILY_ALPHA_INJECTOR" /></td>
				<td class="text-right"><Money :value="alphaInjectorPrice" /></td>
				<td class="text-right"><FormattedNumber :number="50000" /> per day</td>
				<td class="text-right"><Money :value="alphaInjectorPrice / 50000" /></td>
			</tr>

			<template v-for="point, idx in injectorPoints" :key="idx">
				<tr>
					<td class="text-left"><TypeLink :type-id="SMALL_SKILL_INJECTOR" /> ({{point.limit}})</td>
					<td class="text-right"><Money :value="smallInjectorPrice" /></td>
					<td class="text-right"><FormattedNumber :number="point.points / 5" /></td>
					<td class="text-right"><Money :value="smallInjectorPrice / (point.points / 5)" /></td>
				</tr>
				<tr>
					<td class="text-left"><TypeLink :type-id="LARGE_SKILL_INJECTOR" /> ({{point.limit}})</td>
					<td class="text-right"><Money :value="largeInjectorPrice" /></td>
					<td class="text-right"><FormattedNumber :number="point.points" /></td>
					<td class="text-right"><Money :value="largeInjectorPrice / point.points" /></td>
				</tr>
			</template>
		</tbody>

		<h2>Accelerators</h2>
		<table class="table-auto w-full my-3">
			<thead>
				<th class="text-left">Source</th>
				<th class="text-right">Price</th>
				<th class="text-right">Skill points</th>
				<th class="text-right">ISK per SP</th>
			</thead>

			<tbody>
				<tr>
					<td class="text-left"><TypeLink :type-id="MASTER_AT_ARMS_CEREBRAL_ACCELERATOR" /> (Alpha)</td>
					<td class="text-right"><Money :value="masterAtArmsPrice" /></td>
					<td class="text-right"><FormattedNumber :number="10800" /> per day</td>
					<td class="text-right"><Money :value="masterAtArmsPrice / 10800" /></td>
				</tr>

				<tr>
					<td class="text-left"><TypeLink :type-id="MASTER_AT_ARMS_CEREBRAL_ACCELERATOR" /> (Omega)</td>
					<td class="text-right"><Money :value="masterAtArmsPrice" /></td>
					<td class="text-right"><FormattedNumber :number="21600" /> per day</td>
					<td class="text-right"><Money :value="masterAtArmsPrice / 21600" /></td>
				</tr>
			</tbody>
		</table>

		<h2>Implants</h2>
	</table>

	<h2>Resources</h2>
	<div>
		<ul class="list-disc list-inside">
			<li><ExternalLink url="https://wiki.eveuniversity.org/Skills_and_learning">Skills and learning</ExternalLink> - UniWiki</li>
		</ul>
	</div>
</template>

<style scoped>
th, tr {
  @apply border-b border-slate-500;
}
</style>
