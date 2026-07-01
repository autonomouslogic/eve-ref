<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {DISCORD_URL} from "~/lib/urls";
import {getJitaSellPrice} from "~/lib/marketUtils";
import {PLEX_TYPE_ID} from "~/lib/typeConstants";
import {DateTime} from "luxon";
import Money from "~/components/dogma/units/Money.vue";
import Datetime from "~/components/dogma/units/Datetime.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";
import {DAY} from "~/lib/timeUtils";

useHead({
	title: "🎉 Giveaways"
});

interface Giveaway {
	name: string,
	value: number,
	winners: number,
	endTime: DateTime,
	started: boolean,
	i: number
}

// Giveaway registry - add new giveaway types here as needed
interface GiveawayCalculator {
	calculateValue: () => Promise<number>;
}

const giveawayRegistry: Record<string, GiveawayCalculator> = {
	"Weekend Fleet Pack (50x PLEX, 3 Day Omega)": {
		calculateValue: async () => {
			const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
			return (50 + 500 * 12 / 365 * 3) * plexPrice;
		}
	},
	// Add more giveaway types here: each name maps to a calculator function
	// Example:
	// "Maller Scope Syndication YC122 SKIN": {
	// 	calculateValue: async () => getJitaSellPrice(56884) || 0
	// }
};

const weekdays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

// Fetch giveaways from API (both current and upcoming)
const [upcomingResponse, currentResponse] = await Promise.all([
	fetch("https://api.prizepixie.fun/v1/guilds/1074946548208783400/channels/1316251424073453568/upcoming-giveaways"),
	fetch("https://api.prizepixie.fun/v1/guilds/1074946548208783400/channels/1316251424073453568/current-giveaways")
]);

const upcomingGiveaways = await upcomingResponse.json();
const currentGiveaways = await currentResponse.json();
const allApiGiveaways = [...currentGiveaways, ...upcomingGiveaways];

// Convert API response to giveaway objects with calculated values
const unrolled: Giveaway[] = [];
let giveawayIndex = 0;

for (const apiGiveaway of allApiGiveaways) {
	// Skip cancelled or failed giveaways
	if (apiGiveaway.cancelled || apiGiveaway.failed) continue;

	// Look up calculator for this giveaway name, calculate value
	const calculator = giveawayRegistry[apiGiveaway.name];
	const value = calculator ? await calculator.calculateValue() : 0;

	unrolled.push({
		name: apiGiveaway.name,
		value,
		winners: apiGiveaway.winners,
		endTime: DateTime.fromISO(apiGiveaway.endTime),
		started: apiGiveaway.started,
		i: giveawayIndex++
	});
}

unrolled.sort((a, b) => a.endTime.toMillis() - b.endTime.toMillis());

const pastGiveaways = {
	"July 2026": 10 * 478.70e6,
	"June 2026": 8 * 472.44e6,
	"May 2026": 10 * 495.28e6,
	"April 2026": 8 * 441.95e6,
	"March 2026": 10 * 441.95e6,
	"February 2026": 8 * 452.88e6 + 2 * 167.50e6 + 1 * 598.80e6 + 2 * 325.00,
	"January 2026": 10 * 496.48e6,
	"December 2025 🍾": 8 * 486.64e6 + 45 * 501.34e6 + 5 * 319.40e6 + 5 * 170.00e6 + 5 * 128.80e6 + 5 * 174.00e6 + 5 * 650.00e6 + 5 * 779.90e6 + 6e9,
	"November 2025": 8 * 500.55e6,
	"October 2025": 10 * 552.29e6,
	"September 2025": 8 * 556.16e6,
	"August 2025": 10 * 602.94e6,
	"July 2025": 8 * 620.72e6,
	"June 2025": 8 * 620.72e6 + 4 * 99.99e6 + 4 * 184.00e6,
	"May 2025": 10 * 629.26e6,
	"April 2025": 8 * 617.04e6,
	"March 2025": 4.92e9,
	"February 2025": 15727787671,
	"January 2025": 610787671,
	"December 2024 🍻": 33.52e9,
};

const totalPastPrizes = Object.values(pastGiveaways).reduce(
	(previousValue, currentValue, currentIndex, array): number => {
		return previousValue + currentValue;
	});

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
			<tr v-for="giveaway in unrolled" :key="giveaway.i">
				<td>
					{{giveaway.name}}
					<span v-if="giveaway.started" class="badge">Started</span>
				</td>

				<td class="text-right">
					<Money :value="giveaway.value" />
				</td>

				<td class="text-right">
					{{giveaway.winners}}
				</td>

				<td>
					<template v-if="giveaway.endTime.toMillis() > DateTime.now().toMillis()">
						<Datetime :millisecond-epoch="giveaway.endTime.toMillis()" />
						<template v-if="giveaway.endTime.toUTC().toMillis() - DateTime.now().toUTC().toMillis() < DAY">
							(today)
						</template>
						<template v-else-if="giveaway.endTime.toMillis() - DateTime.now().toMillis() < 8 * DAY">
							(next {{weekdays[giveaway.endTime.toUTC().weekday - 1]}})
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
	<p>
		Total prizes in past giveaways: <Money :value="totalPastPrizes" />
	</p>
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

.badge {
  @apply ml-2 px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800;
}
</style>
