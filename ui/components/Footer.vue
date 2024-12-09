<script setup lang="ts">
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {
	DISCORD_URL,
	EVE_REFERRAL_URL,
	EVE_STORE_URL,
	GITHUB_URL,
	MARKEE_DRAGON_URL,
	PATREON_URL,
	YOUTUBE_URL
} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";
</script>

<template>
	<div class="footer flex p-4 justify-around flex-col sm:flex-row">
		<InternalLink to="/" class="flex items-center">
			<img src="~/assets/logo.png" class="h-20 mr-3 rounded-full" alt="EVE Ref Logo" />
			<span class="self-center text-2xl font-semibold whitespace-nowrap text-black">EVE Ref</span>
		</InternalLink>
		<div class="flex justify-between gap-8">
			<div>
				<h2>EVE Ref</h2>
				<ul>
					<li><InternalLink to="/about">About</InternalLink></li>
					<li><ExternalLink url="https://status.everef.net/">Status</ExternalLink></li>
					<li><ExternalLink url="https://docs.everef.net/">Docs</ExternalLink></li>
					<li><ExternalLink url="https://docs.everef.net/datasets/">Data</ExternalLink></li>
					<li><InternalLink to="/privacy">Privacy</InternalLink></li>
					<li>
						<LocaleSwitcher/>
					</li>
				</ul>
			</div>
			<div>
				<h2>Participate</h2>
				<ul>
					<li><InternalLink :to="DISCORD_URL"><span><font-awesome-icon icon="fa-brands fa-discord" /></span> Discord</InternalLink></li>
					<li><ExternalLink :url="GITHUB_URL"><span><font-awesome-icon icon="fa-brands fa-github" /></span> Github</ExternalLink></li>
					<li><ExternalLink :url="YOUTUBE_URL"><span><font-awesome-icon icon="fa-brands fa-youtube" /></span> YouTube</ExternalLink></li>
				</ul>
			</div>
			<div>
				<h2>Support</h2>
				<ul>
					<li><ExternalLink :url="PATREON_URL"><span><font-awesome-icon icon="fa-brands fa-patreon" /></span> Patreon</ExternalLink></li>
					<li><ExternalLink :url="MARKEE_DRAGON_URL">Markeedragon</ExternalLink></li>
					<li><ExternalLink :url="EVE_STORE_URL">EVE Online Store</ExternalLink></li>
					<li><ExternalLink :url="EVE_REFERRAL_URL">Play EVE Online</ExternalLink></li>
				</ul>
			</div>
		</div>
	</div>
</template>

<style scoped>
.footer {
  background-color: var(--header-background-color);
  color: black;
}

h1, h2, h3, h4, h5, h6 {
  color: black;
}

h2 {
  @apply uppercase text-lg;
}

a {
  @apply text-gray-600;
}
</style>
