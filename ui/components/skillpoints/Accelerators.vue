<script setup lang="ts">
import {
	ADVANCED_BOOST_CEREBRAL_ACCELERATOR,
	BASIC_BOOST_CEREBRAL_ACCELERATOR,
	EXPERT_BOOST_CEREBRAL_ACCELERATOR,
	GENIUS_BOOST_CEREBRAL_ACCELERATOR,
	MASTER_AT_ARMS_CEREBRAL_ACCELERATOR,
	PLEX_TYPE_ID,
	PROTOTYPE_CEREBRAL_ACCELERATOR,
	SPECIALIST_BOOST_CEREBRAL_ACCELERATOR,
	STANDARD_BOOST_CEREBRAL_ACCELERATOR
} from "~/lib/typeConstants";
import {getJitaSellPrice} from "~/lib/marketUtils";
import TypeLink from "~/components/helpers/TypeLink.vue";
import Money from "~/components/dogma/units/Money.vue";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import refdata, {cacheTypeBundle} from "~/refdata";
import {getTypeAttributeByName, loadDogmaAttributesForType} from "~/lib/dogmaUtils";
import Duration from "~/components/dogma/units/Duration.vue";
import {DAY, MINUTE} from "~/lib/timeUtils";

const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;

async function getBonus(typeId: number): Promise<number> {
	var type = await refdata.getType({typeId});
	var attrs = Object.values(await loadDogmaAttributesForType(type));
	return getTypeAttributeByName("intelligenceBonus", type, attrs)?.value || 0;
}

async function getDuration(typeId: number): Promise<number> {
	var type = await refdata.getType({typeId});
	var attrs = Object.values(await loadDogmaAttributesForType(type));
	return getTypeAttributeByName("boosterDuration", type, attrs)?.value || 0;
}

async function getSlot(typeId: number): Promise<number> {
	var type = await refdata.getType({typeId});
	var attrs = Object.values(await loadDogmaAttributesForType(type));
	return getTypeAttributeByName("boosterness", type, attrs)?.value || 0;
}

async function initAccelerator(typeId: number, plex: number): Promise<any> {
	await cacheTypeBundle(typeId);
	const biology = 5;
	const bonus = await getBonus(typeId);
	const isk = (plex > 0 ? plex * plexPrice : await getJitaSellPrice(typeId)) || 0;
	const duration = await getDuration(typeId);
	const acceleratedSp = bonus * 1.5 * duration / MINUTE * (1 + 0.2 * biology);
	const maximumSp = acceleratedSp / duration * 30 * DAY;
	const iskPerSp = isk / acceleratedSp;
	const slot = await getSlot(typeId);
	return {
		typeId,
		slot,
		bonus,
		duration,
		acceleratedSp,
		maximumSp,
		plex,
		isk,
		iskPerSp
	};
}

const accelerators = [
	await initAccelerator(MASTER_AT_ARMS_CEREBRAL_ACCELERATOR, 0),
	await initAccelerator(PROTOTYPE_CEREBRAL_ACCELERATOR, 0),
	await initAccelerator(BASIC_BOOST_CEREBRAL_ACCELERATOR, 5),
	await initAccelerator(STANDARD_BOOST_CEREBRAL_ACCELERATOR, 20),
	await initAccelerator(ADVANCED_BOOST_CEREBRAL_ACCELERATOR, 45),
	await initAccelerator(SPECIALIST_BOOST_CEREBRAL_ACCELERATOR, 80),
	await initAccelerator(EXPERT_BOOST_CEREBRAL_ACCELERATOR, 125),
	await initAccelerator(GENIUS_BOOST_CEREBRAL_ACCELERATOR, 180),
];
</script>

<template>
	<h2>Accelerators</h2>
	<i>Assuming Biology V</i>
	<table class="table-auto w-full my-3">
		<thead>
			<th></th>
			<th class="text-right">Slot</th>
			<th class="text-right">Price</th>
			<th class="text-right">Bonus</th>
			<th class="text-left">Account</th>
			<th class="text-right">Accelerated SP</th>
			<th class="text-right">Max SP/mth</th>
			<th class="text-right">ISK/SP</th>
		</thead>

		<tbody>
			<template v-for="accelerator in accelerators" :key="accelerator.typeId">
				<tr>
					<td class="text-left" rowspan="2"><TypeLink :type-id="accelerator.typeId" /></td>
					<td class="text-right" rowspan="2">{{ accelerator.slot }}</td>
					<td class="text-right" rowspan="2">
						<template v-if="accelerator.plex > 0 || accelerator.isk > 0">
							<template v-if="accelerator.plex > 0">
								<FormattedNumber :number="accelerator.plex" /> PLEX
								<br/>
							</template>
							<Money :value="accelerator.isk" />
						</template>
						<template v-else>-</template>
					</td>
					<td class="text-right" rowspan="2">+{{accelerator.bonus}} for <Duration :milliseconds="accelerator.duration" /></td>
					<td class="text-left">Alpha</td>
					<td class="text-right"><FormattedNumber :number="accelerator.acceleratedSp / 2" /></td>
					<td class="text-right"><FormattedNumber :number="accelerator.maximumSp / 2" /></td>
					<td class="text-right"><Money :value="accelerator.iskPerSp * 2" /></td>
				</tr>

				<tr>
					<td class="text-left">Omega</td>
					<td class="text-right"><FormattedNumber :number="accelerator.acceleratedSp" /></td>
					<td class="text-right"><FormattedNumber :number="accelerator.maximumSp" /></td>
					<td class="text-right"><Money :value="accelerator.iskPerSp" /></td>
				</tr>
			</template>
		</tbody>
	</table>
</template>

<style scoped>
th, tr {
  @apply border-b border-slate-500;
}
th, td {
  @apply px-2;
}
</style>
