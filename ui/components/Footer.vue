<script setup lang="ts">
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {DISCORD_URL, EVE_REFERAL_URL, GITHUB_URL, MARKEE_DRAGON_URL, PATREON_URL} from "~/lib/urls";

const {locale } = useI18n();

</script>

<template>
	<div>
		<div class="footer flex py-4 justify-around">
			<NuxtLink href="/" class="flex items-center">
				<img src="~/assets/logo.png" class="h-20 mr-3 rounded-full" alt="EVE Ref Logo" />
				<span class="self-center text-2xl font-semibold whitespace-nowrap text-black">EVE Ref</span>
			</NuxtLink>
			<div class="grid grid-cols-3 gap-x-8">
				<div>
					<h2>EVE Ref</h2>
					<ul>
						<li><NuxtLink to="/about">About</NuxtLink></li>
						<li><NuxtLink to="/status">Status</NuxtLink></li>
						<li><NuxtLink to="https://docs.everef.net/" :external="true">Docs</NuxtLink></li>
						<li><NuxtLink to="https://docs.everef.net/datasets/" :external="true">Data</NuxtLink></li>
						<li><NuxtLink to="/privacy">Privacy</NuxtLink></li>
					</ul>
				</div>
				<div>
					<h2>Participate</h2>
					<ul>
						<li><ExternalLink :url="DISCORD_URL"><span><font-awesome-icon icon="fa-brands fa-discord" /></span> Discord</ExternalLink></li>
						<li><ExternalLink :url="GITHUB_URL"><span><font-awesome-icon icon="fa-brands fa-github" /></span> Github</ExternalLink></li>
					</ul>
				</div>
				<div>
					<h2>Support</h2>
					<ul>
						<li><ExternalLink :url="EVE_REFERAL_URL">Play EVE Online</ExternalLink></li>
						<li><ExternalLink :url="MARKEE_DRAGON_URL">Markeedragon</ExternalLink></li>
						<li><ExternalLink :url="PATREON_URL"><span><font-awesome-icon icon="fa-brands fa-patreon" /></span> Patreon</ExternalLink></li>
					</ul>
				</div>
			</div>
		</div>
	</div>
</template>

<style scoped>
.footer {
  background-color: var(--header-background-color);
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
