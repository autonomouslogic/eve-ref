<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {MARKEE_DRAGON_URL, PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";
import {useLazyFetch} from "nuxt/app";
import type {DonationsFile} from "~/lib/donations";
import {formatMoney} from "~/lib/money";
import {DAY} from "~/lib/timeUtils";

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
		text: "EVE is supported by you",
		url: PATREON_URL,
		urlText: "Become a Patreon"
	} as Motd,
];

const {status: donorsStatus, data: donors} = await useLazyFetch<DonationsFile>("https://static.everef.net/donations.json", {
	server: false
});

const motd = computed(() => {
	if (new Date().getTime() < new Date("2025-03-04T23:59:00Z").getTime()) {
		return {
			text: "Get 20% off PLEX plus an additional 3% with code \"everef\"",
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

	const time = new Date().getTime();
	const day = Math.floor(time / DAY);
	if (day % 3 == 0) {
		return null;
	}
	return motdFallbacks[day % motdFallbacks.length];
});

</script>

<template>
	<div v-if="motd" class="motd">
		<section class="flex">
			<div class="py-4 px-2 mx-auto max-w-screen-xl text-center flex flex-col md:flex-row text-xl">
				<p class="font-extrabold mx-3 tracking-tight">
					<span v-html="motd.text"></span>
				</p>
				<ExternalLink :url="motd.url" class="mx-3 font-normal">{{motd.urlText}} &raquo;</ExternalLink>
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
