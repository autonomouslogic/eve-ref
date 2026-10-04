<script setup lang="ts">
import {DateTime} from "luxon";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import InternalLink from "~/components/helpers/InternalLink.vue";
import Money from "~/components/dogma/units/Money.vue";
import {DISCORD_URL, EVE_REFERRAL_URL, EVE_STORE_URL, MARKEE_DRAGON_URL, PATREON_URL} from "~/lib/urls";
import FormattedNumber from "~/components/helpers/FormattedNumber.vue";
import ProgressBar from "~/components/helpers/ProgressBar.vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {getJitaSellPrice} from "~/lib/marketUtils";
import {PLEX_TYPE_ID} from "~/lib/typeConstants";
import {formatMoney} from "~/lib/money";
import {formatNumber} from "~/lib/number";

useHead({
	title: "🎉 10 Years of EVE Ref"
});

// Mock data - to be wired up to the real donation/prize API later.
const ANNIVERSARY_DATE = DateTime.fromISO("2027-10-08T00:55:32Z", {zone: "utc"});
const START_DATE = DateTime.fromISO("2027-10-01T00:00:00Z", {zone: "utc"});

const plexPrice = await getJitaSellPrice(PLEX_TYPE_ID) || 0;
const oneMonthValue = 500 * plexPrice;
const sixMonthsValue = 6 * oneMonthValue;

const topDonors = [
	{name: "Kuokkaa Anki", amount: 69e9},
	{name: "Arnsterik Fror", amount: 30e9},
	{name: "Goggert Innolf", amount: 20e9},
	{name: "Benencel Crielere", amount: 19e9},
	{name: "Skiadore Kerane", amount: 1.5e9},
	{name: "Maro Yama", amount: 900e6},
	{name: "Asly Tarmas", amount: 500e6},
	{name: "Shahshin Bourz", amount: 500e6},
	{name: "Aikelwalo Sodalrat", amount: 150e6},
	{name: "Kezti Sundara", amount: 42e6},
];

const donatedSoFar = topDonors.reduce((sum, donor) => sum + donor.amount, 0);
const donationGoal = 300e9;

let prizeMoneyPool = 100e9;
const prizeMoneyLevels: {value: number, winners?: number}[] = [
	{value: 10e9},
	{value: 5e9},
	{value: 2.5e9},
	{value: 1e9}
];

prizeMoneyLevels.forEach((level, i) => {
	const isLast = i === prizeMoneyLevels.length - 1;
	if (level.winners === undefined) {
		level.winners = Math.floor(prizeMoneyPool / (isLast ? 1 : 3) / level.value);
	}
	prizeMoneyPool -= level.winners * level.value;
});
const prizes = [
	{
		name: "Ten Years of Omega",
		description: [
			`10x 6-month Omega codes, value ${formatMoney(sixMonthsValue, 1)} each`,
			`60x 1-month Omega codes, value ${formatMoney(oneMonthValue, 1)} each`,
		],
		winners: "65 winners",
	},
	...prizeMoneyLevels.map((level) => ({
		name: `${formatNumber(level.value, 0)} ISK`,
		description: [""],
		winners: `${level.winners} winner${level.winners === 1 ? "" : "s"}`,
	})),
];

const now = ref(DateTime.utc());
let timer: ReturnType<typeof setInterval> | undefined;
onMounted(() => {
	timer = setInterval(() => {
		now.value = DateTime.utc();
	}, 1000);
});
onUnmounted(() => {
	if (timer) clearInterval(timer);
});

const remaining = computed(() => {
	const diff = START_DATE.diff(now.value, ["days", "hours", "minutes", "seconds"]);
	return {
		days: Math.max(0, Math.floor(diff.days)),
		hours: Math.max(0, Math.floor(diff.hours)),
		minutes: Math.max(0, Math.floor(diff.minutes)),
		seconds: Math.max(0, Math.floor(diff.seconds)),
	};
});

const donationProgress = computed(() => Math.max(0, Math.min(1, donatedSoFar / donationGoal)));
const donationMarks = computed(() => [0, donationGoal / 3, donationGoal * 2 / 3, donationGoal]);
</script>

