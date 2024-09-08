<script setup lang="ts">

const query = ref<string>(useRoute().query.query as string || "");

async function submit() {
	const q = query.value;
	console.log("q:", q);
	if (!q) {
		return;
	}
	console.log("Navigating");
	await navigateTo({
		path: "/search",
		query: {
			query: q
		}
	});
}

</script>

<template>
	<input
		v-model="query"
		@keydown.enter.prevent="submit"
		type="text"
		placeholder="Search"
	/>
</template>

<style scoped>
input {
  @apply border border-solid border-gray-700 p-1;
}
</style>
