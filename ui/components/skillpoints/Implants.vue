<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {
	CYBERNETIC_SUBPROCESSOR_BASIC,
	CYBERNETIC_SUBPROCESSOR_IMPROVED,
	CYBERNETIC_SUBPROCESSOR_STANDARD,
	LIMITED_CYBERNETIC_SUBPROCESSOR,
	LIMITED_CYBERNETIC_SUBPROCESSOR_BETA,
	LIMITED_MEMORY_AUGMENTATION,
	LIMITED_MEMORY_AUGMENTATION_BETA,
	LIMITED_NEURAL_BOOST,
	LIMITED_NEURAL_BOOST_BETA,
	LIMITED_OCULAR_FILTER,
	LIMITED_OCULAR_FILTER_BETA,
	LIMITED_SOCIAL_ADAPTATION_CHIP,
	LIMITED_SOCIAL_ADAPTATION_CHIP_BETA,
	MEMORY_AUGMENTATION_BASIC,
	MEMORY_AUGMENTATION_IMPROVED,
	MEMORY_AUGMENTATION_STANDARD,
	NEURAL_BOOST_BASIC,
	NEURAL_BOOST_IMPROVED,
	NEURAL_BOOST_STANDARD,
	OCULAR_FILTER_BASIC,
	OCULAR_FILTER_IMPROVED,
	OCULAR_FILTER_STANDARD,
	SOCIAL_ADAPTATION_CHIP_BASIC,
	SOCIAL_ADAPTATION_CHIP_IMPROVED,
	SOCIAL_ADAPTATION_CHIP_STANDARD,
} from "~/lib/typeConstants";
import {DAY} from "~/lib/timeUtils";
import {calculateAcceleratedSkillpoints} from "~/lib/skillUtils";
import Duration from "~/components/dogma/units/Duration.vue";
import refdataApi, {cacheGroupBundle} from "~/refdata";
import {CYBERIMPLANT} from "~/lib/groupConstants";
import refdata from "~/refdata";

class ImplantSet {
	name: string = "";
	bonus: number = 0;
	typeIds: number[] = [];
	totalPrice: number = 0;
	spPerMonthAlpha: number = 0;
	spPerMonthOmega: number = 0;
	iskPerSps: ImplantIskPerSp[] = [];
}

class ImplantIskPerSp {
	duration: number = 0;
	iskPerSpAlpha: number = 0;
	iskPerSpOmega: number = 0;
}

const sets: ImplantSet[] = [
	{
		name: "Limited",
		bonus: 1,
		typeIds: [
			LIMITED_OCULAR_FILTER,
			LIMITED_MEMORY_AUGMENTATION,
			LIMITED_NEURAL_BOOST,
			LIMITED_CYBERNETIC_SUBPROCESSOR,
			LIMITED_SOCIAL_ADAPTATION_CHIP,
		]
	} as ImplantSet,
	{
		name: "Limited Beta",
		bonus: 2,
		typeIds: [
			LIMITED_OCULAR_FILTER_BETA,
			LIMITED_MEMORY_AUGMENTATION_BETA,
			LIMITED_NEURAL_BOOST_BETA,
			LIMITED_CYBERNETIC_SUBPROCESSOR_BETA,
			LIMITED_SOCIAL_ADAPTATION_CHIP_BETA,
		]
	} as ImplantSet,
	{
		name: "Basic",
		bonus: 3,
		typeIds: [
			OCULAR_FILTER_BASIC,
			MEMORY_AUGMENTATION_BASIC,
			NEURAL_BOOST_BASIC,
			CYBERNETIC_SUBPROCESSOR_BASIC,
			SOCIAL_ADAPTATION_CHIP_BASIC,
		]
	} as ImplantSet,
	{
		name: "Standard",
		bonus: 4,
		typeIds: [
			OCULAR_FILTER_STANDARD,
			MEMORY_AUGMENTATION_STANDARD,
			NEURAL_BOOST_STANDARD,
			CYBERNETIC_SUBPROCESSOR_STANDARD,
			SOCIAL_ADAPTATION_CHIP_STANDARD,
		]
	} as ImplantSet,
	{
		name: "Improved",
		bonus: 5,
		typeIds: [
			OCULAR_FILTER_IMPROVED,
			MEMORY_AUGMENTATION_IMPROVED,
			NEURAL_BOOST_IMPROVED,
			CYBERNETIC_SUBPROCESSOR_IMPROVED,
			SOCIAL_ADAPTATION_CHIP_IMPROVED,
		]
	} as ImplantSet,
	// {
	//   name: "Advanced",
	//   bonus: 6,
	//   typeIds: [
	//     OCULAR_FILTER_ADVANCED,
	//     MEMORY_AUGMENTATION_ADVANCED,
	//     NEURAL_BOOST_ADVANCED,
	//     CYBERNETIC_SUBPROCESSOR_ADVANCED,
	//     SOCIAL_ADAPTATION_CHIP_ADVANCED,
	//   ]
	// } as ImplantSet,
	// {
	//   name: "Elite",
	//   bonus: 7,
	//   typeIds: [
	//     OCULAR_FILTER_ELITE,
	//     MEMORY_AUGMENTATION_ELITE,
	//     NEURAL_BOOST_ELITE,
	//     CYBERNETIC_SUBPROCESSOR_ELITE,
	//   ]
	// } as ImplantSet,
];

