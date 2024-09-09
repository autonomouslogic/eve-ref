<script setup lang="ts">

const query = ref<string>(useRoute().query.query as string || "");

const debounceMs = 500;
let lastEntryTimeout: any = null;
let lastQuery = "";

async function submit() {
	const q = query.value;
	if (!q) {
		return;
	}
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
	if (lastEntryTimeout != null) {
		clearTimeout(lastEntryTimeout);
	}
	lastEntryTimeout = setTimeout(() => {
		submit();
	}, debounceMs);
	lastQuery = q;
}

</script>

<template>
	<form @submit.prevent="submit">
		<input
			v-model="query"
			@keyup="change"
			type="text"
			placeholder="Search"
		/>
	</form>
</template>

<style scoped>
input {
  @apply border border-solid border-gray-700 p-1;
}
</style>
