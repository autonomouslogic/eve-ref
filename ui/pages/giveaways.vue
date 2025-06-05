<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {DISCORD_URL} from "~/lib/urls";
import {getJitaSellPrice} from "~/lib/marketUtils";
import {PLEX_TYPE_ID} from "~/lib/typeConstants";
import {DateTime} from "luxon";
import TypeLink from "~/components/helpers/TypeLink.vue";
import Money from "~/components/dogma/units/Money.vue";
import Datetime from "~/components/dogma/units/Datetime.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";
import {DAY} from "~/lib/timeUtils";

useHead({
	title: "🎉 Giveaways"
});

interface Prize {
	name: string,
	typeId: number,
	quantity: number,
	winners: number,
	value: number,
	jitaSpace: boolean,
	dates: DateTime[],
	i: number
}

const ASTERO_SCOPE_SYNDICATION = 56880;
const VEXOR_SCOPE_SYNDICATION = 56882;
const LESHAK_SCOPE_SYNDICATION = 61182;
const RUPTURE_SCOPE_SYNDICATION = 56883;
const STRATIOS_SCOPE_SYNDICATION = 61186;
const MALLER_SCOPE_SYNDICATION = 56884;
const FEDERATION_NAVY_COMET_MEDIA_MIASMA = 84115;
const NIGHTMARE_MEDIA_MIASMA = 84131;

const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;

const firstFleetPack = DateTime.fromISO("2025-06-06T14:00:00Z");
const fleetPackDates = [];
for (let i = 0; i < 8; i++) {
	fleetPackDates.push(firstFleetPack.plus({days: i * 7}));
}

const prizes: Prize[] = [
	{
		name: "Weekend Fleet Pack (50x PLEX, 3 Day Omega)",
		value: (50 + 500 * 12 / 365 * 3) * plexPrice,
		dates: fleetPackDates,
		winners: 2
	} as Prize,
	{
		name: "Astero Scope Syndication YC122 SKIN",
		value: await getJitaSellPrice(ASTERO_SCOPE_SYNDICATION) || 0,
		dates: [
			DateTime.fromISO("2025-06-07T23:00:00Z"),
			DateTime.fromISO("2025-06-14T23:00:00Z"),
			DateTime.fromISO("2025-06-21T23:00:00Z"),
			DateTime.fromISO("2025-06-28T23:00:00Z")
		],
		winners: 1
	} as Prize,
	{
		name: "Maller Scope Syndication YC122 SKIN",
		value: await getJitaSellPrice(MALLER_SCOPE_SYNDICATION) || 0,
		dates: [
			DateTime.fromISO("2025-06-08T23:00:00Z"),
			DateTime.fromISO("2025-06-15T23:00:00Z"),
			DateTime.fromISO("2025-06-22T23:00:00Z"),
			DateTime.fromISO("2025-06-29T23:00:00Z")
		],
		winners: 1
	} as Prize,
];

var i = 0;
for (let prize of prizes) {
	if (prize.quantity == undefined) {
		prize.quantity = 1;
	}
	if (prize.winners == undefined) {
		prize.winners = 1;
	}
	if (prize.value == undefined && prize.typeId != undefined) {
		const price = await getJitaSellPrice(prize.typeId) || 0;
		prize.value = price * prize.quantity;
	}
}

const unrolled = prizes.flatMap(prize => prize.dates.map(date => {
	return ({
		name: prize.name,
		typeId: prize.typeId,
		quantity: prize.quantity,
		value: prize.value,
		winners: prize.winners,
		jitaSpace: prize.jitaSpace,
		dates: [date],
		i : i++
	});
}));

unrolled.sort((a, b) => a.dates[0].toMillis() - b.dates[0].toMillis());

const totalWorth = unrolled.reduce((acc, prize) => acc + prize.value * prize.winners, 0);

const pastGiveaways = {
	"July 2025": 8 * 620.72e6,
	"June 2025": 8 * 620.72e6 + 4 * 99.99e6 + 4 * 184.00e6,
	"May 2025": 10 * 629.26e6,
	"April 2025": 8 * 617.04e6,
	"March 2025": 4.92e9,
	"February 2025": 15727787671,
	"January 2025": 610787671,
	"December 2024": 33.52e9,
};

</script>

<template>
	<h1>🎉 Giveaways</h1>
	<p>
		Join #giveaways on <InternalLink :to="DISCORD_URL">Discord</InternalLink> to participate.
	</p>
	<p>
		Below is the approximate schedule of the upcoming giveaways.
	</p>
	<p>
		Items will be contracted in Jita. Codes will be sent on Discord and can be redeemed via the <ExternalLink url="https://secure.eveonline.com/code-activation">Code Activation</ExternalLink> page.
	</p>

	<h2>Schedule</h2>
	<p v-if="unrolled.length == 0">
		No scheduled giveaways right now, but some may be posted sporadically on Discord.
	</p>
	<table v-else class="standard-table">
		<thead>
			<tr>
				<th>Prize</th>
				<th class="text-right">Value</th>
				<th class="text-right">Winners</th>
				<th>Draw time</th>
			</tr>
		</thead>
		<tbody>
			<tr v-for="prize in unrolled" :key="prize.i">

				<td v-if="prize.name">{{prize.name}}</td>
				<td v-else-if="prize.typeId">
					<template v-if="prize.quantity > 1">{{prize.quantity}}x&nbsp;</template>
					<TypeLink :type-id="prize.typeId" />
					<template v-if="prize.jitaSpace">
						- sponsored by <ExternalLink url="https://jita.space" title="Jita Space">Jita.space</ExternalLink>
					</template>
				</td>

				<td class="text-right">
					<Money :value="prize.value" />
				</td>

				<td class="text-right">
					{{prize.winners}}
				</td>

				<td>
					<template v-if="prize.dates[0].toMillis() > DateTime.now().toMillis()">
						<Datetime :millisecond-epoch="prize.dates[0].toMillis()" />
						<template v-if="prize.dates[0].toMillis() - DateTime.now().toMillis() < DAY">
							(today)
						</template>
						<template v-else-if="prize.dates[0].toMillis() - DateTime.now().toMillis() < 8 * DAY">
							(next {{weekdays[prize.dates[0].weekday - 1]}})
						</template>
					</template>
					<template v-else>
						Ended
					</template>
				</td>

			</tr>
		</tbody>
	</table>

	<h2>Past giveaways</h2>

	<table class="standard-table">
		<thead>
			<tr>
				<th>Event</th>
				<th class="text-right">Prizes</th>
			</tr>
		</thead>
		<tbody>
			<tr v-for="(prizes, name) in pastGiveaways" :key="name">
				<td>{{name}}</td>
				<td class="text-right"><Money :value="prizes" /></td>
			</tr>
		</tbody>
	</table>
</template>

<style scoped>
p {
  @apply my-4;
}
</style>