<template>
	<div class="ten-years">
		<section class="hero text-center py-12 px-4 rounded-lg mb-8">
			<h1 class="anniversary-title">10 Years</h1>
			<h2 class="of-eve-ref">of EVE Ref</h2>
			<p class="tagline mt-4 max-w-xl mx-auto">
				Win <b>ten years</b> of Omega!
			</p>
			<p class="tagline mt-4 max-w-xl mx-auto">
				A decade of data, dedication, and the EVE community.<br/>
				Thank you for being part of the journey!
			</p>
			<div class="date-badge inline-block mt-4 px-4 py-2 border">
				📅 {{ANNIVERSARY_DATE.toFormat("yyyy-MM-dd HH:mm:ss")}} UTC
			</div>
		</section>

		<section class="card flex flex-col md:flex-row gap-8 justify-between mb-8">
			<div class="flex-1 text-center">
				<div class="text-sm tracking-wide text-gray-400 mb-1">DONATED SO FAR</div>
				<div class="donated-amount accent text-4xl font-bold">
					<FormattedNumber :number="donatedSoFar" /> ISK
				</div>
				<ProgressBar :progress="donationProgress" color="#e8567e" class="my-4 h-4 rounded">
					<span></span>
				</ProgressBar>
				<div class="flex justify-between text-xs text-gray-400">
					<span v-for="mark in donationMarks" :key="mark"><Money :value="mark" :decimals="0" /></span>
				</div>
				<p class="text-xs text-gray-400 mt-3">
					All donations are made in-game to "EVE Ref" and 100% will be used for giveaways!
					<InternalLink to="/about">Read more.</InternalLink>
				</p>
			</div>
			<div class="flex-1">
				<h3 class="accent">Support EVE Ref and win amazing prizes!</h3>
				<p class="my-3">
					For the past ten years, EVE Ref has been <i>the</i> reference database for EVE Online.
					EVE Ref also collects and archives game data 24/7 and makes 5.7 TB of data available to anyone for free.
					Join <ExternalLink :url="PATREON_URL"><span><font-awesome-icon icon="fa-brands fa-patreon" /></span> Patreon</ExternalLink>
					and help me keep it online and updated for another ten years!
					Your donations keep the servers running and the data flowing.
				</p>
				<p class="my-3">
					Thank you for all your support over the years!<br/>
				</p>
			</div>
		</section>

		<section class="card text-center mb-8">
			<h3>Countdown to 10-Year Anniversary</h3>
			<p class="text-gray-400 mb-4">
				The celebration starts on:
				📅 {{START_DATE.toFormat("yyyy-MM-dd HH:mm:ss")}} EVE Time
			</p>
			<div class="countdown flex justify-center gap-8 md:gap-16">
				<div>
					<div class="accent text-5xl font-bold">{{remaining.days}}</div>
					<div class="text-xs tracking-wide text-gray-400">DAYS</div>
				</div>
				<div>
					<div class="accent text-5xl font-bold">{{remaining.hours}}</div>
					<div class="text-xs tracking-wide text-gray-400">HOURS</div>
				</div>
				<div>
					<div class="accent text-5xl font-bold">{{remaining.minutes}}</div>
					<div class="text-xs tracking-wide text-gray-400">MINUTES</div>
				</div>
				<div>
					<div class="accent text-5xl font-bold">{{remaining.seconds}}</div>
					<div class="text-xs tracking-wide text-gray-400">SECONDS</div>
				</div>
			</div>
		</section>

		<section class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
			<div class="card">
				<h3 class="accent">🏆 Top Donors (2027)</h3>
				<p class="text-gray-400 mb-3">Thank you to our incredible supporters!</p>
				<table class="standard-table">
					<tbody>
						<tr v-for="(donor, i) in topDonors" :key="donor.name">
							<td class="rank"><span class="rank-badge">{{i + 1}}</span></td>
							<td>{{donor.name}}</td>
							<td class="text-right"><Money :value="donor.amount" /></td>
						</tr>
					</tbody>
				</table>
				<p class="text-right mt-3">
					<InternalLink to="#" class="accent">View all donors &raquo;</InternalLink>
				</p>
			</div>

			<div class="card">
				<h3 class="accent">🎁 Amazing Prizes</h3>
				<p class="text-gray-400 mb-3">These prizes will be raffled throughout October!</p>
				<table class="standard-table">
					<tbody>
						<tr v-for="prize in prizes" :key="prize.name">
							<td>
								<div class="accent font-bold">{{prize.name}}</div>
								<div v-for="(d, i) in prize.description" :key="i" class="text-sm text-gray-400">{{d}}</div>
							</td>
							<td class="text-right whitespace-nowrap">{{prize.winners}}</td>
						</tr>
					</tbody>
				</table>
				<p class="text-right mt-3">
					<InternalLink to="/giveaways" class="accent">Giveaway Schedule &raquo;</InternalLink>
				</p>
			</div>
		</section>
	</div>
</template>

<style scoped>
.accent {
	color: #e8567e;
}

.hero {
	background: linear-gradient(180deg, rgba(232, 86, 126, 0.12), transparent);
}

.anniversary-title {
	@apply text-6xl md:text-7xl font-extrabold;
	color: #e8567e;
}

.of-eve-ref {
	@apply text-3xl md:text-4xl font-extrabold text-white;
}

.date-badge {
	border-color: #e8567e;
	color: #e8567e;
}

.card {
	background-color: var(--card-background-color);
	@apply p-6;
}

.rank-badge {
	@apply inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold;
	background-color: #e8567e;
	color: white;
}

table.standard-table td {
	border-color: rgba(255, 255, 255, 0.08);
}
</style>
