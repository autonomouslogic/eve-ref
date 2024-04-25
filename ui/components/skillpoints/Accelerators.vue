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
import {DAY} from "~/lib/timeUtils";
import {
	calculateAcceleratedSkillpointsAlpha,
	calculateAcceleratedSkillpointsOmega,
	calculateBoosterDuration
} from "~/lib/boosterUtils";

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

class Accelerator {
	typeId: number = 0;
	slot: number = 0;
	bonus: number = 0;
	duration: number = 0;
	acceleratedSpOmega: number = 0;
	acceleratedSpAlpha: number = 0;
	maximumSpOmega: number = 0;
	maximumSpAlpha: number = 0;
	plex: number = 0;
	isk: number = 0;
	iskPerSpOmega: number = 0;
	iskPerSpAlpha: number = 0;
	note: string = "";
}

async function initAccelerator(typeId: number, accelerator: Accelerator): Promise<Accelerator> {
	await cacheTypeBundle(typeId);
	const type = await refdata.getType({typeId});
	const attrs = Object.values(await loadDogmaAttributesForType(type));
	const biology = 5;

	const slot = await getSlot(typeId);
	const bonus = getTypeAttributeByName("intelligenceBonus", type, attrs)?.value || 0;
	const baseDuration = accelerator.duration || getTypeAttributeByName("boosterDuration", type, attrs)?.value || 0;
	const duration = accelerator.duration || calculateBoosterDuration(baseDuration, biology);
	const acceleratedSpOmega = calculateAcceleratedSkillpointsOmega(bonus, duration);
	const acceleratedSpAlpha = calculateAcceleratedSkillpointsAlpha(bonus, duration);
	const maximumSpOmega = accelerator.maximumSpOmega || acceleratedSpOmega / duration * 30 * DAY;
	const maximumSpAlpha = accelerator.maximumSpAlpha || acceleratedSpAlpha / duration * 30 * DAY;
	const isk = (accelerator.plex > 0 ? accelerator.plex * plexPrice : await getJitaSellPrice(typeId)) || 0;
	const iskPerSpOmega = isk / acceleratedSpOmega;
	const iskPerSpAlpha = isk / acceleratedSpAlpha;

	return {
		typeId,
		slot,
		bonus,
		duration,
		acceleratedSpOmega,
		acceleratedSpAlpha,
		maximumSpOmega,
		maximumSpAlpha,
		plex: accelerator.plex,
		isk,
		iskPerSpOmega,
		iskPerSpAlpha,
		note: accelerator.note,
	} as Accelerator;
}

const accelerators = [
	await initAccelerator(MASTER_AT_ARMS_CEREBRAL_ACCELERATOR, new Accelerator()),
	await initAccelerator(PROTOTYPE_CEREBRAL_ACCELERATOR, {
		duration: 14 * DAY,
		note: "New pilots only, from contracts",
		maximumSpOmega: -1,
		maximumSpAlpha: -1,
	} as Accelerator),
	await initAccelerator(BASIC_BOOST_CEREBRAL_ACCELERATOR, { plex: 5 } as Accelerator),
	await initAccelerator(STANDARD_BOOST_CEREBRAL_ACCELERATOR, { plex: 20 } as Accelerator),
	await initAccelerator(ADVANCED_BOOST_CEREBRAL_ACCELERATOR, { plex: 45 } as Accelerator),
	await initAccelerator(SPECIALIST_BOOST_CEREBRAL_ACCELERATOR, { plex: 80 } as Accelerator),
	await initAccelerator(EXPERT_BOOST_CEREBRAL_ACCELERATOR, { plex: 125 } as Accelerator),
	await initAccelerator(GENIUS_BOOST_CEREBRAL_ACCELERATOR, { plex: 180 } as Accelerator),
];
</script>

<template>
	<h2>Accelerators</h2>
	<p>
		All duration and skill point calculations assume <TypeLink type-id="3405"></TypeLink> V.
		Max SP/mth is based on if a new accelerator is installed right after the previous one.
	</p>
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
					<td class="text-left" rowspan="2">
						<TypeLink :type-id="accelerator.typeId" />
						<template v-if="accelerator.note">
							<br/>
							({{ accelerator.note }})
						</template>
					</td>
					<td class="text-right" rowspan="2">{{ accelerator.slot }}</td>
					<td class="text-right" rowspan="2">
						<template v-if="accelerator.plex > 0 || accelerator.isk > 0">
							<template v-if="accelerator.plex > 0">
								<FormattedNumber :number="accelerator.plex" /> PLEX -
							</template>
							<Money :value="accelerator.isk" />
						</template>
						<template v-else>-</template>
					</td>
					<td class="text-right" rowspan="2">+{{accelerator.bonus}} for <Duration :milliseconds="accelerator.duration" /></td>
					<td class="text-left">Alpha</td>
					<td class="text-right">
						<FormattedNumber :number="accelerator.acceleratedSpAlpha" />
					</td>
					<td class="text-right">
						<template v-if="accelerator.maximumSpAlpha > 0">
							<FormattedNumber :number="accelerator.maximumSpAlpha" />
						</template>
						<template v-else>-</template>
					</td>
					<td class="text-right">
						<template v-if="accelerator.iskPerSpAlpha > 0">
							<Money :value="accelerator.iskPerSpAlpha" />
						</template>
						<template v-else>-</template>
					</td>
				</tr>

				<tr>
					<td class="text-left">Omega</td>
					<td class="text-right"><FormattedNumber :number="accelerator.acceleratedSpOmega" /></td>
					<td class="text-right">
						<template v-if="accelerator.maximumSpOmega > 0">
							<FormattedNumber :number="accelerator.maximumSpOmega" />
						</template>
						<template v-else>-</template>
					</td>
					<td class="text-right">
						<template v-if="accelerator.iskPerSpOmega > 0">
							<Money :value="accelerator.iskPerSpOmega" />
						</template>
						<template v-else>-</template>
					</td>
				</tr>
			</template>
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
