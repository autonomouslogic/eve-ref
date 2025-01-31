<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";
import {useLazyFetch} from "nuxt/app";
import type {DonationsFile} from "~/lib/donations";
import {formatMoney} from "~/lib/money";

const route = useRoute();

interface Motd {
	text: string
	url: string
	urlText: string
}

const {status: donorsStatus, data: donors} = await useLazyFetch<DonationsFile>("https://static.everef.net/donations.json", {
	server: false
});

const motd = computed(() => {
	// Recent donors.
	if (donorsStatus.value == "success" && donors?.value?.recent?.length && donors.value.recent.length > 0) {
		const donorsText = donors.value.recent.map(donor => {
			return `${donor.donor_name} donated ${formatMoney(donor.amount)}`;
		}).join("<br/>");
		console.log(donorsText);
		return {
			text: `${donorsText}`,
			url: "/about",
			urlText: "Donate ISK"
		} as Motd;
	}

	return {
		text: "EVE Ref is funded by donations",
		url: "/about",
		urlText: "Support"
	} as Motd;

	// // No MOTD
	// return null;
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
