<script setup lang="ts">

const query = ref<string>(useRoute().query.query as string || "");

const debounceMs = 500;
let lastEntryTimeout: any = null;
let lastQuery = "";

async function submit() {
	const q = query.value;
	console.log("submit:", q);
	if (!q) {
		return;
	}
	console.log("Submit navigating");
	await navigateTo({
		path: "/search",
		query: {
			query: q
		}
	});
}

async function change() {
	const q = query.value;
	if (lastQuery == q) {
		return;
	}
	console.log("change:", q);
	if (lastEntryTimeout != null) {
		clearTimeout(lastEntryTimeout);
	}
	lastEntryTimeout = setTimeout(() => {
		console.log("Debounced submit");
		submit();
	}, debounceMs);
	lastQuery = q;
}

</script>

<template>
	<input
		v-model="query"
		@keydown.enter.prevent="submit"
		@keyup="change"
		type="text"
		placeholder="Search"
	/>
</template>

<style scoped>
input {
  @apply border border-solid border-gray-700 p-1;
}
</style>
