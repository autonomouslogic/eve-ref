<script setup lang="ts">
const localePath = useLocalePath();

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
		path: localePath("/search"),
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
	<div class="search">
		<form @submit.prevent="submit">
			<input
				v-model="query"
				@keyup="change"
				type="text"
				placeholder="Search"
			/>
		</form>
	</div>
</template>

<style scoped>
.search {
  @apply flex items-center;
}
form, input {
  @apply w-full;
}
input {
  @apply border border-solid border-gray-300 p-1;
}
</style>
