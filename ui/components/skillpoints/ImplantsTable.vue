<script setup lang="ts">
import {getJitaSellPrice} from "~/lib/marketUtils";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import TypeLink from "~/components/helpers/TypeLink.vue";
import {DAY} from "~/lib/timeUtils";
import {calculateAcceleratedSkillpoints} from "~/lib/skillUtils";
import Duration from "~/components/dogma/units/Duration.vue";
import {ImplantIskPerSp, type ImplantSet} from "~/components/skillpoints/ImplantSet";

const props = defineProps<{
	sets: ImplantSet[]
}>();

// Load prices.
const prices: { [key: number]: number } = {};
await Promise.all(props.sets.flatMap(set => {
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
props.sets.forEach(set => {
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
	<table class="table-auto w-full my-3">
		<thead>
			<tr>
				<th class="text-left">Set</th>
				<th class="text-right">Bonus</th>
				<th class="text-left">Implant</th>
				<th class="text-right">Price</th>
				<th class="text-right">Set Price</th>
				<th class="text-left">Account</th>
				<th class="text-right">SP/mth</th>
				<th class="text-right">Life</th>
				<th class="text-right">ISK/SP</th>
			</tr>
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
					</tr>
				</template>
			</template>
		</tbody>
	</table>
</template>

<style scoped>
</style>
