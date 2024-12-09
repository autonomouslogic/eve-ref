<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {
	DATE_TOVIKOV_CHAR,
	DISCORD_URL,
	EVE_REF_CHAR,
	EVE_REF_CORP,
	EVE_REFERRAL_URL,
	EVE_STORE_URL,
	GITHUB_URL,
	HETZNER_REFERAL_URL,
	MARKEE_DRAGON_URL,
	PATREON_URL
} from "~/lib/urls";
import {getJitaSellPrice} from "~/lib/marketUtils";
import {LARGE_SKILL_INJECTOR, PLEX_TYPE_ID} from "~/lib/typeConstants";
import {DateTime} from "luxon";
import refdata from "~/refdata";
import TypeLink from "~/components/helpers/TypeLink.vue";
import Money from "~/components/dogma/units/Money.vue";
import Datetime from "~/components/dogma/units/Datetime.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";
import Duration from "~/components/dogma/units/Duration.vue";
import {DAY} from "~/lib/timeUtils";

useHead({
	title: "🎉 Giveaways"
});

interface Prize {
	name: string,
	worth: number,
	typeId: number,
	quantity: number,
	dates: DateTime[],
	i: number
}

const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;

const firstFleetPack = DateTime.fromISO("2024-12-13T20:00:00+09");
const fleetPackDates = [];
for (let i = 0; i < 19; i++) {
	fleetPackDates.push(firstFleetPack.plus({days: i}));
}

const prizes: Prize[] = [
	{
		name: "Weekend Fleet Pack (50x PLEX, 3 Day Omega)",
		worth: (50 + 500 * 12 / 365 * 3) * plexPrice,
		dates: fleetPackDates
	} as Prize,
	{
		typeId: PLEX_TYPE_ID,
		quantity: 500,
		dates: [
			DateTime.fromISO("2024-12-15T20:00:00+09"),
			DateTime.fromISO("2024-12-22T20:00:00+09"),
			DateTime.fromISO("2024-12-29T20:00:00+09"),
			DateTime.fromISO("2024-12-31T20:00:00+09")
		]
	} as Prize,
	{
		typeId: LARGE_SKILL_INJECTOR,
		quantity: 1,
		dates: [
			DateTime.fromISO("2024-12-14T20:00:00+09"),
			DateTime.fromISO("2024-12-22T20:00:00+09"),
			DateTime.fromISO("2024-12-28T20:00:00+09")
		]
	} as Prize,
	{
		typeId: LARGE_SKILL_INJECTOR,
		quantity: 5,
		dates: [
			DateTime.fromISO("2024-12-31T20:00:00+09")
		]
	} as Prize
];

var i = 0;
for (let prize of prizes) {
	if (prize.worth == undefined && prize.typeId != undefined && prize.quantity > 0) {
		const price = await getJitaSellPrice(prize.typeId) || 0;
		prize.worth = price * prize.quantity;
	}
}

const unrolled = prizes.flatMap(prize => prize.dates.map(date => {
	return ({
		name: prize.name,
		worth: prize.worth,
		typeId: prize.typeId,
		quantity: prize.quantity,
		dates: [date],
		i : i++
	});
}));

unrolled.sort((a, b) => a.dates[0].toMillis() - b.dates[0].toMillis());

const totalWorth = unrolled.reduce((acc, prize) => acc + prize.worth, 0);

</script>

<template>
	<h1>🎉 Giveaways</h1>
	<p>
		Join #giveaways on <InternalLink :to="DISCORD_URL">Discord</InternalLink> to participate.
	</p>
	<p>
		I'm giving away <Money :value="totalWorth" /> in December 2024.
		Below is the approximate schedule of the upcoming giveaways.
	</p>

	<h2>Schedule</h2>
	<table class="standard-table">
		<thead>
			<th>Prize</th>
			<th class="text-right">Worth</th>
			<th>Approximate draw time</th>
		</thead>
		<tbody>
			<tr v-for="prize in unrolled" :key="prize.i">

				<td v-if="prize.name">{{prize.name}}</td>
				<td v-else-if="prize.typeId">
					<template v-if="prize.quantity">{{prize.quantity}}&nbsp;</template>
					<TypeLink :type-id="prize.typeId" />
				</td>

				<td class="text-right">
					<Money :value="prize.worth" />
				</td>

				<td>
					<template v-if="prize.dates[0].toMillis() > DateTime.now().toMillis()">
						<Datetime :millisecond-epoch="prize.dates[0].toMillis()" />
						<template v-if="prize.dates[0].toMillis() - DateTime.now().toMillis() < DAY">
							(today)
						</template>
						<template v-if="prize.dates[0].toMillis() - DateTime.now().toMillis() < 7 * DAY">
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
</template>

<style scoped>
p {
  @apply my-4;
}
</style>
