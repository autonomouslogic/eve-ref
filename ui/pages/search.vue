<script setup lang="ts">

interface SearchEntry {
	text: string
	id: number
	link: string
	type: string
}

// const query = useRoute().query.query;
// Using a normal route parameter makes the page not re-search when the query changes while on the search result page.
const query = computed(() => {
	const q = useRoute().query.query as string | undefined;
	if (q == undefined || q.trim() == "") {
		return undefined;
	}
	return q;
});

useHead({
	title: "Search" + (query.value == undefined ? "" : ` for '${query.value}'`),
});

const {status, data: search} = await useLazyFetch<SearchEntry[]>("https://static.everef.net/search.json", {
	server: false
});

const result = computed(() => {
	const q = query.value;
	if (!search.value || q == undefined) {
		return [];
	}
	return search.value
		.filter((item) => item.text.toLowerCase().includes(q.toLowerCase()))
		.sort((a, b) => a.text.localeCompare(b.text));
});

</script>

<template>
	<h1>Search</h1>
	<template v-if="status == 'idle' || status == 'pending'">
		<p> Loading search data ...</p>
	</template>
	<template v-else-if="status == 'error'">
		<p>Failed to load search data.</p>
	</template>
	<template v-else-if="status == 'success'">
		<template v-if="result.length == 0">
			<p>No result for '{{query}}'</p>
		</template>
		<template v-else>
			<p>{{result.length}} result for '{{query}}'</p>
			<ul>
				<li v-for="(item, index) in result" :key="index" class="mt-2">
					<div><NuxtLink :href="item.link">{{item.text}}</NuxtLink></div>
					<div class="italic text-gray-400">{{ item.type }}</div>
				</li>
			</ul>
		</template>
	</template>
	<template v-else>
		<p>Unknown load status: {{ status }}</p>
	</template>
</template>

<style scoped>
</style>
