<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";

const route = useRoute();
const isIndex = computed(() => (route.name as string).startsWith("index"));

const imgSize = 196;

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
			<div class="py-8 px-4 mx-auto max-w-screen-xl grid grid-cols-1 md:grid-cols-3">
				<div class="max-w-fit justify-self-end px-4 lg:px-8 hidden md:block">
					<img :src="image" :width="imgSize" :height="imgSize" class="" />
				</div>
				<div class="col-span-2 text-center md:text-left">
					<h1 class="mb-4 text-4xl md:text-5xl lg:text-6xl font-extrabold tracking-tight leading-none">{{title}}</h1>
					<p class="mb-4 font-normal text-lg lg:text-xl">{{text}}</p>
					<ExternalLink :url="url" class="">{{urlText}} &raquo;</ExternalLink>
				</div>
			</div>
		</section>
		<section v-else class="flex">
			<div class="py-4 px-2 mx-auto max-w-screen-xl text-center flex flex-col md:flex-row text-xl">
				<p class="font-extrabold mx-3 tracking-tight">{{title}}</p>
				<ExternalLink :url="url" class="mx-3 font-normal">{{urlText}} &raquo;</ExternalLink>
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