// Load prices.
const prices: { [key: number]: number } = {};
await Promise.all(sets.flatMap(set => {
	var total = 0;
	return Promise.all(set.typeIds.map(async typeId => {
		const price = await getJitaSellPrice(typeId) || 0;
		total += price;
		prices[typeId] = price;
	})).then(() => {
		set.totalPrice = total;
	});
}));

// Calculate SP per month.
const iskPerSpDurations = [7 * DAY, 14 * DAY, 30 * DAY];
sets.forEach(set => {
	set.spPerMonthAlpha = calculateAcceleratedSkillpoints(set.bonus, 30 * DAY, false);
	set.spPerMonthOmega = calculateAcceleratedSkillpoints(set.bonus, 30 * DAY, true);

	set.iskPerSps = iskPerSpDurations.map(d => {
		return {
			duration: d,
			iskPerSpAlpha: set.totalPrice / calculateAcceleratedSkillpoints(set.bonus, d, false),
			iskPerSpOmega: set.totalPrice / calculateAcceleratedSkillpoints(set.bonus, d, true),
		} as ImplantIskPerSp;
	});
});

</script>

<template>
	<h2>Implants</h2>
	<p>
		Each set with a breakdown per implant.
		ISK/SP is calculated for the full set, based on how long the implants are installed for.
		If you get podded, this will show you how much you end up paying per SP.
		Since installation is permanent, ISK/SP tends to zero.
	</p>
	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left">Set</th>
			<th class="text-right">Bonus</th>
			<th class="text-left">Implant</th>
			<th class="text-right">Price</th>
			<th class="text-right">Set Price</th>
			<th class="text-left">Account</th>
			<th class="text-right">SP/mth</th>
			<th class="text-right">Life</th>
			<th class="text-right">ISK/SP</th>
		</thead>

		<tbody>
			<template v-for="set in sets" :key="set.name">
				<template v-for="(typeId, ti) in set.typeIds" :key="typeId">
					<tr>
						<td class="text-left" v-if="ti == 0" :rowspan="5">{{ set.name }}</td>
						<td class="text-right" v-if="ti == 0" :rowspan="5">+{{ set.bonus }}</td>
						<td class="text-left"><TypeLink :type-id="typeId" /></td>
						<td class="text-right"><Money :value="prices[typeId]" /></td>
						<td class="text-right" v-if="ti == 0" :rowspan="5"><Money :value="set.totalPrice" /></td>

						<td class="text-left" v-if="ti == 0" :rowspan="2">Alpha</td>
						<td class="text-left" v-if="ti == 2" :rowspan="3">Omega</td>

						<td class="text-right" v-if="ti == 0" :rowspan="2"><FormattedNumber :number="set.spPerMonthAlpha" /></td>
						<td class="text-right" v-if="ti == 2" :rowspan="3"><FormattedNumber :number="set.spPerMonthOmega" /></td>

						<td class="text-right" v-if="ti == 0"><Duration :milliseconds="set.iskPerSps[0].duration" /></td>
						<td class="text-right" v-if="ti == 0"><Money :value="set.iskPerSps[0].iskPerSpAlpha" /></td>
						<td class="text-right" v-if="ti == 1"><Duration :milliseconds="set.iskPerSps[1].duration" /></td>
						<td class="text-right" v-if="ti == 1"><Money :value="set.iskPerSps[1].iskPerSpAlpha" /></td>
						<td class="text-right" v-if="ti == 2"><Duration :milliseconds="set.iskPerSps[0].duration" /></td>
						<td class="text-right" v-if="ti == 2"><Money :value="set.iskPerSps[0].iskPerSpOmega" /></td>
						<td class="text-right" v-if="ti == 3"><Duration :milliseconds="set.iskPerSps[1].duration" /></td>
						<td class="text-right" v-if="ti == 3"><Money :value="set.iskPerSps[1].iskPerSpOmega" /></td>
						<td class="text-right" v-if="ti == 4"><Duration :milliseconds="set.iskPerSps[2].duration" /></td>
						<td class="text-right" v-if="ti == 4"><Money :value="set.iskPerSps[2].iskPerSpOmega" /></td>


						<!--						<td class="text-right">-->
						<!--							<div v-for="iskPerSp in set.iskPerSps" :key="iskPerSp.duration">-->
						<!--								<Duration :milliseconds="iskPerSp.duration" /> /-->
						<!--								<Money :value="iskPerSp.iskPerSpAlpha" /> /-->
						<!--								<Money :value="iskPerSp.iskPerSpOmega" />-->
						<!--							</div>-->
						<!--						</td>-->
					</tr>
				</template>
			</template>
		</tbody>
	</table>
</template>

<style scoped>
</style>
