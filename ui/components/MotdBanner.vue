<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {DATASETS_DOCS_URL, EVE_REFERRAL_URL, MARKEE_DRAGON_URL, PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";
import {useLazyFetch} from "nuxt/app";
import type {DonationsFile} from "~/lib/donations";
import {formatMoney} from "~/lib/money";
import {DAY, FRIDAY, HOUR, MINUTE, SATURDAY, THURSDAY} from "~/lib/timeUtils";
import {DateTime} from "luxon";

const route = useRoute();

interface Motd {
	text: string
	url: string
	urlText: string
}

const eveatonActive = new Date().getTime() < new Date("2026-11-10T11:00:00Z").getTime();

const motdFallbacks: Motd[] = [
	{
		text: "Save 3% on PLEX and Omega with code \"everef\" at checkout",
		url: MARKEE_DRAGON_URL,
		urlText: "Markee Dragon"
	} as Motd,
	{
		text: "EVE Ref is supported by you",
		url: PATREON_URL,
		urlText: "Become a Patreon"
	} as Motd,
	{
		text: "Servers aren't free",
		url: PATREON_URL,
		urlText: "Become a Patreon"
	} as Motd,
	{
		text: "Get 1,000,000 skill points",
		url: EVE_REFERRAL_URL,
		urlText: "Play EVE Online"
	} as Motd,
	{
		text: "EVE Ref has over 4 TB of historical EVE data",
		url: DATASETS_DOCS_URL,
		urlText: "Explore EVE data"
	} as Motd,
	{
		text: "Donate ISK and get your name shown here",
		url: "/about",
		urlText: "Support EVE Ref"
	} as Motd,
];

const {status: donorsStatus, data: donors} = await useLazyFetch<DonationsFile>("https://static.everef.net/donations.json", {
	server: false
});

const motd = computed(() => {
	const date = new Date();
	// const date = DateTime.fromISO("2025-12-04T14:00:00Z").toJSDate();
	const dayOfWeek = date.getUTCDay();
	const hourOfDay = date.getUTCHours();
	const time = date.getTime();
	const day = Math.floor(time / DAY);
	const hour = Math.floor(time / HOUR);

	// Sale.
	if (time < new Date("2025-12-03T10:50:00Z").getTime()) {
		return {
			text: "Black Friday 25% off - extra 3% with code \"everef\" at checkout",
			url: MARKEE_DRAGON_URL,
			urlText: "Markee Dragon"
		} as Motd;
	}

	// Recent donors.
	if (donorsStatus.value == "success" && donors?.value?.recent?.length && donors.value.recent.length > 0) {
		const donorsText = donors.value.recent.map(donor => {
			return `${donor.donor_name} donated ${formatMoney(donor.amount)}`;
		}).join("<br/>");
		return {
			text: `${donorsText}`,
			url: "/about",
			urlText: "Donate ISK"
		} as Motd;
	}

	// CSM.
	const csmEnds = new Date("2025-11-10T11:00:00Z");
	const timeLeft = csmEnds.getTime() - time;
	if (timeLeft > 0) {
		const daysLeft = Math.floor(timeLeft / DAY);
		const hoursLeft = Math.floor(timeLeft / HOUR);
		const minutesLeft = Math.floor(timeLeft / MINUTE);
		var msg = "Ariel Rin for CSM";
		if (hoursLeft < 8) {
			if (minutesLeft < 60) {
				msg = `CSM VOTING CLOSES IN ${minutesLeft} MINUTE${minutesLeft == 1 ? "" : "S"}`;
			}
			else {
				msg = `CSM VOTING CLOSES IN ${hoursLeft} HOUR${hoursLeft == 1 ? "" : "S"}`;
			}
		}
		else if (hoursLeft < 24) {
			msg += ` - only ${hoursLeft} hour${hoursLeft == 1 ? "" : "s"} left`;
		} else if (daysLeft < 4) {
			msg += ` - only ${daysLeft} day${daysLeft == 1 ? "" : "s"} left`;
		}
		return {
			text: msg,
			url: "https://www.eveonline.com/news/view/csm-20-cast-your-vote-now",
			urlText: "Vote now"
		} as Motd;
	}

	// Giveaway event.
	if (time < new Date("2026-01-01T01:00:00Z").getTime()) {
		return {
			text: "Win 22 billion ISK TONIGHT!",
			url: "/giveaways",
			urlText: "Giveaways"
		} as Motd;
	}

	// Giveaway.
	const giveawayDay = FRIDAY;
	const giveawayHour = 14;
	if ((dayOfWeek == giveawayDay - 1 && hourOfDay >= giveawayHour) || (dayOfWeek == giveawayDay && hourOfDay < giveawayHour)) {
		return {
			text: "Win 3 Days of Omega and 50 PLEX every Friday",
			url: "/giveaways",
			urlText: "Giveaways"
		} as Motd;
	}

	// Affiliate.
	if (dayOfWeek == FRIDAY || dayOfWeek == SATURDAY) {
		return {
			text: "Save 3% with code \"everef\" at checkout",
			url: MARKEE_DRAGON_URL,
			urlText: "Markee Dragon"
		} as Motd;
	}

	return null;

	// if (day % 3 == 0) {
	// 	return null;
	// }
	// return motdFallbacks[hour % motdFallbacks.length];
});

</script>

<template>
	<div v-if="eveatonActive" class="motd">
		<section class="flex">
			<div class="py-4 px-2 mx-auto max-w-screen-xl text-center flex flex-row text-xl">
				<p class="font-extrabold mx-3 tracking-tight">
          <img src="~/assets/girls_source_b_128.png" class="h-20" alt="EVEathon Logo" />
				</p>
        <p class="my-auto">
          EVEathon on Twitch this weekend
          <br/>
          <ExternalLink url="https://r3dlabs.com/events/eveathon" class="mx-3 font-normal">Twitch &raquo;</ExternalLink>
          <ExternalLink url="https://tilt.fyi/hAAtwSSvO9" class="mx-3 font-normal">Donate &raquo;</ExternalLink>
        </p>
			</div>
		</section>
	</div>
  <div v-else>Nope</div>
	<div v-if="motd" class="motd">
		<section class="flex">
			<div class="py-4 px-2 mx-auto max-w-screen-xl text-center flex flex-col md:flex-row text-xl">
				<p class="font-extrabold mx-3 tracking-tight">
					<span v-html="motd.text"></span>
				</p>
				<ExternalLink v-if="motd.url" :url="motd.url" class="mx-3 font-normal">{{motd.urlText}} &raquo;</ExternalLink>
			</div>
		</section>
	</div>
</template>

<style scoped>
.motd {
  @apply bg-gray-800 w-full;
}
img {
  @apply rounded-full;
}
</style>
