<template>
	<NuxtLoadingIndicator :height="8" />
	<div class="flex flex-col h-screen justify-between">
		<header>
			<navbar />
		</header>
		<main class="container mb-auto px-4">
			<div class="flex w-full justify-center m-0 p-0">
				<Adsbygoogle
					ad-slot="2103102198"
					ad-format="auto"
					:hide-unfilled="true"
				/>
			</div>
			<slot/>
		</main>
		<footer class="mt-4">
			<Footer/>
		</footer>
	</div>
</template>

<style scoped>
</style>

<script setup lang="ts">
</script>
