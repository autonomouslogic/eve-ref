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

const LOW_GRADE = "Low-grade";
const MID_GRADE = "Mid-grade";
const HIGH_GRADE = "High-grade";

const ALPHA = "Alpha";
const BETA = "Beta";
const DELTA = "Delta";
const EPSILON = "Epsilon";
const GAMMA = "Gamma";

const implantIds = (await refdata.getGroup({groupId: CYBERIMPLANT})).typeIds || [];

// Set grade -> position name -> [implant id , implant price]
const implantSets: {[key: string]: {[key: string]: number[]}} = {};

await Promise.allSettled(implantIds.map(async implantId => {
	const implant = await refdataApi.getType({typeId: implantId});
	if (!implant) {
		return;
	}
	const name = implant.name?.en;
	if (!name) {
		return;
	}

	let grade;
	if (name.startsWith(LOW_GRADE)) {
		grade = LOW_GRADE;
	} else if (name.startsWith(MID_GRADE)) {
		grade = MID_GRADE;
	} else if (name.startsWith(HIGH_GRADE)) {
		grade = HIGH_GRADE;
	} else {
		return;
	}

	const position = name.split(" ")[2];
	if (!position) {
		return;
	}
	if (position != ALPHA && position != BETA && position != DELTA && position != EPSILON && position != GAMMA) {
		return;
	}

	const price = await getJitaSellPrice(implantId) || 0;
	if (price <= 0) {
		return;
	}

	if (!implantSets[grade]) {
		implantSets[grade] = {};
	}
	if (!implantSets[grade][position]) {
		implantSets[grade][position] = [];
	}
	const current = implantSets[grade][position];

	if (current.length != 2 || current[1] > price) {
		current[0] = implantId;
		current[1] = price;
	}
}));

console.log("Faction implants");
console.log(implantSets);

</script>

<template>
	<h2>Faction Implants</h2>
	<p>
		Same as standard implants above, but tries to assemble the cheapest combination of faction implants available.
		This does not create matching sets, as the Omega slot is ignored and different sets may be mixed.
		It assumes you only care about skill points and not the other bonuses.
	</p>

	<table class="table-auto w-full my-3">
		<thead>
			<th class="text-left">Grade Set</th>
			<th class="text-right">Bonus</th>
			<th class="text-left">Implant</th>
			<th class="text-right">Price</th>
		</thead>

		<tbody>
			<template v-for="grade in [LOW_GRADE, MID_GRADE, HIGH_GRADE]" :key="grade">
				<template v-for="position in [ALPHA, BETA, DELTA, EPSILON, GAMMA]" :key="position">
					<tr>
						<td class="text-left" v-if="position == ALPHA" rowspan="5">{{ grade }}</td>
						<td class="text-right" v-if="position == ALPHA" rowspan="5">+x</td>
						<td class="text-left"><TypeLink :type-id="implantSets[grade][position][0]" /></td>
						<td class="text-right"><Money :value="implantSets[grade][position][1]" /></td>
					</tr>
				</template>
			</template>
		</tbody>
	</table>

</template>

<style scoped>
</style>
