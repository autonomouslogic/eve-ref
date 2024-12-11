<script setup lang="ts">
import ExternalLink from "~/components/helpers/ExternalLink.vue";
import {PATREON_URL} from "~/lib/urls";
import InternalLink from "~/components/helpers/InternalLink.vue";

const route = useRoute();
const isIndex = computed(() => (route.name as string).startsWith("index"));

const title = "I'm giving away over 30 billion ISK";
const text = "Let's celebrate the new year by helping me give away over 30 billion ISK in December.";
const url = "/giveaways";
const urlText = "Join Giveaway";
</script>

<template>
	<div class="motd">
		<section v-if="isIndex">
			<div class="py-8 px-4 mx-auto max-w-screen-xl grid">
				<div class="text-center sm:text-left">
					<h1 class="mb-4 text-4xl md:text-5xl lg:text-6xl font-extrabold tracking-tight leading-none">{{title}}</h1>
					<span v-if="text" class="hidden sm:block">
						<p class="mb-4">{{text}}</p>
					</span>
					<p class="mb-4 font-normal text-lg lg:text-xl">
						<InternalLink :to="url">{{urlText}} &raquo;</InternalLink>
					</p>
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
