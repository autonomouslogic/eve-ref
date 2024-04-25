<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {EVE_REFERRAL_URL, MARKEE_DRAGON_URL, PATREON_URL} from "~/lib/urls";
</script>

<template>
	<div class="mt-4 grid grid-cols-3 gap-4">

		<div class="p-4">
			<h2>Love EVE Ref?</h2>
			<p>Support the project on <ExternalLink :url="PATREON_URL">Patreon</ExternalLink></p>
		</div>

		<div class="p-4">
			<h2>New to EVE Online?</h2>
			<p><ExternalLink :url="EVE_REFERRAL_URL">Join New Eden</ExternalLink> and get Get 1,000,000 free skill points.</p>
		</div>

		<div class="p-4">
			<h2>Already Playing?</h2>
			<p>Use code "everef" at <ExternalLink :url="MARKEE_DRAGON_URL">Markee Dragon</ExternalLink> to get 3% off PLEX and Omega.</p>
		</div>

	</div>

	<div class="flex w-full justify-center">
		<img src="~/assets/PartnerBadge2.webp" class="h-40" alt="EVE Online Partner Logo" />
	</div>
</template>

<style scoped>
h1 {}
</style>
