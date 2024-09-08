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
	<div class="border border-blue-500">
		<div>Status: {{status}}</div>
		<div>Query: {{query}}</div>
		<div v-if="status == 'pending'">Loading ...</div>
		<div v-else-if="status == 'error'">Failed loading search</div>
		<div v-else>Loaded</div>
	</div>
	<div v-if="result">
		<div v-for="(item, index) in result" :key="index" class="border border-green-900">
			<div><NuxtLink :href="item.link">{{item.text}}</NuxtLink></div>
			<div>{{ item.type }}</div>
		</div>
	</div>

</template>

<style scoped>
</style>
