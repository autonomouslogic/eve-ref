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

useHead({
	title: "About"
});
</script>

<template>
	<h1>About EVE Ref</h1>
	<p>
		EVE Ref is a reference site for the game EVE Online by CCP.
		It provides easy access to the information present in the game, such as ships, item stats, industry, reprocessing, systems, agents, etc.
		EVE Ref is created by <ExternalLink url="https://kennethjorgensen.com">Kenn</ExternalLink>,
		aka <ExternalLink :url="DATE_TOVIKOV_CHAR">Dato Tovikov</ExternalLink>.
		Join the <ExternalLink :url="DISCORD_URL">Discord</ExternalLink> to get in touch and participate.
		Source code is available on <ExternalLink :url="GITHUB_URL">Github</ExternalLink>.
	</p>

	<div class="flex w-full justify-center">
		<img src="~/assets/PartnerBadge2.webp" class="h-40" alt="EVE Online Partner Logo" />
	</div>

	<div class="grid grid-cols-1 md:grid-cols-3 gap-4">
		<span>
			<h2>Financial Support</h2>
			<p>
				<ExternalLink :url="PATREON_URL">Become a patron</ExternalLink> to support EVE Ref financially.
				EVE Ref is a free service with no ads, but it does cost money to run and takes time to build and maintain.
				Any and all contributions are greatly appreciated &#128077;
			</p>
		</span>
		<span>
			<h2>Affiliates</h2>
			<p>
				<ExternalLink :url="MARKEE_DRAGON_URL">Buy PLEX or Omega from Markee Dragon</ExternalLink>
				to support EVE Ref through their affiliate programme. Use the code "everef" to get 3% off.
			</p>
			<p>
				<ExternalLink :url="EVE_STORE_URL">Buy in the EVE Online Store</ExternalLink> and use code "everef".
			</p>
			<p>
				<ExternalLink :url="EVE_REFERRAL_URL">Join New Eden</ExternalLink> and get 1,000,000 free skill points.
			</p>
			<p>
				<ExternalLink :url="HETZNER_REFERAL_URL">Use Hetzner</ExternalLink> and €20 free credits.
			</p>
		</span>
		<span>
			<h2>ISK Donations</h2>
			<p>
				ISK donations are also welcome and can be made directly to
				<ExternalLink :url="EVE_REF_CHAR">EVE Ref</ExternalLink> of
				<ExternalLink :url="EVE_REF_CORP">EVE Ref Holdings</ExternalLink> [EVREF], or
				<ExternalLink :url="DATE_TOVIKOV_CHAR">Dato Tovikov</ExternalLink>.
			</p>
		</span>
	</div>

	<h2>Copyright</h2>
	<p>
		EVE Ref is the property of <ExternalLink url="https://autonomouslogic.com">Autonomous Logic</ExternalLink>. All rights reserved.
	</p>
	<p>
		EVE Online and the EVE logo are the registered trademarks of CCP hf. All rights are reserved worldwide.
		All other trademarks are the property of their respective owners. EVE Online, the EVE logo, EVE and all associated
		logos and designs are the intellectual property of CCP hf.
		All artwork, screenshots, characters, vehicles, storylines, world facts or other recognizable features of the
		intellectual property relating to these trademarks are likewise the intellectual property of CCP hf. CCP hf.
		has granted permission to EVE Ref to use EVE Online and all associated logos and designs for promotional and
		information purposes on its website but does not endorse, and is not in any way affiliated with, EVE Ref.
		CCP is in no way responsible for the content on or functioning of this website, nor can it be liable for any damage
		arising from the use of this website.
	</p>
</template>

<style scoped>
p {
  @apply my-4;
}
</style>
