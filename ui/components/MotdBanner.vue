<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const route = useRoute();
const isIndex = computed(() => (route.name as string).startsWith("index"));

const characterId = 90406623;
const title = "Ariel Rin for CSM19";
const text = "Voting for CSM starts October 17th";
const image = `https://images.evetech.net/characters/${characterId}/portrait`;
const url = "https://forums.eveonline.com/t/ariel-rin-for-csm19/462992";
const urlText = "Read more";
</script>

<template>
	<div class="motd">
		<section v-if="isIndex">
			<div class="py-8 px-4 mx-auto max-w-screen-xl text-center lg:py-16 flex">
				<div>
					<img :src="image" width="256" height="256" class="mx-auto" />
				</div>
				<div>
					<h1 class="mb-4 text-4xl font-extrabold tracking-tight leading-none text-gray-900 md:text-5xl lg:text-6xl dark:text-white">{{title}}</h1>
					<p class="mb-8 text-lg font-normal text-gray-500 lg:text-xl sm:px-16 lg:px-48 dark:text-gray-400">{{text}}</p>
					<div class="flex flex-col space-y-4 sm:flex-row sm:justify-center sm:space-y-0">
						<ExternalLink :url="url" class="py-3 px-5 sm:ms-4">{{urlText}} &raquo;</ExternalLink>
					</div>
				</div>
			</div>
		</section>
		<section v-else class="flex">
			<div class="py-4 px-2 mx-auto max-w-screen-xl text-center flex flex-row">
				<p class="font-bold mx-3 text-xl">{{title}}</p>
				<ExternalLink :url="url" class="x-3 text-xl font-normal">{{urlText}} &raquo;</ExternalLink>
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
