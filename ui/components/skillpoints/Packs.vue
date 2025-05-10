<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import {PLEX_TYPE_ID, SKILL_EXTRACTOR} from "~/lib/typeConstants";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {DAY} from "~/lib/timeUtils";
import {calculateAcceleratedSkillpoints, calculateBoosterDuration, calculateSkillpoints} from "~/lib/skillUtils";
import Duration from "~/components/dogma/units/Duration.vue";
import {getTypeAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import refdata, {cacheTypeBundle} from "~/refdata";
import {PackPrice, packPrices} from "~/conf/newEdenStore";

const biology = 5;

const averageAttribute = 20;
const omegaSkillPointsPerDay = calculateSkillpoints(averageAttribute, averageAttribute, DAY, true);
const omegaSkillPointsPerMonth = calculateSkillpoints(averageAttribute, averageAttribute, 30 * DAY, true);
const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const extractorPrice = await getJitaSellPrice(SKILL_EXTRACTOR) || 0;

class Pack {
	price: PackPrice;
	omegaOnly: boolean = false;
	finalSpOmega: number = 0;
	finalSpAlpha: number = 0;
	isk: number = 0;
	effectivePrice: number = 0;
	iskPerSp: number = 0;

	constructor(price: PackPrice) {
		this.price = price;
	}
}

const packs: Pack[] = [];
await Promise.all(packPrices.map(async packPrice => {
	const pack = new Pack(packPrice);
	packs.push(pack);
	pack.finalSpOmega = 0;
	pack.finalSpAlpha = 0;
	if (packPrice.includedSp > 0) {
		pack.finalSpOmega = packPrice.includedSp;
		pack.finalSpAlpha = packPrice.includedSp;
	}
	if (packPrice.omega > 0) {
		pack.finalSpOmega += packPrice.omega / DAY * omegaSkillPointsPerDay;
		pack.omegaOnly = true;
	}
	if (packPrice.mct > 0) {
		pack.finalSpOmega += packPrice.mct * omegaSkillPointsPerMonth;
		pack.omegaOnly = true;
	}
	if (packPrice.accelerators && packPrice.accelerators.length > 0) {
		const accelerators = packPrice.accelerators;
		for (let accelerator of accelerators) {
			await cacheTypeBundle(accelerator.accelerator);
			const type = await refdata.getType({typeId: accelerator.accelerator});
			const attrs = Object.values(await loadDogmaAttributesForType(type));
			const bonus = getTypeAttributeByName("intelligenceBonus", type, attrs)?.value || 0;
			const baseDuration = getTypeAttributeByName("boosterDuration", type, attrs)?.value || 0;
			const duration = calculateBoosterDuration(baseDuration, biology);
			const acceleratedSpOmega = calculateAcceleratedSkillpoints(bonus, duration, true);
			const acceleratedSpAlpha = calculateAcceleratedSkillpoints(bonus, duration, false);
			pack.finalSpOmega += acceleratedSpOmega * accelerator.count;
			pack.finalSpAlpha += acceleratedSpAlpha * accelerator.count;
		}
	}

	pack.isk = packPrice.plex * plexPrice;
	pack.effectivePrice = pack.isk;
	if (packPrice.skillExtractors > 0) {
		pack.effectivePrice -= packPrice.skillExtractors * extractorPrice;
	}
}));
packs.sort((a, b) => a.price.plex - b.price.plex);

</script>

<template>
	<h2>Packs</h2>
	<p>
		Skill Extractors included in the packs are assumed to be sold directly on the market, reducing the final ISK/SP.
		SKINs, crates, and expert systems are excluded from the calculations.
		Packs which include Omega time do not show Alpha values as it's assumed injectors are used within the included time.
	</p>
	<table class="table-auto w-full my-3">
		<thead>
			<tr>
				<th class="text-left"></th>
				<th class="text-right">Price</th>
				<th class="text-left">Account</th>
				<th class="text-right">Total SP</th>
				<th class="text-right">ISK/SP</th>
			</tr>
		</thead>

		<template v-for="(pack, idx) in packs" :key="idx">
			<tr>
				<td class="text-left" rowspan="2">
					<b>{{ pack.price.name }}</b>
					<div class="ml-6">
						<p v-if="pack.price.includedSp > 0">
							<FormattedNumber :number="pack.price.includedSp" /> SP
						</p>
						<p v-if="pack.price.omega > 0">
							<Duration :milliseconds="pack.price.omega" /> Omega
						</p>
						<p v-if="pack.price.mct > 0">
							<FormattedNumber :number="pack.price.mct" /> MCT Certificates
						</p>
						<p v-if="pack.price.skillExtractors > 0">
							{{ pack.price.skillExtractors }}x <TypeLink :type-id="SKILL_EXTRACTOR" />
						</p>
						<template v-if="pack.price.accelerators?.length > 0">
							<p v-for="accelerator in pack.price.accelerators" :key="accelerator.accelerator">
								{{accelerator.count}}x <TypeLink :type-id="accelerator.accelerator" />
							</p>
						</template>
					</div>
				</td>
				<td class="text-right" rowspan="2">
					<FormattedNumber :number="pack.price.plex" /> PLEX -
					<Money :value="pack.isk" />
					<template v-if="pack.isk != pack.effectivePrice">
						<br/>Effective: <Money :value="pack.effectivePrice" />
					</template>
				</td>
				<td class="text-left">
					<template v-if="!pack.omegaOnly">Alpha</template>
					<template v-else>-</template>
				</td>
				<td class="text-right">
					<template v-if="!pack.omegaOnly">
						<FormattedNumber :number="pack.finalSpAlpha" />
					</template>
					<template v-else>-</template>
				</td>
				<td class="text-right">
					<template v-if="!pack.omegaOnly">
						<Money :value="pack.effectivePrice / pack.finalSpAlpha" />
					</template>
					<template v-else>-</template>
				</td>
			</tr>
			<tr>
				<td class="text-left">Omega</td>
				<td class="text-right"><FormattedNumber :number="pack.finalSpOmega" /></td>
				<td class="text-right"><Money :value="pack.effectivePrice / pack.finalSpOmega" /></td>
			</tr>
		</template>
	</table>
</template>

<style scoped>
</style>
