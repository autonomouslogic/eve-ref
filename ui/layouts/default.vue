<template>
	<NuxtLoadingIndicator />
	<div class="flex flex-col h-screen justify-between">
		<header><navbar /></header>
		<main class="container mb-auto px-4"><slot/></main>
		<footer class="mt-4"><Footer/></footer>
	</div>
</template>

<style>

</style>
<script setup lang="ts">
</script>
