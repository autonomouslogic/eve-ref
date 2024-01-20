<script setup lang="ts">
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

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
					<h2>Support</h2>
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
