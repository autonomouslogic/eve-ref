<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {DATASETS_DOCS_URL, EVE_REFERRAL_URL, MARKEE_DRAGON_URL, PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";
import {useLazyFetch} from "nuxt/app";
import type {DonationsFile} from "~/lib/donations";
import {formatMoney} from "~/lib/money";
import {DAY, HOUR} from "~/lib/timeUtils";

const route = useRoute();

interface Motd {
	text: string
	url: string
	urlText: string
}

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
	const dayOfWeek = new Date().getDay();
	const time = new Date().getTime();
	const day = Math.floor(time / DAY);
	const hour = Math.floor(time / HOUR);

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

	// Priority.
	if (new Date().getTime() < new Date("2025-11-10T23:59:59Z").getTime()) {
		return {
			text: "Ariel Rin for CSM",
			url: "https://www.eveonline.com/news/view/csm-20-candidate-announcements",
			urlText: "View Candidates"
		} as Motd;
	}

	if (dayOfWeek == 4) {
		return {
			text: "Win 50 PLEX and 3 Day Omega every Friday",
			url: "/giveaways",
			urlText: "Giveaways"
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
