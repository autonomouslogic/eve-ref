<script setup lang="ts">

useHead({
	title: "Search"
});

const query = useRoute().query.query;
const {status, data: search} = await useFetch<any[]>("https://static.everef.net/search.json", {
	server: false
});

const result = computed(() => {
	if (search.value) {
		return search.value.filter((item) => item.text.toLowerCase().includes(query?.toString().toLowerCase()));
	}
	return [];
});

</script>

<template>
	<h1>Search</h1>
	<div>Status: {{status}}</div>
	<div>Query: {{query}}</div>
	<div v-if="status == 'pending'">Loading ...</div>
	<div v-else-if="status == 'error'">Failed loading search</div>
	<div v-else>Loaded</div>

	<div v-for="(item, index) in result" :key="index">
		<div>{{item.id}}: {{item.text}}</div>
	</div>

</template>

<style scoped>
</style>
