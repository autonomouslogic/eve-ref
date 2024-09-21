<script setup lang="ts">

import InternalLink from "~/components/helpers/InternalLink.vue";

interface SearchEntry {
	text: string
	query: string
	id: number
	link: string
	type: string
}

// const query = useRoute().query.query;
// Using a normal route parameter makes the page not re-search when the query changes while on the search result page.
const query = computed<string | undefined>(() => {
	const q = useRoute().query.query as string | undefined;
	if (q == undefined || q.trim() == "") {
		return undefined;
	}
	return q;
});

useHead({
	title: "Search" + (query.value == undefined ? "" : ` for '${query.value}'`),
});

const {status, data: searchData} = await useLazyFetch<SearchEntry[]>("https://static.everef.net/search.json", {
	server: false
});

const getEntrySearchable = (entry: SearchEntry): string => entry.query || entry.text;

const compoundSort = (sorts: ((a: SearchEntry, b: SearchEntry) => number)[]): (a: SearchEntry, b: SearchEntry) => number => {
	return (a: SearchEntry, b: SearchEntry): number => {
		for (const sort of sorts) {
			const result = sort(a, b);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	};
};

const textSort = (a: SearchEntry, b: SearchEntry): number => getEntrySearchable(a).localeCompare(getEntrySearchable(b));

function prefixSort(q: string): (a: SearchEntry, b: SearchEntry) => number {
	return (a: SearchEntry, b: SearchEntry): number => {
		const aIndex = getEntrySearchable(a).toLowerCase().indexOf(q.toLowerCase());
		const bIndex = getEntrySearchable(b).toLowerCase().indexOf(q.toLowerCase());
		if (aIndex == 0 && bIndex == 0) {
			return 0;
		}
		if (aIndex == 0) {
			return -1;
		}
		if (bIndex == 0) {
			return 1;
		}
		return 0;
	};
}

const results = computed(() => {
	const q = query.value;
	if (!searchData.value || q == undefined) {
		return [];
	}

	// Check if a number was entered and search by ID if it was.
	if (q.match(/^[0-9]+$/)) {
		const num = parseInt(q, 10);
		return searchData.value
			.filter((item) => item.id == num);
	}

	const queryParts = q.split(/\s+/);
	const regex = new RegExp(queryParts.join(".*"), "i");

	return searchData.value
		.filter(function (item) {
			return getEntrySearchable(item).match(regex);
		})
		.sort(compoundSort([
			prefixSort(queryParts[0]),
			textSort
		]));
});

</script>

<template>
	<h1>Search</h1>
	<p v-if="status == 'idle' || status == 'pending'">Loading search data ...</p>
	<p v-else-if="status == 'error'">Failed to load search data.</p>
	<template v-else-if="status == 'success'">
		<p v-if="results.length == 0">No result for '{{ query }}'</p>
		<template v-else>
			<p>{{ results.length }} result for '{{ query }}'</p>
			<ul>
				<li v-for="item in results" :key="item.id" class="mt-2">
					<div><InternalLink :to="item.link">{{ item.text }}</InternalLink></div>
					<div class="italic text-gray-400">{{ item.type }} [{{ item.id }}]</div>
				</li>
			</ul>
		</template>
	</template>
	<p v-else>Unknown load status: {{ status }}</p>
</template>

<style scoped>
</style>
