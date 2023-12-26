<script setup lang="ts">
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

const {locale, availableLocales } = useI18n();
const selectedLocale = ref(locale);

</script>

<template>
	<div class="border-t-2 border-neutral-400">
		<div class="flex py-4 justify-around">
			<NuxtLink href="/" class="flex items-center">
				<img src="~/assets/logo.png" class="h-20 mr-3 rounded-full" alt="EVE Ref Logo" />
				<span class="self-center text-2xl font-semibold whitespace-nowrap text-black">EVE Ref</span>
			</NuxtLink>
			<div class="grid grid-cols-3 gap-x-8">
				<div>
					<h2 class="uppercase text-lg">EVE Ref</h2>
					<ul>
						<li><NuxtLink to="/about">About</NuxtLink></li>
						<li><NuxtLink to="/status">Status</NuxtLink></li>
						<li><a href="https://data.everef.net/">Data</a></li>
						<li><a href="https://docs.everef.net/">Docs</a></li>
            <li><NuxtLink to="/privacy">Privacy</NuxtLink></li>
					</ul>
				</div>
				<div>
					<h2 class="uppercase text-lg">Participate</h2>
					<ul>
						<li><a href="https://discord.gg/fZYPAxFyXG">
							<span><font-awesome-icon icon="fa-brands fa-discord" /></span>
							Discord
						</a></li>
						<li><a href="https://github.com/autonomouslogic/eve-ref/">
							<span><font-awesome-icon icon="fa-brands fa-github" /></span>
							Github
						</a></li>
					</ul>
				</div>
				<div>
					<h2 class="uppercase text-lg">Support</h2>
					<ul>
						<li><a href="https://www.eveonline.com/signup?invc=b28d194d-7181-4bf0-8e3f-72cebbc7ca7d">Play EVE Online</a></li>
						<li><a href="https://store.markeedragon.com/affiliate.php?id=933">Markeedragon</a></li>
						<li><a href="https://patreon.com/everef">
							<span><font-awesome-icon icon="fa-brands fa-patreon" /></span>
							Patreon
						</a></li>
					</ul>
				</div>
			</div>
		</div>
	</div>
</template>

<style scoped>
a {
  @apply no-underline;
}
</style>
