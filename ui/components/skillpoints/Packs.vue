<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {
	ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	EXPERT_BOOST_CEREBRAL_ACCELERATOR,
	GENIUS_BOOST_CEREBRAL_ACCELERATOR,
	PLEX_TYPE_ID,
	SKILL_EXTRACTOR,
	SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
	STANDARD_BOOST_CEREBRAL_ACCELERATOR
} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {DAY} from "~/lib/timeUtils";
import {calculateAcceleratedSkillpoints, calculateBoosterDuration, calculateSkillpoints} from "~/lib/skillUtils";
import Duration from "~/components/dogma/units/Duration.vue";
import {getTypeAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import refdata, {cacheTypeBundle} from "~/refdata";

const biology = 5;

class Pack {
	name: string = "";
	plex: number = 0;
	omega: number = 0;
	includedSp: number = 0;
	accelerator: number = 0;
	skillExtractors: number = 0;
	mct: number = 0;

	omegaOnly: boolean = false;
	finalSpOmega: number = 0;
	finalSpAlpha: number = 0;
	price: number = 0;
	effectivePrice: number = 0;
	iskPerSp: number = 0;
}

const packPrices = [
	{
		name: "Apprentice Bundle",
		plex: 565,
		includedSp: 50_000,
		accelerator: GENIUS_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Novice Bundle",
		plex: 658,
		includedSp: 100_000 + 250_000,
		accelerator: EXPERT_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Graduate Bundle",
		plex: 820,
		includedSp: 2 * 100_000 + 2 * 250_000,
		accelerator: GENIUS_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Master Bundle",
		plex: 1530,
		includedSp: 1_000_000 + 3 * 250_000,
		accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Explorer Career Pack",
		plex: 275,
		omega: 14 * DAY,
		accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Industrialist Career Pack",
		plex: 350,
		omega: 14 * DAY,
		accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Enforcer Career Pack",
		plex: 425,
		omega: 14 * DAY,
		accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Soldier of Fortune Career Pack",
		plex: 475,
		omega: 14 * DAY,
		accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Warclone Omega Bundle",
		plex: 250,
		omega: 14 * DAY,
	} as Pack,
	{
		name: "Bronze Starter Pack",
		plex: 250,
		includedSp: 50_000,
		accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Silver Starter Pack",
		plex: 500,
		includedSp: 250_000,
		omega: 30 * DAY,
		accelerator: ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Gold Starter Pack",
		plex: 1000,
		includedSp: 500_000,
		omega: 60 * DAY,
		accelerator: STANDARD_BOOST_CEREBRAL_ACCELERATOR,
	} as Pack,
	{
		name: "Platinum Starter Pack",
		plex: 2000,
		includedSp: 500_000,
		omega: 90 * DAY,
		mct: 2,
		accelerator: SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
		skillExtractors: 10,
	} as Pack,
];

const averageAttribute = 20;
const omegaSkillPointsPerDay = calculateSkillpoints(averageAttribute, averageAttribute, DAY, true);
const omegaSkillPointsPerMonth = calculateSkillpoints(averageAttribute, averageAttribute, 30 * DAY, true);
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const extractorPrice = await getJitaSellPrice(SKILL_EXTRACTOR) || 0;

await Promise.all(packPrices.map(async pack => {
	pack.finalSpOmega = 0;
	pack.finalSpAlpha = 0;
	if (pack.includedSp > 0) {
		pack.finalSpOmega = pack.includedSp;
		pack.finalSpAlpha = pack.includedSp;
	}
	if (pack.omega > 0) {
		pack.finalSpOmega += pack.omega / DAY * omegaSkillPointsPerDay;
		pack.omegaOnly = true;
	}
	if (pack.mct > 0) {
		pack.finalSpOmega += pack.mct * omegaSkillPointsPerMonth;
		pack.omegaOnly = true;
	}
	if (pack.accelerator > 0) {
		await cacheTypeBundle(pack.accelerator);
		const type = await refdata.getType({typeId: pack.accelerator});
		const attrs = Object.values(await loadDogmaAttributesForType(type));
		const bonus = getTypeAttributeByName("intelligenceBonus", type, attrs)?.value || 0;
		const baseDuration = getTypeAttributeByName("boosterDuration", type, attrs)?.value || 0;
		const duration = calculateBoosterDuration(baseDuration, biology);
		const acceleratedSpOmega = calculateAcceleratedSkillpoints(bonus, duration, true);
		const acceleratedSpAlpha = calculateAcceleratedSkillpoints(bonus, duration, false);
		pack.finalSpOmega += acceleratedSpOmega;
		pack.finalSpAlpha += acceleratedSpAlpha;
	}

	pack.price = pack.plex * plexPrice;
	pack.effectivePrice = pack.price;
	if (pack.skillExtractors > 0) {
		pack.effectivePrice -= pack.skillExtractors * extractorPrice;
	}
}));

</script>

<template>
	<h2>Packs</h2>
	<p>
		Skill Extractors included the packs are assumed to be sold directly on the market, reducing the final ISK/SP.
		SKINs, crates, and Expert Systems are not included in the calculations at all.
		Packs which include Omega time do not show Alpha values.
	</p>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left"></th>
			<th class="text-right">Price</th>
			<th class="text-left">Account</th>
			<th class="text-right">Total SP</th>
			<th class="text-right">ISK/SP</th>
		</thead>

		<template v-for="(packPrice, idx) in packPrices" :key="idx">
			<tr>
				<td class="text-left" rowspan="2">
					<b>{{ packPrice.name }}</b>
					<div class="ml-6">
						<p v-if="packPrice.includedSp > 0">
							<FormattedNumber :number="packPrice.includedSp" /> SP
						</p>
						<p v-if="packPrice.omega > 0">
							<Duration :milliseconds="packPrice.omega" /> Omega
						</p>
						<p v-if="packPrice.mct > 0">
							<FormattedNumber :number="packPrice.mct" /> MCT Certificates
						</p>
						<p v-if="packPrice.skillExtractors > 0">
							{{ packPrice.skillExtractors }}x <TypeLink :type-id="SKILL_EXTRACTOR" />
						</p>
						<p v-if="packPrice.accelerator > 0">
							<TypeLink :type-id="packPrice.accelerator" />
						</p>
					</div>
				</td>
				<td class="text-right" rowspan="2">
					<FormattedNumber :number="packPrice.plex" /> PLEX -
					<Money :value="packPrice.plex * plexPrice" />
					<template v-if="packPrice.price != packPrice.effectivePrice">
						<br/>Effective: <Money :value="packPrice.effectivePrice" />
					</template>
				</td>
				<td class="text-left">
					<template v-if="!packPrice.omegaOnly">Alpha</template>
					<template v-else>-</template>
				</td>
				<td class="text-right">
					<template v-if="!packPrice.omegaOnly">
						<FormattedNumber :number="packPrice.finalSpAlpha" />
					</template>
					<template v-else>-</template>
				</td>
				<td class="text-right">
					<template v-if="!packPrice.omegaOnly">
						<Money :value="packPrice.effectivePrice / packPrice.finalSpAlpha" />
					</template>
					<template v-else>-</template>
				</td>
			</tr>
			<tr>
				<td class="text-left">Omega</td>
				<td class="text-right"><FormattedNumber :number="packPrice.finalSpOmega" /></td>
				<td class="text-right"><Money :value="packPrice.effectivePrice / packPrice.finalSpOmega" /></td>
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
